def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preDockerImageManagement(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) { 
   script {
      dir('DOCPF_DOCKER'){
         dockerImageBuild.doMainStageProcessing(pipelineParams)
         dockerImagePush.doMainStageProcessing(pipelineParams)
      }    
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the docker image management stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postDockerImageManagement(pipelineParams)
}