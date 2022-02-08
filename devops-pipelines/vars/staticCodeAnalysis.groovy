def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preSCA(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {  
   switch(pipelineParams.STATIC_CODE_ANALYSIS_CHOICE) {
      case 'sonarqube':
         if(pipelineParams.BUILD_TOOL == "msbuild") {
            // Notify the user that MSBuild is not compatible with Unix if applicable.
            if (isUnix()) {
               logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": MSBuild, Linux")
            }
            // Notify SonarScanner that MSBuild will be used to build the source code.
            def runVcvarsBatch = pipelineParams.VCVARS_BATCH_FILE ? "call \"${pipelineParams.VCVARS_BATCH_FILE}\" ${pipelineParams.VCVARS_ARCH} ${pipelineParams.VCVARS_PLATFORM} ${pipelineParams.VCVARS_WINSDK} ${pipelineParams.VCVARS_VCVER}" : ""
            bat """
               ${runVcvarsBatch}
               SonarScanner.MSBuild.exe begin /k:\"org.sonarqube:sonarqube-scanner-msbuild\" /n:\"${pipelineParams.SONARQUBE_PROJECT_NAME}\" /v:\"${pipelineParams.BUILD_NUMBER}\" /d:sonar.host.url="${pipelineParams.SONARQUBE_URL}"
               rem Build the source code using MSBuild.
               MSBuild.exe "${pipelineParams.SOLUTION_FILE}" /t:Rebuild /p:Configuration=${pipelineParams.BUILD_CONFIGURATION} /p:Platform=\"${pipelineParams.BUILD_PLATFORM}\"
               rem Notify SonarScanner not to track future MSBuild runs.
               SonarScanner.MSBuild.exe end
            """
         }
         else if (pipelineParams.BUILD_TOOL == "maven") {
            // Use the same command to run but execute with shell or Batch depending on the environment.
            def sonarqube_command = """mvn sonar:sonar -Dsonar.host.url="${pipelineParams.SONARQUBE_URL}" -gs settings.xml"""
            if (isUnix()) {
               sh "${sonarqube_command}"
            }
            else {
               bat "${sonarqube_command}"
            }
         }
         else if (pipelineParams.BUILD_TOOL == "setuptools") {
            // Use the same command to run but execute with shell or Batch depending on the environment.
            def sonarqube_command = """sonar-scanner -Dsonar.projectKey=${pipelineParams.SONARQUBE_PROJECT_KEY} -Dsonar.sources="${pipelineParams.SOURCE_CODE_DIRECTORY}" -Dsonar.host.url="${pipelineParams.SONARQUBE_URL}" """
            if (isUnix()) {
               sh "${sonarqube_command}"
            } 
            else {
               bat "${sonarqube_command}"
            }
         }
         break
      case 'klocwork':
         // Notify the user that this section is only compatible with Windows.
         if (isUnix()) {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": Klocwork, Linux")
         }
         // Create the Klocwork project.
         def runVcvarsBatch = pipelineParams.VCVARS_BATCH_FILE ? "call \"${pipelineParams.VCVARS_BATCH_FILE}\" ${pipelineParams.VCVARS_ARCH} ${pipelineParams.VCVARS_PLATFORM} ${pipelineParams.VCVARS_WINSDK} ${pipelineParams.VCVARS_VCVER}" : ""
         bat """
            ${runVcvarsBatch}
            kwcheck create
            rem Create the build specification to use when running static code analysis on this code.
            kwinject -o kwinject.out --enable-csproj MSBuild.exe "${pipelineParams.SOLUTION_FILE}" /p:Configuration=${pipelineParams.BUILD_CONFIGURATION} /p:Platform=\"${pipelineParams.BUILD_PLATFORM}\"
            rem Run static code analysis using the build specification created above.
            kwcheck run -b kwinject.out
            rem Output the Klocwork results to an XML file.
            kwcheck list -F xml > kw_results.out
         """
    	 if (pipelineParams.PUBLISH_KLOCWORK_RESULTS == 'true') {
            // This file is used to turn the Klocwork results into a Junit file for the Jenkins plugin to read.
         	final pythonContent = libraryResource('static-code-analysis/kw_junit.py')
         	writeFile(file: 'static-code-analysis/kw_junit.py', text: pythonContent)
           
         // Use the copied Python script to change the result file to Junit format.
         bat "python static-code-analysis\\kw_junit.py"
         }
         break
      case 'cppcheck':
         // Set default results file if no value was given
         pipelineParams.STATIC_CODE_ANALYSYS_RESULTS = "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS ?: 'CPPCheckResults.xml'}"

         // Notify the user that this section is only compatible with Unix.
         if (isUnix()) {
            sh """
               cppcheck --force -j 3 --xml --xml-version=2 "${pipelineParams.SOURCE_CODE_DIRECTORY}" 2> "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS}"
            """
         }
     	 else {
            bat """
               cppcheck --force -j 3 --xml --xml-version=2 "${pipelineParams.SOURCE_CODE_DIRECTORY}" 2> "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS}"
            """
         }
         break
      case 'pylint':
         // Set default results file if no value was given
         pipelineParams.STATIC_CODE_ANALYSYS_RESULTS = "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS ?: 'PylintScaResults.txt'}"   
         // define pylint command used by both Windows and Linux
         def pylintCommand =  """ 
                              pylint "${pipelineParams.SOURCE_CODE_DIRECTORY}" --rcfile="${pipelineParams.PYLINT_CONFIG_FILE}" --output-format=parseable --disable=${pipelineParams.PYLINT_IGNORED_WARNINGS}
         """
         def commandStatus
         if (isUnix()) 
         {  
            // Run pylint command and pass output to a file. Receive the exit code of pylint as a value
            commandStatus = sh(script: """
                                          ${pylintCommand.trim()} > "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS}" 
                                       """, returnStatus: true)
         }
         else
         {
            // Run pylint command and pass output to a file. Receive the exit code of pylint as a value
            commandStatus = bat(script: """ python -m ${pylintCommand.trim()} 1> "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS}" """, returnStatus: true) 
         }
         // Check what exit code the shell command returns and set build status accordingly
         if (commandStatus in [2, 4, 8, 16, 20]) 
         {
            logger.logWarning("Pylint" + constants.TEST_FAILURE_WARNING)
            currentBuild.result = 'UNSTABLE'
         }
         else if (commandStatus != 0)
         {
            currentBuild.result = 'FAILURE'
         }

         break
      case 'spotbugs':
      case 'spotbugs_cli':
         // Set default results file if no value was given
         pipelineParams.STATIC_CODE_ANALYSYS_RESULTS = "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS ?: 'report.html'}"

         // Use the same command to run but execute with shell or Batch depending on the environment.
         def spotbugs_command = """spotbugs -textui -html -outputFile ${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS} "${pipelineParams.ARTIFACT_LOCATION}/" """
         if (isUnix()) {
            sh "${spotbugs_command}"
         } 
         else {
            bat "${spotbugs_command}"
         }
         break
      case 'spotbugs_pom':
         if (isUnix()) {
            sh "mvn compile site"
         } 
         else {
            bat "mvn compile site"
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.STATIC_CODE_ANALYSIS_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   // This block will properly display the static code analysis results depending on the tool that was used.
   switch(pipelineParams.STATIC_CODE_ANALYSIS_CHOICE) {
      case 'klocwork':
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.PARSE_ERROR_LOG}"
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.BUILD_ERROR_LOG}"

     	   if (pipelineParams.PUBLISH_KLOCWORK_RESULTS == 'true') {
            toolArguments = [[$class: 'JUnitType', deleteOutputFiles: true, failIfNotNew: true, pattern: '**\\kw_output.xml', skipNoTestFiles: false, stopProcessingIfError: true]]
            if (utilMethods.checkPluginVersion("xunit","2.0.0", pipelineParams) < 0) {
               xunitClass = 'XUnitBuilder'
            }
            else {
               xunitClass = 'XUnitPublisher'
            }
            def scaPublishArgs =  [$class: "${xunitClass}", testTimeMargin: '3000', thresholdMode: 1, thresholds: [
                                          [$class: 'FailedThreshold', failureNewThreshold: "${pipelineParams.SCA_NEW_FAILURE_THRESHOLD}", failureThreshold: "${pipelineParams.SCA_FAILURE_THRESHOLD}", unstableNewThreshold: "${pipelineParams.SCA_NEW_UNSTABLE_THRESHOLD}", unstableThreshold: "${pipelineParams.SCA_UNSTABLE_THRESHOLD}"]],
                                       tools: toolArguments]
            step(scaPublishArgs +  pipelineParams.SCA_PUBLISH_ARGS)
         }
         break
      case 'cppcheck':
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS}"

         def cppcheckPublishArgs = [allowNoReport: true, pattern: "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS}", failureThreshold: "${pipelineParams.SCA_FAILURE_THRESHOLD}", newFailureThreshold: "${pipelineParams.SCA_NEW_FAILURE_THRESHOLD}"]
         publishCppcheck cppcheckPublishArgs + pipelineParams.SCA_PUBLISH_ARGS
         break
      case 'pylint':
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS}"
         break
      case 'spotbugs':
      case 'spotbugs_cli':
      case 'spotbugs_pom':
         def spotbugsPublishArgs =  [allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: "", 
                                     reportFiles: "${pipelineParams.STATIC_CODE_ANALYSYS_RESULTS}", reportName: 'Static Code Analysis Report', reportTitles: 'Static Code Analysis']
         
         publishHTML(spotbugsPublishArgs + pipelineParams.SCA_PUBLISH_ARGS)
         break
      case 'sonarqube':
         // Since SonarQube is a valid option, do not fail the build.
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.STATIC_CODE_ANALYSIS_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postSCA(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}