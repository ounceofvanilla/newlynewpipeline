def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preFpgaVerifyCDC(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {
    // Switch to directory in which CDC verification project
    // will run and create its output files 
   dir("${pipelineParams.FPGA_VERIFY_CDC_DIRECTORY}") {
      // Executing different blocks of code depending on the FPGA CDC verification tool being used.
      switch(pipelineParams.FPGA_VERIFY_CDC_CHOICE) {
         case 'meridian':
               // If a non-Unix machine is executing the code, notify the user that it is not compatible.
               if (!isUnix()) {
                  logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": Meridian CDC, Windows")
               }

               def waitLicense = pipelineParams.FPGA_VERIFY_CDC_WAIT_TIME > 0 ? "-wait_license ${pipelineParams.FPGA_VERIFY_CDC_WAIT_TIME}" : ''
               utilMethods.invokeFpgaTool(pipelineParams, 
                                          "mcdc -i ${pipelineParams.FPGA_VERIFY_CDC_SCRIPT_FILE} ${waitLicense}")

            break
         default:
	    logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.FPGA_VERIFY_CDC_CHOICE, this.getClass().getSimpleName())
            break
      }
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   // Archive or publish FPGA CDC check reports
   switch(pipelineParams.FPGA_VERIFY_CDC_CHOICE) {
      case 'meridian':
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.FPGA_VERIFY_CDC_REPORT_FILE}"
         break

       default:
          logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.FPGA_VERIFY_CDC_CHOICE, this.getClass().getSimpleName())
          break
   }
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postFpgaVerifyCDC(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}