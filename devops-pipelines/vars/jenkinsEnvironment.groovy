// Map with lists of valid tools for each stage
//VALID_WORKSPACE_CLEANUP_JIRA_VALUES is used to validate both WORKSPACE_CLEANUP_STATUS_VALUE and JIRA_COMMENT_CRITERIA, because they share values
@groovy.transform.Field
def stageValidationMap = [VALID_PREBUILD_TOOLS: ['cmake', 'nuget', 'qmake'],
                          VALID_BUILD_TOOLS: ['gradle', 'make', 'maven', 'msbuild', 'setuptools', 'vxworks'],
                          VALID_CREATE_INSTALLER_TOOLS: ['debian', 'cpack'],
                          VALID_VCVARS_ARCH: ['x86', 'x86_amd64', 'x86_x64', 'x86_arm', 'x86_arm64', 'amd64', 'x64', 'amd64_x86', 'x64_x86', 'amd64_arm', 'x64_arm', 'amd64_arm64', 'x64_arm64'],
                          VALID_FPGA_COMPILE_TOOLS: ['quartus', 'vivado'],
                          VALID_FPGA_LINT_TOOLS: ['ascent'],
                          VALID_FPGA_VERIFY_CDC_TOOLS: ['meridian'],
                          VALID_CPACK_GENERATORS: ['archive', 'deb', 'rpm'],
                          VALID_UNIT_TEST_TOOLS: ['googletest', 'vstest', 'junit', 'unittest', 'pytest', 'qttest', 'ctest'],
                          VALID_STATIC_CODE_ANALYSIS_TOOLS: ['sonarqube', 'klocwork', 'cppcheck', 'pylint', 'spotbugs', 'spotbugs_cli', 'spotbugs_pom'],
                          VALID_STATIC_APPLICATION_SECURITY_TOOLS: ['fortify', 'coverity', 'klocwork'],
                          VALID_CODE_COVERAGE_TOOLS: ['opencover', 'opencppcoverage', 'cobertura', 'jacoco', 'gcov', 'coverage', 'pytest-cov'],
                          VALID_STYLE_CHECK_TOOLS: ['stylecop', 'checkstyle', 'pylint'],
                          VALID_ARTIFACT_MANAGEMENT_TOOLS: ['generic', 'nuget', 'pypi', 'maven', 'debian'],
                          VALID_ARTIFACT_DEPLOYMENT_TOOLS: ['generic', 'ansible'],
                          VALID_FUNCTIONAL_TEST_TOOLS: ['testexecute', 'testcomplete', 'pytest'],
                          VALID_DYNAMIC_APPLICATION_SECURITY_TOOLS: ['zap'],
                          VALID_RELEASE_ARTIFACT_MANAGEMENT_TOOLS: ['artifactory'],
                          VALID_CONTAINER_SCAN_TOOLS: ['trivy'],
                          VALID_BOOLEAN_VALUES: ['true', 'false'],
                          VALID_SOURCE_CODE_TOOLS: ['bitbucket'],
                          VALID_MODE_TYPES: ['model', 'archive'],
                          VALID_ANSIBLE_REPO_TYPE: ['source', 'separate'],
                          VALID_ARCHIVE_CHOICE: ['properties', 'name'],
                          VALID_WORKSPACE_CLEANUP_JIRA_VALUES: ['always', 'success', 'unstable', 'not-failure', 'never'], 
                          VALID_SEND_NOTIFICATION_VALUES: ['always', 'unstable', 'changed', 'failure', 'never']]

// Class wrapping stage data to allow persistent changes across calls to the jenkinsEnvironment object
class StageDataClass {
   static Map stageDataMap = [:]
}

def getValidStageMap() {
   return stageValidationMap
}

/** Get a map of supported stages from either a program-provided path or the default resource.
  * Note: stage data is of the format {NAME: {STAGEOBJECT, INITIAL_STAGE_DATA_KEY, ENABLED}}
  *
  * Map pipelineParams: the pipeline parameters containing provided stage data
  * Returns a map of stage data relevant to running the stage
 **/
def getStageData(Map pipelineParams = null) {
   if (StageDataClass.stageDataMap) {
      return StageDataClass.stageDataMap
   }

   if (pipelineParams == null) {
      logger.logError "Required pipeline parameters map was null when attempting to initialize stage data."
   }

   stageDataMap = StageDataClass.stageDataMap
   // Get the stage properties file
   def stagePropertiesText
   if (pipelineParams.STAGE_PROPERTIES_FILE) {
      if (fileExists(pipelineParams.STAGE_PROPERTIES_FILE)) {
         // Get the program-specified properties file
         stagePropertiesText = readFile pipelineParams.STAGE_PROPERTIES_FILE
      }
      else {
         logger.logError "Could not find the specified pipeline stages file '${pipelineParams.STAGE_PROPERTIES_FILE}'."
      }
   }
   else {
      // Load the default from the shared library's 'resources' directory
      stagePropertiesText = libraryResource "pipelineStages.properties"
   }
   // Build properties from the file content
   def stageProperties = parsePropertiesStringToMap(stagePropertiesText)
   
   // Make sure no unknown stages were specified
   // Stages with the following format CUSTOM_{STAGE} will be skipped when checking for unknown stages
   def unknownStages = stageProperties.values().findAll { it =~ /^(?!CUSTOM_.*)/ && !constants.INITIAL_STAGE_DATA.keySet().contains(it) }
   if (unknownStages) {
      logger.logError("""Unrecognized stage(s) specified: ${unknownStages}
                        |Valid stages are: ${constants.INITIAL_STAGE_DATA.keySet()}""".stripMargin())
   }
   
   // Make sure all enabled stages are present (allowing deprecated RUN_{STAGE} params)
   // Also make sure all disabled stages are flagged
   stageProperties.each { name, initialStageDataKey ->
      def runStageParam = pipelineParams["RUN_${initialStageDataKey}"]
      def customAgentLabel = pipelineParams["${initialStageDataKey}_AGENT_LABEL"]
      def parallelStageGroup = pipelineParams["${initialStageDataKey}_PARALLEL_GROUP"] ?: UUID.randomUUID().toString()

      // Stage is enabled if it's present in stageProperties, but pipelineParams.RUN_{STAGE} takes precedence
      boolean enableStage = stageProperties.values().findAll { it == initialStageDataKey } && !"${runStageParam}".equalsIgnoreCase('false')
      enableStage = enableStage || "${runStageParam}".equalsIgnoreCase('true')
      setStageDataItem(pipelineParams, name, enableStage, initialStageDataKey, customAgentLabel, parallelStageGroup)
   }

   return stageDataMap
}

/** Get data for a particular stage by the INITIAL_STAGE_DATA_KEY, verifying the key is a supported stage.
  * Note: stage data is of the format {NAME: {STAGEOBJECT, INITIAL_STAGE_DATA_KEY, ENABLED}}
  *
  * String initialStageDataKey: the value from the stageDataMap which corresponds to the keys of constants.INITIAL_STAGE_DATA
  * Returns the stage data associated with provided value
 **/
def getStageData(String initialStageDataKey) {
   if (!constants.INITIAL_STAGE_DATA.findAll { it.key == initialStageDataKey }) {
      logger.logError("""Unrecognized stage specified: ${initialStageDataKey}
                        |Valid stages are: ${constants.INITIAL_STAGE_DATA.keySet()}""".stripMargin())
   }

   if (!StageDataClass.stageDataMap) {
      logger.logError("Stage data was not initialized; cannot access value ${initialStageDataKey} of null map")
   }

   return getStageData().find {it.value?."initialStageDataKey" == initialStageDataKey}?.value
  
}

