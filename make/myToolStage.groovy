def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   logger.logInfo("Custom Tool Stage: Pre Stage Processing")
}

def doMainStageProcessing(Map pipelineParams) {
   logger.logInfo("Custom Tool Stage: Main Stage Processing")
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the custom tool stage example.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   logger.logInfo("Custom Tool Stage: Post Stage Processing")
   logger.logInfo("Current value of pipelineParams.CUSTOM_STAGE_SAMPLE_PARAM: ${pipelineParams.CUSTOM_STAGE_SAMPLE_PARAM}")
   // Pipeline parameters can change when running a stage multiple times
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}
return this