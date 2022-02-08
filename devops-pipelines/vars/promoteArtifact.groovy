def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.prePromoteArtifact(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) { 
   switch(pipelineParams.ARTIFACT_MANAGEMENT_TOOL) {
      case 'artifactory':
         def oldFileName = ''
         def newFileName = ''
         // If the user did not specify a file name, they are promoting a directory.
         // This will rename the directory to append the artifact promotion ending.
         if (pipelineParams.FILE_NAME_OF_ARTIFACT_TO_PROMOTE == '') {
            int lastSlash = "${pipelineParams.PATH_TO_ARTIFACT_TO_PROMOTE}".lastIndexOf('/');
            oldFileName = "${pipelineParams.PATH_TO_ARTIFACT_TO_PROMOTE}".substring(lastSlash, pipelineParams.PATH_TO_ARTIFACT_TO_PROMOTE.length())
            logger.logInfo("Old artifact name: ${oldFileName}")
            newFileName =  "${oldFileName}" + "${pipelineParams.ARTIFACT_PROMOTION_SUFFIX}"
            logger.logInfo("New artifact name: ${newFileName}")
            pipelineParams.PATH_TO_ARTIFACT_TO_PROMOTE = pipelineParams.PATH_TO_ARTIFACT_TO_PROMOTE.replaceAll(oldFileName, '')
         }
         // If the user did specify a file name, they are promoting a single artifact.
         // This will rename the file to append the artifact promotion ending prior to the file extension.
         else {
            oldFileName = pipelineParams.FILE_NAME_OF_ARTIFACT_TO_PROMOTE
            logger.logInfo("Old artifact name: ${oldFileName}")
            int lastDot = "${pipelineParams.FILE_NAME_OF_ARTIFACT_TO_PROMOTE}".lastIndexOf('.');
            if (pipelineParams.APPEND_BEFORE_EXTENSION == 'false' || lastDot < 1) {
               newFileName =  "${oldFileName}${pipelineParams.ARTIFACT_PROMOTION_SUFFIX}"
            } else {
               newFileName = "${oldFileName.substring(0, lastDot)}${pipelineParams.ARTIFACT_PROMOTION_SUFFIX}${oldFileName.substring(lastDot)}"
            }
            logger.logInfo("New artifact name: ${newFileName}")
         }
         def apiUrl = "${pipelineParams.ARTIFACT_MANAGEMENT_TOOL_URL}/api"
         def pathToArtifact = pipelineParams.PATH_TO_ARTIFACT_TO_PROMOTE
         if (pipelineParams.ARTIFACT_PROMOTION_SUFFIX != '') {
            // Rename the artifact using the new artifact name discovered above
            utilMethods.curl(credentialsId: pipelineParams.ARTIFACT_CREDENTIALS,
                             requestMethod: 'POST',
                             url: "${apiUrl}/move/${pathToArtifact}/${oldFileName}?to=/${pathToArtifact}/${newFileName}")
         }
         // Assign properties to the artifact
         // If the user is promoting a directory, this will assign the property to the directory and all files in the directory.
         // If the user is promoting a single artifact, this will assign the property just to the artifact.
         pipelineParams.ARTIFACT_PROPERTIES?.each { key, val ->
            utilMethods.curl(credentialsId: pipelineParams.ARTIFACT_CREDENTIALS,
                             requestMethod: 'PUT',
                             url: "${apiUrl}/storage/${pathToArtifact}/${newFileName}?properties=${key}=${val}")
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.ARTIFACT_MANAGEMENT_TOOL, this.getClass().getSimpleName())
         break
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the promote artifact stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postPromoteArtifact(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}