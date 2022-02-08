def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preDockerImagePush(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {
   docker.withRegistry("${pipelineParams.REGISTRY_URL}") {
      def TAG = "${pipelineParams.DOCKER_REPO_BRANCH}".substring("${pipelineParams.DOCKER_REPO_BRANCH}".lastIndexOf('/') + 1)
      if (!pipelineParams.DockerImage) {
         logger.logError("No image to push.")
      }
      if (pipelineParams.DOCKER_TAG_PUSH == 'true') {
         pipelineParams.DockerImage.push("${TAG}")
      }
      def PUSH_LATEST = "${pipelineParams.TAG_LATEST?.toLowerCase() ?: 'false'}"

      if (PUSH_LATEST == 'true') {
         pipelineParams.DockerImage.push('latest')
      }    
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the docker image push stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postDockerImagePush(pipelineParams)
}