def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preUnitTest(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {  
   def commandStatus
   switch(pipelineParams.UNIT_TEST_CHOICE) {
      case 'googletest':
         // Set unit test results file if no value was given
         pipelineParams.UNIT_TEST_RESULTS_FILE = "${pipelineParams.UNIT_TEST_RESULTS_FILE ?: 'UnitTestResults.xml'}"
         if (isUnix()) 
         {
            if (pipelineParams.GT_TEST_BINARY)
            {
               // If given GT_TEST_BINARY (googletest executable), run that test file.
               commandStatus = sh(script: """
                                             ${pipelineParams.GT_TEST_BINARY} --gtest_output=xml:"${pipelineParams.UNIT_TEST_RESULTS_FILE}"
                                          """, returnStatus: true)
            }
            else
            {
               // Deprecated:
               // It not given the googletest executable, use find to execute all scripts in the specified directory and subdirectories.
               commandStatus = sh(script: """
                                             find '${pipelineParams.UNIT_TEST_DIRECTORY}' -type f -executable -exec bash -c {} \\+ | tee "${pipelineParams.UNIT_TEST_RESULTS_FILE}"
                                          """, returnStatus: true)
            }
         }
         else 
         {
            // Execute the test binary specifying that test results should be produced in a file.
            commandStatus = bat(script: """
                                          ${pipelineParams.GT_TEST_BINARY} --gtest_output=xml:"${pipelineParams.UNIT_TEST_RESULTS_FILE}"
                                       """, returnStatus: true)
         }
         
         if(commandStatus == 1) 
         {
            // Do not fail the build if unit tests fail
            // Set it to unstable and continue
            logger.logWarning("Unit" + constants.TEST_FAILURE_WARNING)
            currentBuild.result = 'UNSTABLE'
         }
         else if (commandStatus != 0)
         {
            logger.logError("googletest" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      case 'vstest':
         // Notify the user that this section is only compatible with Windows.
         if (isUnix()) {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": VSTest, Linux")
         }

         // Set default VSTest results file if vstest is enabled
         pipelineParams.UNIT_TEST_RESULTS_FILE = "${pipelineParams.UNIT_TEST_RESULTS_FILE ?: 'UnitTestResults.trx'}"

         // Use VSTest to execute the unit test binaries. It will run for all binaries that match the pattern specified by the user.
         // Since we output the binaries to the workspace, we know that the unit test binaries will be located in the workspace.
         // Check if OpenCover was selected
         if (pipelineParams.RUN_CODE_COVERAGE == 'true' && pipelineParams.CODE_COVERAGE_CHOICE == 'opencover')
         {
            echo "VSTest tests will be executed with OpenCover"
         }
         else
         {
            commandStatus = bat(script:"""
                                          SETLOCAL enableDelayedExpansion
                                          FOR %%f IN (${pipelineParams.TEST_BINARY_PATTERN}) do (SET CONTAINERS=!CONTAINERS! %%f)
                                          vstest.console.exe !CONTAINERS! /ResultsDirectory:"${env.workspace}" /logger:trx;LogFileName="${pipelineParams.UNIT_TEST_RESULTS_FILE}"
                                          SETLOCAL disableDelayedExpansion
                                       """, returnStatus: true)
         
            if(commandStatus == 1) 
            {
               // Do not fail the build if unit tests fail
               // Set it to unstable and continue
               logger.logWarning("Unit" + constants.TEST_FAILURE_WARNING)
               currentBuild.result = 'UNSTABLE'
            }
            else if (commandStatus != 0)
            {
               logger.logError("VSTest" + constants.TOOL_EXECUTION_FAILURE)
               currentBuild.result = 'FAILURE'
            }
         }
         break
      case 'junit':
         // Set unit test results file if no value was given
         pipelineParams.UNIT_TEST_RESULTS_FILE = "${pipelineParams.UNIT_TEST_RESULTS_FILE ?: "**\\\\target\\\\surefire-reports\\\\*.xml"}"
         //Set default coverage report folder if no value was given (no processing occurs during code coverage stage for jacoco)
         pipelineParams.CODE_COVERAGE_REPORT_FOLDER = "${pipelineParams.CODE_COVERAGE_REPORT_FOLDER ?: 'coverage'}"

         // Use the same command to run but execute with shell or Batch depending on the environment.
         def junit_command = "mvn test -gs settings.xml"
         if (isUnix()) {
               withEnv (["CODE_COVERAGE_REPORT_FOLDER=${pipelineParams.CODE_COVERAGE_REPORT_FOLDER}"]) {
               commandStatus = sh(script: "${junit_command}", returnStatus: true)
               }
         }
         else {
            withEnv (["CODE_COVERAGE_REPORT_FOLDER=${pipelineParams.CODE_COVERAGE_REPORT_FOLDER}"]) {
               commandStatus = bat(script: "${junit_command}", returnStatus: true)
            }
         }
         
         if (commandStatus == 1)
         {
            // Do not fail the build if unit tests fail
            // Set it to unstable and continue
            logger.logWarning("Unit" + constants.TEST_FAILURE_WARNING)
            currentBuild.result = 'UNSTABLE'
         }
         else if (commandStatus != 0)
         {
            logger.logError("JUnit" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      case 'unittest':
         // Use the same commands to run but execute with shell or Batch depending on the environment.
         def unittest_command = """-m unittest discover "${pipelineParams.UNIT_TEST_DIRECTORY}" """
         // Produce an XML report that will be compatible with the plugin.
         def xmlrunner_command = """-m xmlrunner discover -s "${pipelineParams.UNIT_TEST_DIRECTORY}" -o "${pipelineParams.UNIT_TEST_REPORT_FOLDER}" """
         if (isUnix()) {
            commandStatus = sh(script: """
                                          python3 ${unittest_command}
                                          python3 ${xmlrunner_command}
                                       """, returnStatus: true)
         }
         else {
            commandStatus = bat(script:"""
                                          python ${unittest_command}
                                          python ${xmlrunner_command}
                                       """, returnStatus: true)
         }

         if (commandStatus == 1) 
         {
            // Do not fail the build if unit tests fail
            // Set it to unstable and continue
            logger.logWarning("Unit" + constants.TEST_FAILURE_WARNING)
            currentBuild.result = 'UNSTABLE'
         }
         else if (commandStatus != 0)
         {
            logger.logError("Unittest" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      case 'pytest':
         // Set unit test results file if no value was given
         pipelineParams.UNIT_TEST_RESULTS_FILE = "${pipelineParams.UNIT_TEST_RESULTS_FILE ?: 'UnitTestResults.xml'}"

         def testTypes = pipelineParams.UNIT_TEST_TYPES ? " -m \"${pipelineParams.UNIT_TEST_TYPES}\" " : ''
         // Use the same command to run but execute with shell or Batch depending on the environment.
         def pytest_command = "-m pytest \"${pipelineParams.UNIT_TEST_DIRECTORY}\" -v --junitxml=\"${pipelineParams.UNIT_TEST_RESULTS_FILE}\"${testTypes}"
         if (isUnix()) {
            commandStatus = sh(script: "python3 ${pytest_command}", returnStatus: true)
         }
         else {
            commandStatus = bat(script: "python ${pytest_command}", returnStatus: true)
         }
         
         if (commandStatus == 1)
         {
            // Do not fail the build if unit tests fail
            // Set it to unstable and continue
            logger.logWarning("Unit" + constants.TEST_FAILURE_WARNING)
            currentBuild.result = 'UNSTABLE'
         }
         else if (commandStatus != 0)
         {
            logger.logError("PyTest" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      case 'qttest':
         // If a Windows machine is executing the code, notify the user that it is not compatible.
         if (!isUnix())
         {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": qttest, Windows")
         }
         try {
            // Set unit test results file if no value was given
            pipelineParams.UNIT_TEST_RESULTS_FILE = "${pipelineParams.UNIT_TEST_RESULTS_FILE ?: 'UnitTestResults.xml'}"

            // Use find to execute all scripts in the specified directory and subdirectories.
            sh """
               # Run the tests and send output to xml file
               find "${pipelineParams.UNIT_TEST_DIRECTORY}" -type f -executable -execdir {} -xml > "${pipelineParams.UNIT_TEST_RESULTS_FILE}" \\;
            """
         } catch (e) {
            logger.logError("QtTest" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      case 'ctest':
         // If a Windows machine is executing the code, notify the user that it is not compatible.
         if (!isUnix()){
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": ctest, Windows")
         }
         // If UNIT_TEST_RESULTS_FILE wasn't declared, set it to the default value.
         pipelineParams.UNIT_TEST_RESULTS_FILE = "${pipelineParams.UNIT_TEST_RESULTS_FILE ?: 'UnitTestResults.xml'}"
         // Enables --build-config parameter if a config file (CTEST_CONFIG_FILE_ARG) was specified.
         def configArg = pipelineParams.CTEST_CONFIG_FILE_ARG ? "--build-config ${pipelineParams.CTEST_CONFIG_FILE_ARG}" : ''
         // Enables --label-regex parameter if a regex pattern for labels (CTEST_LABEL_REGEX_INCLUDE_ARG) was specified.
         def labelRegexIncludeArg = pipelineParams.CTEST_LABEL_REGEX_INCLUDE_ARG ? "--label-regex ${pipelineParams.CTEST_LABEL_REGEX_INCLUDE_ARG}" : ''
         // Enables --label-exclude parameter if a regex pattern to be excluded on labels (CTEST_LABEL_REGEX_EXCLUDE_ARG) was specified.
         def labelRegexExcludeArg = pipelineParams.CTEST_LABEL_REGEX_EXCLUDE_ARG ? "--label-exclude ${pipelineParams.CTEST_LABEL_REGEX_EXCLUDE_ARG}" : ''
         // Enables --test-regex parameter if a regex pattern to include tests (CTEST_REGEX_INCLUDE_ARG) was specified.
         def testRegexIncludeArg = pipelineParams.CTEST_REGEX_INCLUDE_ARG ? "--test-regex ${pipelineParams.CTEST_REGEX_INCLUDE_ARG}" : ''
         // Enables --exclude-regex parameter if regex patter to exclude tests (CTEST_REGEX_EXCLUDE_ARG) was specified.
         def testRegexExcludeArg = pipelineParams.CTEST_REGEX_EXCLUDE_ARG ? "--exclude-regex ${pipelineParams.CTEST_REGEX_EXCLUDE_ARG}" : ''
         // Enables --repeat parameter if a repeat mode (CTEST_REPEAT_MODE_ARG) was specified.
         def repeatModeArg = pipelineParams.CTEST_REPEAT_MODE_ARG ? "--repeat ${pipelineParams.CTEST_REPEAT_MODE_ARG}" : ''
         // Enables --stop-on-failure parameter if flag (CTEST_STOP_ON_FAILURE_FLAG) was set to true.
         def stopOnFailureFlagArg = (pipelineParams.CTEST_STOP_ON_FAILURE_FLAG == 'true') ? "--stop-on-failure" : ''
         //Executes ctest with all required parameters and renames the default xml to specified value
         dir(pipelineParams.UNIT_TEST_DIRECTORY){
               commandStatus = sh(script: """
                  ctest ${configArg} ${labelRegexIncludeArg} ${labelRegexExcludeArg} ${testRegexIncludeArg} ${testRegexExcludeArg} ${repeatModeArg} ${stopOnFailureFlagArg} --no-compress-output -T Test
                  mv Testing/**/Test.xml ${pipelineParams.UNIT_TEST_RESULTS_FILE}
               """, returnStatus: true)
         }
         if (commandStatus == 1){
            // Do not fail the build if unit tests fail
            // Set it to unstable and continue
            logger.logWarning("Unit" + constants.TEST_FAILURE_WARNING)
            currentBuild.result = 'UNSTABLE'
         }
         else if (commandStatus != 0){ 
            logger.logError("Ctest" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.UNIT_TEST_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   // Default variables that are manipulated depending on the unit test tool.
   def JUNIT_TEST_RESULT_REPORT = ""
   def toolArguments
   def xunitClass
   // This section will change the above default values if necessary.
   // These need to be done before the junit plugin is run.
   switch(pipelineParams.UNIT_TEST_CHOICE) {
      case 'vstest':
         // Check if OpenCover was selected
         if (!(pipelineParams.RUN_CODE_COVERAGE == 'true' && pipelineParams.CODE_COVERAGE_CHOICE == 'opencover')) {
            if (!isUnix()) {
               toolArguments = [[$class: 'MSTestJunitHudsonTestType', deleteOutputFiles: true, failIfNotNew: true, pattern: '*.trx', skipNoTestFiles: false, stopProcessingIfError: true]]
            }
            archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.UNIT_TEST_RESULTS_FILE}"
         }
         break
      case 'googletest':
         if (!isUnix()) 
         {  
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.UNIT_TEST_RESULTS_FILE}"
            toolArguments = [[$class: 'GoogleTestType', deleteOutputFiles: false, failIfNotNew: false, pattern: "**\\${pipelineParams.UNIT_TEST_RESULTS_FILE}", skipNoTestFiles: false, stopProcessingIfError: true]]
         }
         break
      case 'junit':
         JUNIT_TEST_RESULT_REPORT = pipelineParams.UNIT_TEST_RESULTS_FILE
         break
      case 'pytest':
         JUNIT_TEST_RESULT_REPORT = "**\\${pipelineParams.UNIT_TEST_RESULTS_FILE}"
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.UNIT_TEST_RESULTS_FILE}"
         break
      case 'unittest':
         JUNIT_TEST_RESULT_REPORT = "**\\${pipelineParams.UNIT_TEST_REPORT_FOLDER}\\*.xml"
         archiveArtifacts allowEmptyArchive: true, artifacts: "${JUNIT_TEST_RESULT_REPORT}"
         break
      case 'qttest':
         JUNIT_TEST_RESULT_REPORT = "**\\${pipelineParams.UNIT_TEST_RESULTS_FILE}"
         toolArguments = [[$class: 'QTestLibType', deleteOutputFiles: false, failIfNotNew: false, pattern: "**\\${pipelineParams.UNIT_TEST_RESULTS_FILE}", skipNoTestFiles: false, stopProcessingIfError: true]]
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.UNIT_TEST_RESULTS_FILE}"
         break
      case 'ctest':
         if (isUnix()) 
         {  
            CTEST_TEST_RESULT_REPORT = "**\\${pipelineParams.UNIT_TEST_RESULTS_FILE}"
            archiveArtifacts allowEmptyArchive: true, artifacts: "${CTEST_TEST_RESULT_REPORT}"
            toolArguments = [[$class: 'CTestType', deleteOutputFiles: false, failIfNotNew: false, pattern: "${CTEST_TEST_RESULT_REPORT}", skipNoTestFiles: false, stopProcessingIfError: true]]
         }
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.UNIT_TEST_CHOICE, this.getClass().getSimpleName())
         break
   }

   // Set name of xunit class depending on plugin version
   if (utilMethods.checkPluginVersion("xunit","2.0.0", pipelineParams) < 0) {
      xunitClass = 'XUnitBuilder'
   }
   else {
      xunitClass = 'XUnitPublisher'
}

   // Don't overwrite the xunit tool arguments if the unit test framework is vstest or qttest
   if (!['vstest','qttest','googletest','ctest'].contains(pipelineParams.UNIT_TEST_CHOICE.toString()))
   {
      toolArguments = [[$class: 'JUnitType', deleteOutputFiles: false, failIfNotNew: true, pattern: "${JUNIT_TEST_RESULT_REPORT}", skipNoTestFiles: false, stopProcessingIfError: true]]
   }
   
   // Create the xunit parameters by expand the xunit class and test tool parameters
   def unittestPublishArgs =  [$class: "${xunitClass}", testTimeMargin: '3000', thresholdMode: 1, thresholds: [
                                 [$class: 'FailedThreshold', failureNewThreshold: "${pipelineParams.FAILED_UNIT_TEST_FAILURE_NEW_THRESHOLD}", failureThreshold: "${pipelineParams.FAILED_UNIT_TEST_FAILURE_THRESHOLD}", unstableNewThreshold: "${pipelineParams.FAILED_UNIT_TEST_UNSTABLE_NEW_THRESHOLD}", unstableThreshold: "${pipelineParams.FAILED_UNIT_TEST_UNSTABLE_THRESHOLD}"], 
                                 [$class: 'SkippedThreshold', failureNewThreshold: "${pipelineParams.SKIPPED_UNIT_TEST_FAILURE_NEW_THRESHOLD}", failureThreshold: "${pipelineParams.SKIPPED_UNIT_TEST_FAILURE_THRESHOLD}", unstableNewThreshold: "${pipelineParams.SKIPPED_UNIT_TEST_UNSTABLE_NEW_THRESHOLD}", unstableThreshold: "${pipelineParams.SKIPPED_UNIT_TEST_UNSTABLE_THRESHOLD}"]],
                               tools: toolArguments]
                                 
   step(unittestPublishArgs + pipelineParams.UNIT_TEST_PUBLISH_ARGS)
   
   // publish junit results to Zephyr
   if(pipelineParams.PUBLISH_TO_ZEPHYR == 'true'){
      zeeReporter createPackage: pipelineParams.ZEPHYR_CREATE_PACKAGE, cycleDuration: pipelineParams.ZEPHYR_CYCLE_DURATION, cycleKey: pipelineParams.ZEPHYR_CYCLE_KEY, cyclePrefix: '', parserTemplateKey: '1', projectKey: pipelineParams.ZEPHYR_PROJECT_KEY, releaseKey: pipelineParams.ZEPHYR_RELEASE_KEY, resultXmlFilePath: 'target/surefire-reports/TEST-hello.GreeterTest.xml', serverAddress: pipelineParams.ZEPHYR_SERVER_ADDRESS
   }
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postUnitTest(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}