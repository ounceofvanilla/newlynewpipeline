def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preContainerScan(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {
   switch(pipelineParams.CONTAINER_SCAN_CHOICE) {
      case 'trivy':
         if (pipelineParams.DockerImage && !pipelineParams.CONTAINER_TO_SCAN) {
            pipelineParams.CONTAINER_TO_SCAN = pipelineParams.DOCKER_IMAGE
         }
         if (isUnix()) {
            sh "trivy image --output ${pipelineParams.CONTAINER_SCAN_RESULTS_FILE} ${pipelineParams.CONTAINER_TO_SCAN}"
         }
         else {
            bat "trivy image --output ${pipelineParams.CONTAINER_SCAN_RESULTS_FILE} ${pipelineParams.CONTAINER_TO_SCAN}"
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.CONTAINER_SCAN_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   switch(pipelineParams.CONTAINER_SCAN_CHOICE) {
      case 'trivy':
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.CONTAINER_SCAN_RESULTS_FILE}"
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.CONTAINER_SCAN_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postContainerScan(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}