/** Set pipelineParams values based on job-level parameters.
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def setParametersFromJob(Map pipelineParams) {
   pipelineParams.each { key, value ->
      def jobValue = params["OVERRIDE_${key}"]
      if (jobValue) {
         logger.logInfo("Overriding Jenkinsfile parameter of ${key} with job-level parameter value '${jobValue}'")
         pipelineParams[key] = jobValue
      }
   }
   params.each { key, value ->
      if (key.contains("OVERRIDE_RUN_") || key.contains("OVERRIDE_DISPLAY_NAME")) {
         def simpleKey = key - "OVERRIDE_"
         logger.logInfo("Overriding Jenkinsfile parameter of ${simpleKey} with job-level parameter value '${value}'")
         pipelineParams[simpleKey] = value
      }
   }
}

/** Validate/initialize all RUN_{STAGE} parameters, setting defaults based on deprecated behavior.
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateRunStageParams(Map pipelineParams) {
   // Disable certain stages by default if no custom stageData properties file was provided
   def enabledByDefault = pipelineParams.STAGE_PROPERTIES_FILE ? 'true' : 'false'

   // Validate deprecated RUN_* parameters before building stageData 
   validateParameter(pipelineParams, 'RUN_BUILD', 'true', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_CREATE_INSTALLER', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_FPGA_COMPILE', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_UNIT_TEST', 'true', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_STATIC_CODE_ANALYSIS', 'true', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_STATIC_APPLICATION_SECURITY', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_FPGA_LINT', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_FPGA_VERIFY_CDC', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_CODE_COVERAGE', 'true', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_STYLE_CHECK', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_ARTIFACT_MANAGEMENT', 'true', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_DEPLOY_ARTIFACTS', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_FUNCTIONAL_TEST', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_DYNAMIC_APPLICATION_SECURITY', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_TAG_SOURCE_CODE', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_PROMOTE_ARTIFACT', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_ARCHIVE_ARTIFACT', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_MERGE_BRANCH', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_PACKAGE_FOR_DELIVERY', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_DOCKER_IMAGE_MANAGEMENT', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_DOCKER_IMAGE_BUILD', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_DOCKER_IMAGE_PUSH', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'RUN_CONTAINER_SCAN', enabledByDefault, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   if (pipelineParams.RUN_DOCKER_IMAGE_BUILD == 'true') {
      pipelineParams.RUN_DOCKER_IMAGE_MANAGEMENT = 'false'
   }
   else {
      pipelineParams.RUN_DOCKER_IMAGE_PUSH = 'false'
   }
}

/** Validate/initialize all build stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateBuildStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'BUILD_TOOL', 'INVALID', true, 'BUILD', stageValidationMap['VALID_BUILD_TOOLS'])
   pipelineParams.PROJECT_ROOT = pipelineParams.PROJECT_ROOT ?: '.'
   pipelineParams.SOURCE_CODE_DIRECTORY = pipelineParams.SOURCE_CODE_DIRECTORY ?: 'src'
   pipelineParams.VCVARS_BATCH_FILE = pipelineParams.VCVARS_BATCH_FILE ?: ''
   if ("${pipelineParams.VCVARS_BATCH_FILE}" != '') {
      // This is a required vcvars argument. verify the settings if the vcvars batch file is specified
      validateParameter(pipelineParams, 'VCVARS_ARCH', 'INVALID', true, '', stageValidationMap['VALID_VCVARS_ARCH'])
   }
   pipelineParams.VCVARS_PLATFORM = pipelineParams.VCVARS_PLATFORM ?: ''
   pipelineParams.VCVARS_WINSDK = pipelineParams.VCVARS_WINSDK ?: ''
   pipelineParams.VCVARS_VCVER = pipelineParams.VCVARS_VCVER ?: ''
}

/** Validate/initialize all create installer stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateCreateInstallerStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'CREATE_INSTALLER_CHOICE', 'INVALID', true, 'CREATE_INSTALLER', stageValidationMap['VALID_CREATE_INSTALLER_TOOLS'])
   validateParameter(pipelineParams, 'PACKAGE_NAME', 'INVALID', false, 'CREATE_INSTALLER', null, [CREATE_INSTALLER_CHOICE: ['debian']])
   validateParameter(pipelineParams, 'PACKAGE_INSTALL_PATH', 'INVALID', false, 'CREATE_INSTALLER', null, [CREATE_INSTALLER_CHOICE: ['debian']])
   validateParameter(pipelineParams, 'INSTALLER_CONFIG_FILE', 'INVALID', false, 'CREATE_INSTALLER', null, [CREATE_INSTALLER_CHOICE: ['debian']])

   //CPack Parameters
   validateParameter(pipelineParams, 'CPACK_WITH_CMAKE', 'true', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'CPACK_GENERATOR', 'INVALID', true, 'CREATE_INSTALLER', stageValidationMap['VALID_CPACK_GENERATORS'], [CREATE_INSTALLER_CHOICE: ['cpack']])
}

/** Validate/initialize all prebuild parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validatePrebuildParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'RUN_PREBUILD', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   // Validate prebuild tool with validation method that doesn't require stageParam validation
   if (pipelineParams.RUN_PREBUILD == 'true') {
      validateParameter(pipelineParams, 'PREBUILD_TOOL', 'INVALID', true, '', stageValidationMap['VALID_PREBUILD_TOOLS'])
      validateParameter(pipelineParams, 'ARTIFACTORY_DEPENDENCY_URL', 'INVALID', false, 'BUILD', null, [PREBUILD_TOOL: ['nuget'], BUILD_TOOL: ['msbuild']])
      validateParameter(pipelineParams, 'CMAKE_LISTS_PATH', 'INVALID', false, 'BUILD', null, [PREBUILD_TOOL: ['cmake']])
      validateParameter(pipelineParams, 'QMAKE_FILE', 'INVALID', false, 'BUILD', null, [PREBUILD_TOOL: ['qmake']])
      if (pipelineParams.PREBUILD_TOOL == 'cmake')
      {
         pipelineParams.PREBUILD_DIRECTORY = pipelineParams.PREBUILD_DIRECTORY ? "${env.WORKSPACE}/${pipelineParams.PREBUILD_DIRECTORY}": "${env.WORKSPACE}/DOCPF_Prebuild"
      }
   }
   else {
       pipelineParams.PREBUILD_DIRECTORY = ''
   }
}

/** Validate/initialize all unit test stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateUnitTestStageParams(Map pipelineParams) {
   //Unit Testing Parameters
   validateParameter(pipelineParams, 'UNIT_TEST_CHOICE', 'INVALID', true, 'UNIT_TEST', stageValidationMap['VALID_UNIT_TEST_TOOLS'])
   if (!isUnix()){
      validateParameter(pipelineParams, 'GT_TEST_BINARY', 'INVALID', false, 'UNIT_TEST', null, [UNIT_TEST_CHOICE: ['googletest']])
      validateParameter(pipelineParams, 'GT_TEST_BINARY', 'INVALID', false, 'CODE_COVERAGE', null, [CODE_COVERAGE_CHOICE: ['opencppcoverage']])
   }
   pipelineParams.UNIT_TEST_DIRECTORY = pipelineParams.UNIT_TEST_DIRECTORY ?: 'unittest'
   pipelineParams.UNIT_TEST_TYPES = pipelineParams.UNIT_TEST_TYPES ?: ''
   pipelineParams.UNIT_TEST_REPORT_FOLDER  = pipelineParams.UNIT_TEST_REPORT_FOLDER  ?: 'UnitTestResults'
   pipelineParams.UNIT_TEST_ARCHIVE_ARGS = pipelineParams.UNIT_TEST_ARCHIVE_ARGS ?: []
   pipelineParams.UNIT_TEST_PUBLISH_ARGS = pipelineParams.UNIT_TEST_PUBLISH_ARGS ?: []
   
   //Set Ctest parameters
   pipelineParams.CTEST_CONFIG_FILE_ARG = pipelineParams.CTEST_CONFIG_FILE_ARG ?: ''
   pipelineParams.CTEST_LABEL_REGEX_INCLUDE_ARG = pipelineParams.CTEST_LABEL_REGEX_INCLUDE_ARG ?: ''
   pipelineParams.CTEST_LABEL_REGEX_EXCLUDE_ARG = pipelineParams.CTEST_LABEL_REGEX_EXCLUDE_ARG ?: ''
   pipelineParams.CTEST_REGEX_INCLUDE_ARG = pipelineParams.CTEST_REGEX_INCLUDE_ARG ?: ''
   pipelineParams.CTEST_REGEX_EXCLUDE_ARG = pipelineParams.CTEST_REGEX_EXCLUDE_ARG ?: ''
   pipelineParams.CTEST_REPEAT_MODE_ARG = pipelineParams.CTEST_REPEAT_MODE_ARG ?: ''
   // If CTEST_STOP_ON_FAILURE_FLAG wasn't declared, set it to false.
   validateParameter(pipelineParams, 'CTEST_STOP_ON_FAILURE_FLAG', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   // Set thresholds to not fail the build by default
   validateParameter(pipelineParams, 'UNIT_TEST_SKIPPED_THRESHOLD_FAILS', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'UNIT_TEST_FAILED_THRESHOLD_FAILS', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])

   // set unit test thresholds to vstest thresholds if they have been set (for deprecated parameters)
   pipelineParams.UNIT_TEST_FAILED_THRESHOLD = pipelineParams.VSTEST_FAILED_THRESHOLD ?: pipelineParams.UNIT_TEST_FAILED_THRESHOLD
   pipelineParams.UNIT_TEST_FAILED_NEW_THRESHOLD = pipelineParams.VSTEST_FAILED_NEW_THRESHOLD ?: pipelineParams.UNIT_TEST_FAILED_NEW_THRESHOLD
   pipelineParams.UNIT_TEST_SKIPPED_THRESHOLD = pipelineParams.VSTEST_SKIPPED_THRESHOLD ?: pipelineParams.UNIT_TEST_SKIPPED_THRESHOLD
   pipelineParams.UNIT_TEST_SKIPPED_NEW_THRESHOLD = pipelineParams.VSTEST_SKIPPED_NEW_THRESHOLD ?: pipelineParams.UNIT_TEST_SKIPPED_NEW_THRESHOLD

   // junit Zephyr parameters
   validateParameter(pipelineParams, 'PUBLISH_TO_ZEPHYR', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   if (getStageData('UNIT_TEST')?.enabled && pipelineParams.UNIT_TEST_CHOICE == 'junit') 
   {
      if(pipelineParams.PUBLISH_TO_ZEPHYR == 'true'){
         validateParameter(pipelineParams, 'ZEPHYR_CREATE_PACKAGE', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
         pipelineParams.ZEPHYR_CYCLE_DURATION = pipelineParams.ZEPHYR_CYCLE_DURATION?.toLowerCase() ?: '30 days'
         pipelineParams.ZEPHYR_CYCLE_KEY = pipelineParams.ZEPHYR_CYCLE_KEY?.toLowerCase() ?: 'CreateNewCycle'
         validateParameter(pipelineParams, 'ZEPHYR_PROJECT_KEY', 'INVALID', false, 'UNIT_TEST', null, [UNIT_TEST_CHOICE: ['junit']])
         validateParameter(pipelineParams, 'ZEPHYR_RELEASE_KEY', 'INVALID', false, 'UNIT_TEST', null, [UNIT_TEST_CHOICE: ['junit']])
         validateParameter(pipelineParams, 'ZEPHYR_SERVER_ADDRESS', 'INVALID', false, 'UNIT_TEST', null, [UNIT_TEST_CHOICE: ['junit']])
      }
   }

   // make functional test thresholds fail or set build to unstable depending on given parameters
   if(pipelineParams.UNIT_TEST_SKIPPED_THRESHOLD_FAILS == 'true') 
   {  
      // Set xunit to fail pipeline if skipped thresholds are passed
      pipelineParams.SKIPPED_UNIT_TEST_FAILURE_THRESHOLD = pipelineParams.UNIT_TEST_SKIPPED_THRESHOLD
      pipelineParams.SKIPPED_UNIT_TEST_FAILURE_NEW_THRESHOLD = pipelineParams.UNIT_TEST_SKIPPED_NEW_THRESHOLD
   }
   else
   {  
      // Set xunit to make pipeline unstable if skipped thresholds are passed
      pipelineParams.SKIPPED_UNIT_TEST_UNSTABLE_THRESHOLD = pipelineParams.UNIT_TEST_SKIPPED_THRESHOLD
      pipelineParams.SKIPPED_UNIT_TEST_UNSTABLE_NEW_THRESHOLD = pipelineParams.UNIT_TEST_SKIPPED_NEW_THRESHOLD
   }

   if(pipelineParams.UNIT_TEST_FAILED_THRESHOLD_FAILS == 'true') 
   {  
      // Set xunit to fail pipeline if failure thresholds are passed
      pipelineParams.FAILED_UNIT_TEST_FAILURE_THRESHOLD = pipelineParams.UNIT_TEST_FAILED_THRESHOLD
      pipelineParams.FAILED_UNIT_TEST_FAILURE_NEW_THRESHOLD = pipelineParams.UNIT_TEST_FAILED_NEW_THRESHOLD
   }
   else
   {  
      // Set xunit to make pipeline unstable if failure thresholds are passed
      pipelineParams.FAILED_UNIT_TEST_UNSTABLE_THRESHOLD = pipelineParams.UNIT_TEST_FAILED_THRESHO
      pipelineParams.FAILED_UNIT_TEST_UNSTABLE_NEW_THRESHOLD = pipelineParams.UNIT_TEST_FAILED_NEW_THRESHOLD
   }

   //Validate and set JENKINS_CREDENTIALS
   pipelineParams.JENKINS_CREDENTIALS = pipelineParams.JENKINS_CREDENTIALS ?: 'JENKINS_CREDENTIALS'
   
   //Check that JENKINS_CREDENTIALS exist
   if (getStageData('UNIT_TEST')?.enabled && pipelineParams.UNIT_TEST_CHOICE == 'vstest') 
   {
      if (!utilMethods.checkCredentialsIdExists(pipelineParams.JENKINS_CREDENTIALS)) 
      {
         logger.logError("No credentials for the ID '${pipelineParams.JENKINS_CREDENTIALS}' are stored in Jenkins")
      }
   }
}

/** Validate/initialize all static code analysis stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateStaticAnalysisStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'STATIC_CODE_ANALYSIS_CHOICE', 'INVALID', true, 'STATIC_CODE_ANALYSIS', stageValidationMap['VALID_STATIC_CODE_ANALYSIS_TOOLS'])
   pipelineParams.SCA_PUBLISH_ARGS = pipelineParams.SCA_PUBLISH_ARGS ?: []

   //SonarQube Parameters
   pipelineParams.SONARQUBE_PROJECT_NAME = pipelineParams.SONARQUBE_PROJECT_NAME ?: 'PipelineProject'
   pipelineParams.SONARQUBE_PROJECT_KEY = pipelineParams.SONARQUBE_PROJECT_KEY ?: 'PIPELINE'
   validateParameter(pipelineParams, 'SONARQUBE_URL', 'INVALID', false, 'STATIC_CODE_ANALYSIS', null, [STATIC_CODE_ANALYSIS_CHOICE: ['sonarqube']])
   //If the user would like to use SonarQube for static code analysis, ensure they are not using our server.
   if (getStageData('STATIC_CODE_ANALYSIS')?.enabled && pipelineParams.STATIC_CODE_ANALYSIS_CHOICE == 'sonarqube') {
      if(!env.GIT_URL.contains("bitbucket/scm/devp/") && pipelineParams.SONARQUBE_URL.toLowerCase().contains("lnvle2289")) {
         logger.logError("SonarQube Server ${pipelineParams.SONARQUBE_URL} cannot be used on production projects.")
      }
   }
   pipelineParams.SCA_NEW_FAILURE_THRESHOLD = pipelineParams.SCA_NEW_FAILURE_THRESHOLD ?: ''
   pipelineParams.SCA_FAILURE_THRESHOLD = pipelineParams.SCA_FAILURE_THRESHOLD ?: ''
   pipelineParams.SCA_UNSTABLE_THRESHOLD = pipelineParams.SCA_UNSTABLE_THRESHOLD ?: ''
   pipelineParams.SCA_NEW_UNSTABLE_THRESHOLD = pipelineParams.SCA_NEW_UNSTABLE_THRESHOLD ?: ''
   if (getStageData('STATIC_CODE_ANALYSIS')?.enabled && pipelineParams.STATIC_CODE_ANALYSIS_CHOICE != 'klocwork') {  
      if(pipelineParams.SCA_NEW_UNSTABLE_THRESHOLD || pipelineParams.SCA_UNSTABLE_THRESHOLD) {
         logger.logError("Unstable threshold setting not applicable for Static Code Analysis tool: ${pipelineParams.STATIC_CODE_ANALYSIS_CHOICE}")
      }
      if(pipelineParams.STATIC_CODE_ANALYSIS_CHOICE != 'cppcheck' && (pipelineParams.SCA_NEW_FAILURE_THRESHOLD || pipelineParams.SCA_FAILURE_THRESHOLD)){
         logger.logError("Failure threshold setting not applicable for Static Code Analysis tool: ${pipelineParams.STATIC_CODE_ANALYSIS_CHOICE}")
      }
   }
   //Klocwork Parameters
   validateParameter(pipelineParams, 'PUBLISH_KLOCWORK_RESULTS', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   pipelineParams.PARSE_ERROR_LOG = pipelineParams.PARSE_ERROR_LOG ?: '.kwlp\\workingcache\\tables\\parse_errors.log'
   pipelineParams.BUILD_ERROR_LOG = pipelineParams.BUILD_ERROR_LOG ?: '.kwlp\\workingcache\\tables\\build.log'
   pipelineParams.KLOCWORK_PUBLISH_ARGS = pipelineParams.KLOCWORK_PUBLISH_ARGS ?: []
}

/** Validate/initialize all static application security testing stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateSASTStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'STATIC_APPLICATION_SECURITY_CHOICE', 'INVALID', true, 'STATIC_APPLICATION_SECURITY', stageValidationMap['VALID_STATIC_APPLICATION_SECURITY_TOOLS'])
   pipelineParams.SAST_PUBLISH_ARGS = pipelineParams.SAST_PUBLISH_ARGS ?: []
   pipelineParams.COVERITY_BIN = pipelineParams.COVERITY_BIN ?: ''
   pipelineParams.COVERITY_MAKE_COMPILER_TYPE = pipelineParams.COVERITY_MAKE_COMPILER_TYPE ?: 'g++'
   pipelineParams.STATIC_APPLICATION_SECURITY_RESULTS_FILE = pipelineParams.STATIC_APPLICATION_SECURITY_RESULTS_FILE ?: 'SastOutput.log'
   pipelineParams.SAST_NEW_FAILURE_THRESHOLD = pipelineParams.SAST_NEW_FAILURE_THRESHOLD ?: ''
   pipelineParams.SAST_FAILURE_THRESHOLD = pipelineParams.SAST_FAILURE_THRESHOLD ?: ''
   pipelineParams.SAST_UNSTABLE_THRESHOLD = pipelineParams.SAST_UNSTABLE_THRESHOLD ?: ''
   pipelineParams.SAST_NEW_UNSTABLE_THRESHOLD = pipelineParams.SAST_NEW_UNSTABLE_THRESHOLD ?: ''
   if (getStageData('STATIC_APPLICATION_SECURITY')?.enabled && "${pipelineParams.STATIC_APPLICATION_SECURITY_CHOICE}" != 'klocwork') {  
      if(pipelineParams.SAST_NEW_UNSTABLE_THRESHOLD || pipelineParams.SAST_UNSTABLE_THRESHOLD) {
         logger.logError("Unstable threshold setting not applicable for Static Application Security tool: ${pipelineParams.STATIC_APPLICATION_SECURITY_CHOICE}")
      }
      if(pipelineParams.SAST_NEW_FAILURE_THRESHOLD || pipelineParams.SAST_FAILURE_THRESHOLD){
         logger.logError("Failure threshold setting not applicable for Static Application Security tool: ${pipelineParams.STATIC_APPLICATION_SECURITY_CHOICE}")
      }
   }
   //Klocwork Parameters
   validateParameter(pipelineParams, 'PUBLISH_KLOCWORK_RESULTS', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   pipelineParams.PARSE_ERROR_LOG = pipelineParams.PARSE_ERROR_LOG ?: '.kwlp\\workingcache\\tables\\parse_errors.log'
   pipelineParams.BUILD_ERROR_LOG = pipelineParams.BUILD_ERROR_LOG ?: '.kwlp\\workingcache\\tables\\build.log'
}

/** Validate/initialize all code coverage stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateCodeCoverageStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'CODE_COVERAGE_CHOICE', 'INVALID', true, 'CODE_COVERAGE', stageValidationMap['VALID_CODE_COVERAGE_TOOLS'])
   validateParameter(pipelineParams, 'COVERAGE_FAIL_UNHEALTHY', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'COVERAGE_FAIL_UNSTABLE', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   pipelineParams.CONDITIONAL_COVERAGE_TARGETS = pipelineParams.CONDITIONAL_COVERAGE_TARGETS  ?: '0'
   pipelineParams.LINE_COVERAGE_TARGETS = pipelineParams.LINE_COVERAGE_TARGETS  ?: '0'
   pipelineParams.METHOD_COVERAGE_TARGETS = pipelineParams.METHOD_COVERAGE_TARGETS  ?: '0'
   pipelineParams.CODE_COVERAGE_PUBLISH_ARGS = pipelineParams.CODE_COVERAGE_PUBLISH_ARGS ?: []
   //Sets file type value used in gcov
   pipelineParams.GCOV_FILENAME_PATTERN = pipelineParams.GCOV_FILENAME_PATTERN ?: '*.cpp'
}

/** Validate/initialize all artifact management stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateArtifactManagementStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'ARTIFACT_MANAGEMENT_CHOICE', 'INVALID', true, 'ARTIFACT_MANAGEMENT', stageValidationMap['VALID_ARTIFACT_MANAGEMENT_TOOLS'])
   validateParameter(pipelineParams, 'ARTIFACT_LOCATION', 'INVALID', false, 'ARTIFACT_MANAGEMENT', null, [STATIC_CODE_ANALYSIS_CHOICE: ['spotbugs', 'spotbugs_cli', 'spotbugs_pom']])
   validateParameter(pipelineParams, 'ARTIFACT_REGEX_PATTERN_FLAG', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   pipelineParams.ARTIFACT_FILENAME_PATTERN = pipelineParams.ARTIFACT_FILENAME_PATTERN  ?: '*'
   validateParameter(pipelineParams, 'ARTIFACT_URL', 'INVALID', false, 'ARTIFACT_MANAGEMENT', null, [ARTIFACT_MANAGEMENT_CHOICE: ['generic', 'nuget', 'pypi', 'debian']])
   pipelineParams.ARTIFACT_DESTINATION_PATH = pipelineParams.ARTIFACT_DESTINATION_PATH  ?: ''
   validateParameter(pipelineParams, 'ARTIFACT_REPLICATE_LOCAL_PATH_FLAG', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'ARTIFACT_GENERATE_CHECKSUMS', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'ARTIFACT_PACKAGE_VALIDATE_CHECKSUM', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   pipelineParams.GROUP_ID = pipelineParams.GROUP_ID ?: ''
   pipelineParams.ARTIFACT_ID = pipelineParams.ARTIFACT_ID ?: ''
   validateParameter(pipelineParams, 'ARTIFACT_RECURSIVE_SEARCH', 'true', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'APPLY_OPTIONAL_ARTIFACT_MANAGEMENT_PROPERTIES', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   
   pipelineParams.VERSION_NUMBER = pipelineParams.VERSION_NUMBER?.trim() ?: ''
   pipelineParams.BUILD_NUMBER = pipelineParams.BUILD_NUMBER?.trim() ?: env.BUILD_NUMBER
   if (pipelineParams.BUILD_TOOL ==~ /^(msbuild|maven)$/ || getStageData('ARTIFACT_MANAGEMENT')?.enabled)
   {
      setVersionNumber(pipelineParams)
      if (getStageData('ARTIFACT_MANAGEMENT')?.enabled)
      {
         validateParameter(pipelineParams, 'VERSION_NUMBER', 'INVALID', false, 'ARTIFACT_MANAGEMENT')
      }
      else
      {
         validateParameter('VERSION_NUMBER', 'BUILD', 'BUILD_TOOL', pipelineParams.BUILD_TOOL, pipelineParams)
         validateParameter(pipelineParams, 'VERSION_NUMBER', 'INVALID', false, 'BUILD', null, [BUILD_TOOL: [pipelineParams.BUILD_TOOL]])
      }
   }
   validateParameter(pipelineParams, 'FORMAL_VERSION', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   pipelineParams.NUGET_SOURCE_NAME = pipelineParams.NUGET_SOURCE_NAME ?: 'ArtifactManager'
   validateParameter(pipelineParams, 'USE_NUGET', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   pipelineParams.ARTIFACT_CREDENTIALS = pipelineParams.ARTIFACT_CREDENTIALS ?: 'ARTIFACT_CREDENTIALS'
   if (getStageData('ARTIFACT_MANAGEMENT')?.enabled || getStageData('PROMOTE_ARTIFACT')?.enabled || pipelineParams.USE_NUGET == 'true' || (getStageData('BUILD')?.enabled && pipelineParams.BUILD_TOOL == 'maven')) 
   {
      setupArtifactCredentials(pipelineParams)
   }
}

/** Validate/initialize all functional test stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateFunctionalTestStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'FUNCTIONAL_TEST_CHOICE', 'INVALID', true, 'FUNCTIONAL_TEST', stageValidationMap['VALID_FUNCTIONAL_TEST_TOOLS'])
   pipelineParams.FUNCTIONAL_TEST_DIRECTORY = pipelineParams.FUNCTIONAL_TEST_DIRECTORY ?: 'functional'
   pipelineParams.FUNCTIONAL_TEST_TYPES = pipelineParams.FUNCTIONAL_TEST_TYPES ?: ''
   pipelineParams.FUNCTIONAL_TESTS_RESULTS_FILE = pipelineParams.FUNCTIONAL_TESTS_RESULTS_FILE ?: 'FunctionalTestResults'
   pipelineParams.TESTEXECUTE_ARGS = pipelineParams.TESTEXECUTE_ARGS ?: []
   pipelineParams.FUNCTIONAL_TEST_PUBLISH_ARGS = pipelineParams.FUNCTIONAL_TEST_PUBLISH_ARGS?: []

   // Set thresholds to not fail the build by default
   validateParameter(pipelineParams, 'FUNCTIONAL_TEST_SKIPPED_THRESHOLD_FAILS', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'FUNCTIONAL_TEST_FAILED_THRESHOLD_FAILS', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])

   // make functional test thresholds fail or set build to unstable depending on given parameters
   if(pipelineParams.FUNCTIONAL_TEST_SKIPPED_THRESHOLD_FAILS == 'true') 
   {  
      // Set xunit to fail pipeline if skipped thresholds are passed
      pipelineParams.SKIPPED_FUNCTIONAL_TEST_FAILURE_THRESHOLD = pipelineParams.FUNCTIONAL_TEST_SKIPPED_THRESHOLD
      pipelineParams.SKIPPED_FUNCTIONAL_TEST_FAILURE_NEW_THRESHOLD = pipelineParams.FUNCTIONAL_TEST_SKIPPED_NEW_THRESHOLD
   }
   else
   {  
      // Set xunit to make pipeline unstable if skipped thresholds are passed
      pipelineParams.SKIPPED_FUNCTIONAL_TEST_UNSTABLE_THRESHOLD = pipelineParams.FUNCTIONAL_TEST_SKIPPED_THRESHOLD
      pipelineParams.SKIPPED_FUNCTIONAL_TEST_UNSTABLE_NEW_THRESHOLD = pipelineParams.FUNCTIONAL_TEST_SKIPPED_NEW_THRESHOLD
   }

   if(pipelineParams.FUNCTIONAL_TEST_FAILED_THRESHOLD_FAILS == 'true') 
   {  
      // Set xunit to fail pipeline if failure thresholds are passed
      pipelineParams.FAILED_FUNCTIONAL_TEST_FAILURE_THRESHOLD = pipelineParams.FUNCTIONAL_TEST_FAILED_THRESHOLD
      pipelineParams.FAILED_FUNCTIONAL_TEST_FAILURE_NEW_THRESHOLD = pipelineParams.FUNCTIONAL_TEST_FAILED_NEW_THRESHOLD
   }
   else
   {  
      // Set xunit to make pipeline unstable if failure thresholds are passed
      pipelineParams.FAILED_FUNCTIONAL_TEST_UNSTABLE_THRESHOLD = pipelineParams.FUNCTIONAL_TEST_FAILED_THRESHOLD
      pipelineParams.FAILED_FUNCTIONAL_TEST_UNSTABLE_NEW_THRESHOLD = pipelineParams.FUNCTIONAL_TEST_FAILED_NEW_THRESHOLD
   }

   //Validate and set SLAVE_CREDENTIALS
   pipelineParams.SLAVE_CREDENTIALS = pipelineParams.SLAVE_CREDENTIALS ?: 'SLAVE_CREDENTIALS'

   //Check that SLAVE_CREDENTIALS exist
   if (getStageData('FUNCTIONAL_TEST')?.enabled && pipelineParams.FUNCTIONAL_TEST_CHOICE == 'testexecute')
   {
      if (!utilMethods.checkCredentialsIdExists(pipelineParams.SLAVE_CREDENTIALS))
      {
         logger.logError("No credentials for the ID '${pipelineParams.SLAVE_CREDENTIALS}' are stored in Jenkins")
      }
   }
}

/** Validate/initialize all dynamic application security testing stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateDASTStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'DYNAMIC_APPLICATION_SECURITY_CHOICE', 'INVALID', true, 'DYNAMIC_APPLICATION_SECURITY', stageValidationMap['VALID_DYNAMIC_APPLICATION_SECURITY_TOOLS'])
   pipelineParams.DYNAMIC_APPLICATION_SECURITY_TARGET = pipelineParams.DYNAMIC_APPLICATION_SECURITY_TARGET ?: ''
   pipelineParams.DYNAMIC_APPLICATION_SECURITY_CONFIG_FILE = pipelineParams.DYNAMIC_APPLICATION_SECURITY_CONFIG_FILE ?: ''
   pipelineParams.DYNAMIC_APPLICATION_SECURITY_ARGS = pipelineParams.DYNAMIC_APPLICATION_SECURITY_ARGS ?: ''
   pipelineParams.DYNAMIC_APPLICATION_SECURITY_REPORT_FILE = pipelineParams.DYNAMIC_APPLICATION_SECURITY_REPORT_FILE ?: 'DASTResults.html'
   pipelineParams.DYNAMIC_APPLICATION_SECURITY_PUBLISH_ARGS = pipelineParams.DYNAMIC_APPLICATION_SECURITY_PUBLISH_ARGS ?: []
}

/** Validate/initialize all artifact deployment stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateArtifactDeploymentStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'ANSIBLE_REPO_TYPE', 'separate', true, '', stageValidationMap['VALID_ANSIBLE_REPO_TYPE'])
   validateParameter(pipelineParams, 'ANSIBLE_DIRECTORY', 'INVALID', false, 'DEPLOY_ARTIFACTS', null, [ANSIBLE_REPO_TYPE: ['source']])
   pipelineParams.ANSIBLE_URL = pipelineParams.ANSIBLE_URL ?: 'https://lnsvr0329.gcsd.harris.com:8443/bitbucket/scm/devp/ansible.git'
   pipelineParams.ANSIBLE_BRANCH = pipelineParams.ANSIBLE_BRANCH ?: 'master'
   pipelineParams.ANSIBLE_PLAYBOOK_NAME = pipelineParams.ANSIBLE_PLAYBOOK_NAME ?: 'generic'
   validateParameter(pipelineParams, 'ANSIBLE_VARS_ENCRYPTED', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'ANSIBLE_PASSFILE_DIRECTORY', 'INVALID', false, 'DEPLOY_ARTIFACTS', null, [ANSIBLE_VARS_ENCRYPTED: ['true']])
   validateParameter(pipelineParams, 'ANSIBLE_PASSFILE_NAME', 'INVALID', false, 'DEPLOY_ARTIFACTS', null, [ANSIBLE_VARS_ENCRYPTED: ['true']])
   
   validateParameter(pipelineParams, 'ARTIFACT_DEST_FOLDER', 'INVALID', false, 'DEPLOY_ARTIFACTS', null, [ANSIBLE_REPO_TYPE: ['separate']])
   // Default deployment search pattern to ARTIFACT_FILENAME_PATTERN for deprecation
   validateParameter(pipelineParams, 'ARTIFACT_DEPLOYMENT_SEARCH_PATTERN', pipelineParams.ARTIFACT_FILENAME_PATTERN, false, 'DEPLOY_ARTIFACTS')
   // Default deployment search regex pattern to ARTIFACT_REGEX_PATTERN_FLAG for deprecation
   validateParameter(pipelineParams, 'ARTIFACT_DEPLOYMENT_SEARCH_REGEX_FLAG',  pipelineParams.ARTIFACT_REGEX_PATTERN_FLAG, true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'ARTIFACT_DEPLOYMENT_TOOL', 'ansible', true, '', stageValidationMap['VALID_ARTIFACT_DEPLOYMENT_TOOLS'])
   
   //Validate and set BITBUCKET_CREDENTIALS
   pipelineParams.BITBUCKET_CREDENTIALS = pipelineParams.BITBUCKET_CREDENTIALS ?: 'BITBUCKET_CREDENTIALS'

   //Check that BITBUCKET_CREDENTIALS exist
   if (getStageData('DEPLOY_ARTIFACTS')?.enabled && pipelineParams.ANSIBLE_REPO_TYPE != 'source')
   {
      if (!utilMethods.checkCredentialsIdExists(pipelineParams.BITBUCKET_CREDENTIALS)) 
      {
         logger.logError("No credentials for the ID '${pipelineParams.BITBUCKET_CREDENTIALS}' are stored in Jenkins")
      }
   }
}

/** Validate/initialize all style check stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateStyleCheckStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'STYLE_CHECK_CHOICE', 'INVALID', true, 'STYLE_CHECK', stageValidationMap['VALID_STYLE_CHECK_TOOLS'])
   pipelineParams.PYLINT_CONFIG_FILE = pipelineParams.PYLINT_CONFIG_FILE ?: ''
   pipelineParams.PYLINT_IGNORED_WARNINGS = pipelineParams.PYLINT_IGNORED_WARNINGS ?: 'none'
}

/** Validate/initialize all tag source code stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateTagSourceStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'SOURCE_CODE_TOOL', 'INVALID', true, 'TAG_SOURCE_CODE', stageValidationMap['VALID_SOURCE_CODE_TOOLS'])
   validateParameter(pipelineParams, 'RELEASE_TAG', 'INVALID', false, 'TAG_SOURCE_CODE')
}

/** Validate/initialize all FPGA compile stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateFpgaCompileStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'FPGA_COMPILE_TOOL', 'INVALID', true, 'FPGA_COMPILE', stageValidationMap['VALID_FPGA_COMPILE_TOOLS'])
   validateParameter(pipelineParams, 'FPGA_COMPILE_TCL_FILE', 'INVALID', false, 'FPGA_COMPILE')
}

/** Validate/initialize all FPGA lint stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateFpgaLintStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'FPGA_LINT_CHOICE', 'INVALID', true, 'FPGA_LINT', stageValidationMap['VALID_FPGA_LINT_TOOLS'])
   validateParameter(pipelineParams, 'FPGA_LINT_DIRECTORY', 'INVALID', false, 'FPGA_LINT')
   validateParameter(pipelineParams, 'FPGA_LINT_SCRIPT_FILE', 'INVALID', false, 'FPGA_LINT')
   pipelineParams.FPGA_LINT_REPORT_FILE = pipelineParams.FPGA_LINT_REPORT_FILE ?: ''
   pipelineParams.FPGA_LINT_WAIT_TIME = pipelineParams.FPGA_LINT_WAIT_TIME ?: '0'
}

/** Validate/initialize all FPGA CDC stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateFpgaCDCStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'FPGA_VERIFY_CDC_CHOICE', 'INVALID', true, 'FPGA_VERIFY_CDC', stageValidationMap['VALID_FPGA_VERIFY_CDC_TOOLS'])
   validateParameter(pipelineParams, 'FPGA_VERIFY_CDC_DIRECTORY', 'INVALID', false, 'FPGA_VERIFY_CDC')
   validateParameter(pipelineParams, 'FPGA_VERIFY_CDC_SCRIPT_FILE', 'INVALID', false, 'FPGA_VERIFY_CDC')
   pipelineParams.FPGA_VERIFY_CDC_REPORT_FILE = pipelineParams.FPGA_VERIFY_CDC_REPORT_FILE ?: ''
   pipelineParams.FPGA_VERIFY_CDC_WAIT_TIME = pipelineParams.FPGA_VERIFY_CDC_WAIT_TIME ?: '0'
}

/** Validate/initialize all promote artifact stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validatePromoteArtifactStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'ARTIFACT_MANAGEMENT_TOOL', 'INVALID', true, 'PROMOTE_ARTIFACT', stageValidationMap['VALID_RELEASE_ARTIFACT_MANAGEMENT_TOOLS'])
   validateParameter(pipelineParams, 'ARTIFACT_MANAGEMENT_TOOL_URL', 'INVALID', false, 'PROMOTE_ARTIFACT', null, [ARTIFACT_MANAGEMENT_TOOL: ['artifactory'], ARTIFACT_DEPLOYMENT_SEARCH_REGEX_FLAG: ['true']])
   validateParameter(pipelineParams, 'PATH_TO_ARTIFACT_TO_PROMOTE', 'INVALID', false, 'PROMOTE_ARTIFACT', null, [ARTIFACT_MANAGEMENT_TOOL: ['artifactory']])
   pipelineParams.FILE_NAME_OF_ARTIFACT_TO_PROMOTE = pipelineParams.FILE_NAME_OF_ARTIFACT_TO_PROMOTE ?: ''
   pipelineParams.ARTIFACT_PROMOTION_SUFFIX = pipelineParams.ARTIFACT_PROMOTION_SUFFIX ?: ''
   validateParameter(pipelineParams, 'APPEND_BEFORE_EXTENSION', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
}

/** Validate/initialize all archive artifact stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateArchiveArtifactStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'ARCHIVE_MODE', 'INVALID', true, 'ARCHIVE_ARTIFACT', stageValidationMap['VALID_MODE_TYPES'])
   validateParameter(pipelineParams, 'ARCHIVE_CHOICE', 'INVALID', true, 'ARCHIVE_ARTIFACT', stageValidationMap['VALID_ARCHIVE_CHOICE'])
   validateParameter(pipelineParams, 'PATH_TO_ARCHIVE_REPOSITORY', 'INVALID', false, 'ARCHIVE_ARTIFACT', null, [ARTIFACT_MANAGEMENT_TOOL: ['artifactory']])
   validateParameter(pipelineParams, 'PATTERN_TO_KEEP', 'INVALID', false, 'ARCHIVE_ARTIFACT', null, [ARTIFACT_MANAGEMENT_TOOL: ['artifactory']])
   validateParameter(pipelineParams, 'IGNORE_EMPTY_FILTERED_ARTIFACTS', 'false', true, 'ARCHIVE_ARTIFACT',  stageValidationMap['VALID_BOOLEAN_VALUES'])
}

/** Validate/initialize all Docker processing stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateDockerBuildStageParams(Map pipelineParams) {
   ['DOCKER_IMAGE_MANAGEMENT', 'DOCKER_IMAGE_BUILD'].each { stageKey ->
      validateParameter(pipelineParams, 'REGISTRY_URL', 'INVALID', false, stageKey)
      validateParameter(pipelineParams, 'DOCKER_IMAGE', 'INVALID', false, stageKey)
      validateParameter(pipelineParams, 'SOURCE_CODE_DIRECTORY', 'INVALID', false, stageKey)
      validateParameter(pipelineParams, 'DOCKER_REPO_URL', 'INVALID', false, stageKey)
   }
}

/** Validate/initialize all Docker processing stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateDockerPushStageParams(Map pipelineParams) {
   ['DOCKER_IMAGE_MANAGEMENT', 'DOCKER_IMAGE_PUSH'].each { stageKey ->
      validateParameter(pipelineParams, 'REGISTRY_URL', 'INVALID', false, stageKey)
      validateParameter(pipelineParams, 'DOCKER_REPO_URL', 'INVALID', false, stageKey)
      validateParameter(pipelineParams, 'DOCKER_TAG_PUSH', 'false', true, stageKey, stageValidationMap['VALID_BOOLEAN_VALUES'])
   }
}

/** Validate/initialize all container scan stage parameters
  *
  * Map pipelineParams: the parameters passed into runPipeline in the Jenkinsfile
 **/
