def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preFunctionalTest(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) { 
   def commandStatus

   // Executing different blocks of code depending on the functional test tool being used.
   switch(pipelineParams.FUNCTIONAL_TEST_CHOICE) {
      case 'testcomplete':
      case 'testexecute':
         if(!isUnix()) 
         {  
            def testCompleteArgs = [suite: "${pipelineParams.TESTEXECUTE_PROJECT_SUITE_FILE}", useTCService: true, credentialsId: "${pipelineParams.SLAVE_CREDENTIALS}",
                                    commandLineArguments: """ /ExportSummary:"${env.WORKSPACE}\\${pipelineParams.FUNCTIONAL_TESTS_RESULTS_FILE}" """]
            testcompletetest testCompleteArgs + pipelineParams.TESTEXECUTE_ARGS
         } else {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": TestExecute/TestComplete, Linux")
         }
         break
      case 'pytest':
         def testTypes = pipelineParams.FUNCTIONAL_TEST_TYPES ? " -m \"${pipelineParams.FUNCTIONAL_TEST_TYPES}\" " : ''
         def pytest_command = "-m pytest \"${pipelineParams.FUNCTIONAL_TEST_DIRECTORY}\" -v --junitxml=\"${pipelineParams.FUNCTIONAL_TESTS_RESULTS_FILE}\"${testTypes}"
         if (isUnix()) 
         {
            commandStatus = sh(script: "python3 ${pytest_command}", returnStatus: true)
         }
         else 
         {
            commandStatus = bat(script: "python ${pytest_command}", returnStatus: true)
         }

         if (commandStatus == 1) 
         {
            // Do not fail the build if unit tests fail
            // Set it to unstable and continue
            logger.logWarning("Functional" + constants.TEST_FAILURE_WARNING)
            currentBuild.result = 'UNSTABLE'
         } 
         else if (commandStatus != 0)
         {
            logger.logError("PyTest" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.FUNCTIONAL_TEST_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   def toolArguments
   def xunitClass

   switch(pipelineParams.FUNCTIONAL_TEST_CHOICE) {
      case 'testcomplete':
      case 'testexecute':
         break
      case 'pytest':
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.FUNCTIONAL_TESTS_RESULTS_FILE}"
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.FUNCTIONAL_TEST_CHOICE, this.getClass().getSimpleName())
         break
   }
   // Check xUnit plugin version
   if (utilMethods.checkPluginVersion("xunit","2.0.0", pipelineParams) < 0) 
   {
      xunitClass = 'XUnitBuilder'
}
   else
   {
      xunitClass = 'XUnitPublisher'
   }

   def functionalPublishArgs =  [$class: "${xunitClass}", testTimeMargin: '3000', thresholdMode: 1, thresholds: [
                                    [$class: 'FailedThreshold', failureNewThreshold: "${pipelineParams.FAILED_FUNCTIONAL_TEST_FAILURE_NEW_THRESHOLD}", failureThreshold: "${pipelineParams.FAILED_FUNCTIONAL_TEST_FAILURE_THRESHOLD}", 
                                       unstableNewThreshold: "${pipelineParams.FAILED_FUNCTIONAL_TEST_UNSTABLE_NEW_THRESHOLD}", unstableThreshold: "${pipelineParams.FAILED_FUNCTIONAL_TEST_UNSTABLE_THRESHOLD}"],
                                    [$class: 'SkippedThreshold', failureNewThreshold: "${pipelineParams.SKIPPED_FUNCTIONAL_TEST_FAILURE_NEW_THRESHOLD}", failureThreshold: "${pipelineParams.SKIPPED_FUNCTIONAL_TEST_FAILURE_THRESHOLD}", 
                                       unstableNewThreshold: "${pipelineParams.SKIPPED_FUNCTIONAL_TEST_UNSTABLE_NEW_THRESHOLD}", unstableThreshold: "${pipelineParams.SKIPPED_FUNCTIONAL_TEST_UNSTABLE_THRESHOLD}"]], 
                                 tools: [[$class: 'JUnitType', deleteOutputFiles: true, failIfNotNew: true, pattern: "**\\${pipelineParams.FUNCTIONAL_TESTS_RESULTS_FILE}", skipNoTestFiles: false, stopProcessingIfError: true]]]
   step(functionalPublishArgs + pipelineParams.FUNCTIONAL_TEST_PUBLISH_ARGS)
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postFunctionalTest(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}