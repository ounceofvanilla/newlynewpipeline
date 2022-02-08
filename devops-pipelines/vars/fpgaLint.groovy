def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preFpgaLint(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {
    // Switch to directory in which lint project
    // will run and create its output files 
   dir("${pipelineParams.FPGA_LINT_DIRECTORY}") {
      // Executing different blocks of code depending on the FPGA lint tool being used.
      switch(pipelineParams.FPGA_LINT_CHOICE) {
         case 'ascent':
               // If a non-Unix machine is executing the code, notify the user that it is not compatible.
               if (!isUnix()) {
                  logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": Ascent Lint, Windows")
               }

               def waitLicense = pipelineParams.FPGA_LINT_WAIT_TIME > 0 ? "-wait_license ${pipelineParams.FPGA_LINT_WAIT_TIME}" : ''
               utilMethods.invokeFpgaTool(pipelineParams, 
                                          "ascentlint -i ${pipelineParams.FPGA_LINT_SCRIPT_FILE} ${waitLicense}")

            break
         default:
	    logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.FPGA_LINT_CHOICE, this.getClass().getSimpleName())
            break
      }
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   // Archive or publish FPGA linting reports
   switch(pipelineParams.FPGA_LINT_CHOICE) {
      case 'ascent':
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.FPGA_LINT_REPORT_FILE}"
         break

       default:
          logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.FPGA_LINT_CHOICE, this.getClass().getSimpleName())
          break
   }
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postFpgaLint(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}