def validateContainerScanStageParams(Map pipelineParams) {
   validateParameter(pipelineParams, 'CONTAINER_SCAN_CHOICE', 'INVALID', true, 'CONTAINER_SCAN', stageValidationMap['VALID_CONTAINER_SCAN_TOOLS'])
   pipelineParams.CONTAINER_SCAN_RESULTS_FILE = pipelineParams.CONTAINER_SCAN_RESULTS_FILE ?: 'ContainerScanResults.out'
   pipelineParams.CONTAINER_TO_SCAN = pipelineParams.CONTAINER_TO_SCAN?.toLowerCase()?: ''
}

def setupVariables(Map pipelineParams) {
   // Setting parameters from job must happen before any pipelineParams are used/validated
   setParametersFromJob(pipelineParams)
   
   // Allow use of deprecated USE_NUGET parameter
   if (pipelineParams.USE_NUGET == 'true')
   {
      pipelineParams.RUN_PREBUILD = 'true'
      pipelineParams.PREBUILD_TOOL = 'nuget'
   }

   // Set display name for the job
   if(pipelineParams['DISPLAY_NAME'])
   {
       currentBuild.displayName = "${pipelineParams['DISPLAY_NAME']}#${BUILD_NUMBER}"
   }
   
   validateRunStageParams(pipelineParams)

   //Custom Stage Directory Parameter
   pipelineParams.CUSTOM_STAGE_SCRIPTS_DIRECTORY = "${pipelineParams.CUSTOM_STAGE_SCRIPTS_DIRECTORY ?: '.'}"

   // Initialize stage data
   getStageData(pipelineParams)


   /* CONTINUOUS INTEGRATION (CI) RELATED STAGES */
   //Software CI Parameters
   //Build Parameters
   validateBuildStageParams(pipelineParams)

   //Create Installer Parameters
   validateCreateInstallerStageParams(pipelineParams)

   //Prebuild Parameters
   validatePrebuildParams(pipelineParams)

   //Unit Testing Parameters
   validateUnitTestStageParams(pipelineParams)

   //Static Code Analysis Parameters
   validateStaticAnalysisStageParams(pipelineParams)

   //Style Checking tool parameters
   validateStyleCheckStageParams(pipelineParams)

   //Static Application Security Parameters
   validateSASTStageParams(pipelineParams)

   //Dynamic Application Security Parameters
   validateDASTStageParams(pipelineParams)
   
   //Code Coverage Parameters
   validateCodeCoverageStageParams(pipelineParams)

   //Artifact Management Parameters
   validateArtifactManagementStageParams(pipelineParams)

   //FPGA CI Parameters
   // FPGA Compile Parameters
   validateFpgaCompileStageParams(pipelineParams)

   // FPGA Lint Parameters
   validateFpgaLintStageParams(pipelineParams)

   // FPGA CDC Verification Parameters
   validateFpgaCDCStageParams(pipelineParams)


   /* CONTINUOUS DEPLOYMENT (CD-1) RELATED STAGES */
   //Software Cd-1 Parameters
   // Deploy Artifact parameters
   validateArtifactDeploymentStageParams(pipelineParams)

   //Functional Test Parameters
   validateFunctionalTestStageParams(pipelineParams)


   /* CONTINUOUS DELIVERY (CD-2) RELATED STAGES */  
   //Tag Source Code Parameters
   validateTagSourceStageParams(pipelineParams)

   //Promote Artifact Parameters
   validatePromoteArtifactStageParams(pipelineParams)

   //Archive Artifact Parameters
   validateArchiveArtifactStageParams(pipelineParams)

   //Docker Processing Parameters
   validateDockerBuildStageParams(pipelineParams)
   validateDockerPushStageParams(pipelineParams)

   //Container Scan Parameters
   validateContainerScanStageParams(pipelineParams)

   /* Miscellaneous supporting parameters */
   // FPGA PRJ Parameters
   if(isUnix()) {
      validateParameter(pipelineParams, 'USE_PRJ_TOOL', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
      pipelineParams.PRJ_FILE = "${pipelineParams.PRJ_FILE ?: 'INVALID'}"
      ['FPGA_COMPILE', 'FPGA_LINT', 'FPGA_VERIFY_CDC'].each { stageKey ->
         validateParameter(pipelineParams, 'PRJ_FILE', 'INVALID', false, stageKey, null, [USE_PRJ_TOOL: ['true']])
      }
   }

   // TestExecute Specific parameters
   validateParameter(pipelineParams, 'TESTEXECUTE_PROJECT_SUITE_FILE', 'INVALID', false, 'FUNCTIONAL_TEST', null, [FUNCTIONAL_TEST_CHOICE: ['testexecute']])

   // make Specific Parameters
   pipelineParams.MAKEFILE_NAME = pipelineParams.MAKEFILE_NAME ?: 'Makefile'
   pipelineParams.MAKEFILE_TARGET = pipelineParams.MAKEFILE_TARGET ?: 'all'

   //Alternate branch parameter
   pipelineParams.ALTERNATE_BRANCH_REPO = env.ALTERNATE_BRANCH_REPO ?: env.GIT_BRANCH

   // vxworks Specific Parameters
   validateParameter(pipelineParams, 'WIND_HOME', 'INVALID', false, 'BUILD', null, [BUILD_TOOL: ['vxworks']])
   validateParameter(pipelineParams, 'VXWORKS_PACKAGE', 'INVALID', false, 'BUILD', null, [BUILD_TOOL: ['vxworks']])

   // msbuild Specific Parameters
   validateParameter(pipelineParams, 'SOLUTION_FILE', 'INVALID', false, 'BUILD', null, [BUILD_TOOL: ['msbuild'], PREBUILD_TOOL: ['nuget']])
   validateParameter(pipelineParams, 'SOLUTION_FILE', 'INVALID', false, 'STATIC_CODE_ANALYSIS', null, [STATIC_CODE_ANALYSIS_CHOICE: ['klocwork']])
   // Validate TEST_BINARY_PATTERN only if it will be used
   if (pipelineParams.UNIT_TEST_CHOICE == 'vstest' || pipelineParams.CODE_COVERAGE_CHOICE == 'opencover')
   {
      validateParameter(pipelineParams, 'TEST_BINARY_PATTERN', 'INVALID', false, 'BUILD', null, [BUILD_TOOL: ['msbuild']])
   }

   validateParameter(pipelineParams, 'BUILD_CONFIGURATION', 'INVALID', false, 'BUILD', null, [BUILD_TOOL: ['msbuild']])
   validateParameter(pipelineParams, 'BUILD_PLATFORM', 'INVALID', false, 'BUILD', null, [BUILD_TOOL: ['msbuild']])

   validateParameter(pipelineParams, 'NUSPEC_FILE', 'INVALID', false, 'BUILD', null, [ARTIFACT_MANAGEMENT_CHOICE: ['nuget']])
   validateParameter(pipelineParams, 'YAML_FILE', 'INVALID', false, 'PACKAGE_FOR_DELIVERY')
   
   //For Qmake builds, there is an issue when running with pre-3.1.5 kernels. The fix is to run a strip
   //command to remove a section that will disable the kernel check.
   //Here is the link: https://github.com/dnschneid/crouton/wiki/Fix-error-while-loading-shared-libraries:-libQt5Core.so.5
   if (fileExists("/DOCPF_DOCKER_VERSION") && (pipelineParams.PREBUILD_TOOL == 'qmake' || pipelineParams.UNIT_TEST_CHOICE == 'qttest')){
      def linuxDistribution = sh (script: "cat /DOCPF_DOCKER_VERSION", returnStdout: true).trim().toLowerCase()
      def fileToPatch
      if (linuxDistribution.contains('debian') || linuxDistribution.contains('ubuntu')) {
         fileToPatch = '/usr/lib/x86_64-linux-gnu/libQt5Core.so.5'
      } else if (linuxDistribution.contains('centos')) {
         fileToPatch = '/usr/lib64/libQt5Core.so.5.9.7'
      }
      sh """
         sudo strip --remove-section=.note.ABI-tag ${fileToPatch}
      """
   }

   //Job Notification parameter
   // Convert deprecated 'true'/'false' to updated 'always'/'never'
   if (!pipelineParams.SEND_NOTIFICATION || pipelineParams.SEND_NOTIFICATION.toLowerCase() == 'false') {
      pipelineParams.SEND_NOTIFICATION = 'never'
   }
   else if (pipelineParams.SEND_NOTIFICATION.toLowerCase() == 'true') {
      pipelineParams.SEND_NOTIFICATION = 'always'
   }
   validateParameter(pipelineParams, 'SEND_NOTIFICATION', 'INVALID', false, '', stageValidationMap['VALID_SEND_NOTIFICATION_VALUES'])
   
   //Workspace Cleanup parameter
   validateParameter(pipelineParams, 'POST_WORKSPACE_CLEANUP_STATUS', 'always', true, '', stageValidationMap['VALID_WORKSPACE_CLEANUP_JIRA_VALUES'])
   validateParameter(pipelineParams, 'PRE_WORKSPACE_CLEANUP_STATUS', 'false', true, '', stageValidationMap['VALID_BOOLEAN_VALUES'])
   validateParameter(pipelineParams, 'JIRA_COMMENT_CRITERIA', 'never', true, '', stageValidationMap['VALID_WORKSPACE_CLEANUP_JIRA_VALUES'])
}

def printVariables(Map pipelineParams) {
   //Display Docker container version
      versionFile = "/DOCPF_DOCKER_VERSION"
      if (isUnix()){
          if (fileExists("$versionFile")){
              DOCPF_DOCKER_VERSION = sh (
                  script: "cat $versionFile",
                  returnStdout: true
              ).trim()
              logger.logInfo("DOCPF_DOCKER_VERSION = ${DOCPF_DOCKER_VERSION}") 
          }
          else {
              logger.logInfo("DOCPF_DOCKER_VERSION = UNKNOWN")
          }
      }
      else {
          logger.logInfo("DOCPF_DOCKER_VERSION = WINDOWS") 
      }
   //Print each variable defined via the Jenkinsfile
   pipelineParams.each { key, val -> logger.logInfo("$key = $val") }
}

def setVersionNumber(Map pipelineParams) {
   if("${pipelineParams.VERSION_NUMBER}" == '') {
      if(pipelineParams.VERSION_FILE) {
         if(fileExists("${pipelineParams.VERSION_FILE}")) {
            pipelineParams.VERSION_NUMBER = readFile("${pipelineParams.VERSION_FILE}").trim()
         } else {
            logger.logError("Version file ${pipelineParams.VERSION_FILE} can not be found.")
         }
      } else {
         pipelineParams.VERSION_NUMBER = 'INVALID'
      }
   }
}

// Setup the Artifact credentials
def setupArtifactCredentials(Map pipelineParams) {
   if (!utilMethods.checkCredentialsIdExists("${pipelineParams.ARTIFACT_CREDENTIALS}")) 
   {
      // If credentials don't exist, try backwards compatible 'ARTIFACTORY_CREDENTIALS'
      if (utilMethods.checkCredentialsIdExists('ARTIFACTORY_CREDENTIALS')) 
      {  
         logger.logInfo("No credentials for the ID '${pipelineParams.ARTIFACT_CREDENTIALS}' found. Using 'ARTIFACTORY_CREDENTIALS'.")
         pipelineParams.ARTIFACT_CREDENTIALS = 'ARTIFACTORY_CREDENTIALS'
      }
      else
      {
         logger.logError("No credentials for the ID '${pipelineParams.ARTIFACT_CREDENTIALS}' or 'ARTIFACTORY_CREDENTIALS' are stored in Jenkins.")
      }
   }
}

// Convert 'AN_EXAMPLE_STRING' to 'exampleString.groovy'
def paramToGroovyFile(String string) {
   def tempStr = string.toLowerCase().split('_').collect{ it.capitalize() }.drop(1).join('') + ".groovy"
   return tempStr[0].toLowerCase() + tempStr.substring(1)
}


/** Parse a string in the format of a properties file into a map, which will preserve order.
  *
  * String propertiesString: a multiline string (probably the contents of a properties file)
  * Returns a map of the properties defined in propertiesString
  **/
def parsePropertiesStringToMap(String propertiesString) {
   def properties = [:]
   propertiesString.split(/(\r\n|\r|\n)/).each { line ->
      // Trim spaces from the front of the line
      def propsLine = line.replaceAll(/^\s*/, '')
      // Skip comments denoted by '#' and verifies the line is not empty
      if (propsLine && !(propsLine ==~ /^#.*$/))
      {
         // Possible assignment separators are '=', ':', and ' ' (unescaped)
         def propPartsList = propsLine.split(/(?<!\\)\s*[=:\s]/, 2)
         if (propPartsList.size() > 0) {
            // Build map entry with trimmed key and allowing for empty (null) values
            String key = propPartsList[0]
            // Allow for escaped spaces in key as well
            key = key.trim().replaceAll('\\\\', '')
            String value = propPartsList.size() == 2 ? propPartsList[1] : null
            // Trim whitespace from the front of the value, assume trailing whitespace is intentional
            // Default to title case formatted key if value is empty
            def defaultKey = key.toLowerCase().split('_').collect { it.capitalize() }.join(' ')
            value = value?.replaceAll(/^\s*['"](.+)['"]\s*$/, '$1') ?: defaultKey
            properties[value] = key
         }
      }
   }
   //Verifies that the stageData properties file is not empty
   if (!properties)
   {
      logger.logError("No properties found in stage properties file")
   }
   return properties
}

/** Attempt to load groovy file from pipelineParams.CUSTOM_STAGE_SCRIPTS_DIRECTORY to enable custom stage
  *
  * Map pipelineParams: the pipeline parameters containing provided stage data
  * String customStage: name of groovy file
  * Returns loaded custom stage script
  **/
def loadCustomStage(Map pipelineParams, String customStage) {
    //Find the path of groovy file that corresponds to the customStage input
    // Convert customStage to the correct naming convention of the groovy file and add to possible dir location
    def customStagePath = "${pipelineParams.CUSTOM_STAGE_SCRIPTS_DIRECTORY}/${paramToGroovyFile(customStage)}"
    def customStageScript
    logger.logInfo("CUSTOM_STAGE_SCRIPTS_DIRECTORY = ${pipelineParams.CUSTOM_STAGE_SCRIPTS_DIRECTORY}")
      try
      {
         customStageScript = load("${customStagePath}")
      }
      catch (Exception e)
      {
         logger.logError("Unable to load the custom stage ${customStage} from the following path: ${customStagePath}")
         logger.logError(e.toString())
      }
    return customStageScript
} 

// Initialize a value for the given key (stage name) in stageDataMap and set the applicable values
// If the key belongs to a custom stage, attempt to load the corresponding groovy stage file as the stageObject
def setStageDataItem(Map pipelineParams, String name, boolean enabled = true, String initialStageDataKey, String customAgentLabel, String parallelStageGroup) {
   if (initialStageDataKey =~ /^(CUSTOM_.*)/) {
      StageDataClass.stageDataMap[name] = [stageObject: loadCustomStage(pipelineParams, initialStageDataKey)]
   } else {
      StageDataClass.stageDataMap[name] = constants.INITIAL_STAGE_DATA[initialStageDataKey]
   }
   StageDataClass.stageDataMap[name]["initialStageDataKey"] = initialStageDataKey
   StageDataClass.stageDataMap[name]["enabled"] = enabled
   StageDataClass.stageDataMap[name]["customAgentLabel"] = customAgentLabel
   StageDataClass.stageDataMap[name]["parallelStageGroup"] = parallelStageGroup
}