def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preStyleCheck(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) { 
   switch (pipelineParams.STYLE_CHECK_CHOICE)
   {
      case 'stylecop':
         // If a Unix machine is executing the code, it is not compatible.
         if (isUnix()) 
         {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": StyleCop, Linux")
         }
         // Check NuGet is being run to add StyleCop package to project
         if (pipelineParams.RUN_PREBUILD == 'false' || pipelineParams.PREBUILD_TOOL != 'nuget')
         {
            logger.logError('Pre Build tool must be NuGet to run StyleCop.')
         }
         // StyleCop is run when building the source code with MSBuild
         break

      case 'checkstyle':
         pipelineParams.STYLE_CHECK_RESULTS_FILE = "${pipelineParams.STYLE_CHECK_RESULTS_FILE ?: 'styleCheckResults.xml'}"

         // If style configuration choice is not custom, set the style configuration path from the selected choice
         if (pipelineParams.STYLE_CONFIG_CHOICE != 'custom' && pipelineParams.STYLE_CONFIG_FILEPATH == '')
         {
            pipelineParams.STYLE_CONFIG_FILEPATH = "${pipelineParams.STYLE_CONFIG_CHOICE}_checks.xml"
         }
         if (isUnix()) 
         {  
            sh """
                  java -jar /opt/checkstyle/checkstyle-8.31-all.jar -c "${pipelineParams.STYLE_CONFIG_FILEPATH}" -f xml "${pipelineParams.SOURCE_CODE_DIRECTORY}" | tee "${pipelineParams.STYLE_CHECK_RESULTS_FILE}"
               """
         }
         else
         {  
            bat """
                   java -jar C:\\checkstyle\\checkstyle-8.31-all.jar -c "${pipelineParams.STYLE_CONFIG_FILEPATH}" -f xml "${pipelineParams.SOURCE_CODE_DIRECTORY}" > "${pipelineParams.STYLE_CHECK_RESULTS_FILE}"
                   type "${pipelineParams.STYLE_CHECK_RESULTS_FILE}"
                """
         }
         break
      
      case 'pylint':
         pipelineParams.STYLE_CHECK_RESULTS_FILE = "${pipelineParams.STYLE_CHECK_RESULTS_FILE ?: 'styleCheckResults'}"

         // define pylint command used by both Windows and Linux
         def pylintCommand =  """ 
                              pylint "${pipelineParams.SOURCE_CODE_DIRECTORY}" --rcfile="${pipelineParams.PYLINT_CONFIG_FILE}" --output-format=parseable --disable=${pipelineParams.PYLINT_IGNORED_WARNINGS}
                           """
         
         def commandStatus = -1

         if (isUnix()) 
         {  
            // Run pylint command and pass output to a file. Receive the exit code of pylint as a value
            commandStatus = sh(script: """
                                          ${pylintCommand.trim()} > "${pipelineParams.STYLE_CHECK_RESULTS_FILE}" 
                                       """, returnStatus: true)
         }
         else
         {
            // Run pylint command and pass output to a file. Receive the exit code of pylint as a value
            commandStatus = bat(script: """ python -m ${pylintCommand.trim()} 1> "${pipelineParams.STYLE_CHECK_RESULTS_FILE}" """, returnStatus: true) 
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
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.STYLE_CHECK_CHOICE, this.getClass().getSimpleName())
         break
         
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   switch (pipelineParams.STYLE_CHECK_CHOICE)
   {
      case 'stylecop':
         break
      case 'checkstyle':
      case 'pylint':
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.STYLE_CHECK_RESULTS_FILE}"
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.STYLE_CHECK_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postStyleCheck(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}