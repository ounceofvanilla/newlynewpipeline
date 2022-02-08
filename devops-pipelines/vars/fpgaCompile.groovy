def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preFpgaCompile(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) { 
   dir("${pipelineParams.PROJECT_ROOT}") {
      // Executing different blocks of code depending on the FPGA design tool being used.
      switch(pipelineParams.FPGA_COMPILE_TOOL) {
         case 'quartus':
               // If a non-Unix machine is executing the code, notify the user that it is not compatible.
               if (!isUnix()) {
                  logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": Quartus, Windows")
               }

               utilMethods.invokeFpgaTool(pipelineParams, 
                              "quartus_sh -t '${pipelineParams.FPGA_COMPILE_TCL_FILE}'")
            break
         case 'vivado':
               // If a non-Unix machine is executing the code, notify the user that it is not compatible.
               if (!isUnix()) {
                  logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": Vivado, Windows")
               }
               
               utilMethods.invokeFpgaTool(pipelineParams, 
                              "vivado -mode batch -nojournal -notrace -source '${pipelineParams.FPGA_COMPILE_TCL_FILE}'")
            break
         default:
	    logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.FPGA_COMPILE_TOOL, this.getClass().getSimpleName())
            break
      }
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the build stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postFpgaCompile(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}