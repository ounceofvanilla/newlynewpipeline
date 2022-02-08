def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preArchiveArtifacts(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) { 
   switch(pipelineParams.ARTIFACT_MANAGEMENT_TOOL) {
      case 'artifactory':
         // Obtain all files located in the PATH_TO_ARTIFACT_TO_PROMOTE
         def apiUrl = "${pipelineParams.ARTIFACT_MANAGEMENT_TOOL_URL}/api/storage/${pipelineParams.PATH_TO_ARTIFACT_TO_PROMOTE}"
         def response = utilMethods.curl(credentialsId: pipelineParams.ARTIFACT_CREDENTIALS, url: apiUrl, returnStdout: true)
         def fileUris = utilMethods.getFileUrisFromJsonText(response)
         
         def filesToArchive = []
         if (pipelineParams.ARCHIVE_CHOICE == 'name') {
            // Create list of files that will be archived based on the filename pattern
            filesToArchive = fileUris.findAll{ element -> !element.matches(pipelineParams.PATTERN_TO_KEEP) }
         } else if (pipelineParams.ARCHIVE_CHOICE == 'properties') {
            // Create list of files that will be archived based on the properties the files contain
            filesToArchive = getFilesWithoutProperties(fileUris, pipelineParams, apiUrl)
         }
         // Iterate through the list of files to archive and use the move api call
         apiUrl = apiUrl.replaceFirst('/api/storage/', '/api/move/')
         filesToArchive.each { file ->
            logger.logInfo("The following file will be archived: ${file}")
            // If the user specified they want to use the archive mode, move the file from ARTIFACT_MANAGEMENT_TOOL_URL to PATH_TO_ARCHIVE_REPOSITORY
            // The file will still follow the same PATH_TO_ARTIFACT_TO_PROMOTE in the new archive repository (PATH_TO_ARCHIVE_REPOSITORY)
            if(pipelineParams.ARCHIVE_MODE == 'archive') {
               def fileMoveUrl = "${apiUrl}/${file}?to=/${pipelineParams.PATH_TO_ARCHIVE_REPOSITORY}${file}"
               utilMethods.curl(credentialsId: pipelineParams.ARTIFACT_CREDENTIALS, url: fileMoveUrl, requestMethod: "POST")
            }
         }
         // If there are no files to be archived, alert the user
         if(filesToArchive.isEmpty()) {
            if (pipelineParams.IGNORE_EMPTY_FILTERED_ARTIFACTS == 'true'){
               logger.logWarning("No files will be archived")
               currentBuild.result = 'UNSTABLE'
            } 
            else {
               logger.logError("No files will be archived")
            }
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.ARTIFACT_MANAGEMENT_TOOL, this.getClass().getSimpleName())
         break
   }
}

def getFilesWithoutProperties(def allFileUris, Map pipelineParams, String apiUrl)
{
   def filesWithoutProperties = []
   // Iterate through all files located at PATH_TO_ARTIFACT_TO_PROMOTE
   allFileUris.each { fileUri ->
      boolean containsAllProperties = true
      // Put file properties in map format for processing
      def properties = utilMethods.curl(credentialsId: pipelineParams.ARTIFACT_CREDENTIALS, 
                                        url: "${apiUrl}${fileUri}?properties", 
                                        returnStdout: true)
      // If the file does not have any properties, add and go to the next.
      if (properties.contains("errors")) {
         filesWithoutProperties.add(fileUri)
         continue
      }
      def result = utilMethods.parseJson(properties).properties
      // Put properties user defined in map format for processing
      Map propertiesToMatch = utilMethods.parseJson(pipelineParams.PATTERN_TO_KEEP)
      // Check to see that ALL properties in PATTERN_TO_KEEP are not present.
      propertiesToMatch.each { key, value ->
         containsAllProperties = (containsAllProperties && result[key] == value)
      }
      // True means they are present, false means they aren't
      // Since we only want to move those that do not have the properties
      // we must return the opposite of the find function.
      if (!containsAllProperties) {
         filesWithoutProperties.add(fileUri)
      }
   }
   return filesWithoutProperties
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the archive artifacts stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postArchiveArtifacts(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}