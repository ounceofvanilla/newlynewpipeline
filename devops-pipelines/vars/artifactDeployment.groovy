 def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preArtifactDeployment(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {
   def artifactUrlPath = artifactManagement.getArtifactUrlPath(pipelineParams)
   // By default, just use the base filename pattern
   def fileList = [pipelineParams.ARTIFACT_DEPLOYMENT_SEARCH_PATTERN]
   if (pipelineParams.ARTIFACT_DEPLOYMENT_SEARCH_REGEX_FLAG == 'true') {
      // In order to support deprecation as much as possible, we need to take the difference of these URLs
      // to construct the API call
      def repoName = pipelineParams.ARTIFACT_URL - pipelineParams.ARTIFACT_MANAGEMENT_TOOL_URL
      def apiUrl = "${pipelineParams.ARTIFACT_MANAGEMENT_TOOL_URL}/api/storage/${repoName}"
      // Get file listing from artifact url
      def response = utilMethods.curl(credentialsId: pipelineParams.ARTIFACT_CREDENTIALS, 
                                      url: "${apiUrl}/${artifactUrlPath}?list&deep=1", 
                                      returnStdout: true)
      def fileUris = utilMethods.getFileUrisFromJsonText(response)
      // Find file URIs that match the pattern
      fileList = fileUris.findAll{ element -> element.matches(pipelineParams.ARTIFACT_DEPLOYMENT_SEARCH_PATTERN) }
   
      if(!fileList) {
         logger.logError("No files were found to deploy at artifact management path ${artifactUrlPath} matching pattern ${pipelineParams.ARTIFACT_DEPLOYMENT_SEARCH_PATTERN}")
      }
   }
      
   def artifactUrl = "${pipelineParams.ARTIFACT_URL}/${artifactUrlPath}"
   fileList.each { filePath ->
      switch(pipelineParams.ARTIFACT_DEPLOYMENT_TOOL) {
         case 'generic':
            utilMethods.curl(credentialsId: pipelineParams.ARTIFACT_CREDENTIALS, 
                             url: "${artifactUrl}/${filePath}",
                             outFile: "${pipelineParams.ARTIFACT_DEST_FOLDER}/${filePath}",
                             returnStdout: true)
            break
         case 'ansible':
            if (isUnix()) {
               def ansibleCommand =  "ansible-playbook './${pipelineParams.ANSIBLE_PLAYBOOK_NAME}.yml'"
               if (pipelineParams.ANSIBLE_VARS_ENCRYPTED == 'true') {
                  ansibleCommand += " --vault-password-file '${pipelineParams.ANSIBLE_PASSFILE_DIRECTORY}/${pipelineParams.ANSIBLE_PASSFILE_NAME}'"
               }
               // Make these variables available in the shell so Ansible can look them up if necessary
               def envVarsList = [
                  PIPELINE_PARAMS_ARTIFACT_URL: "${artifactUrl}",
                  PIPELINE_PARAMS_SOURCE_PATH: filePath,
                  PIPELINE_PARAMS_DEST_FOLDER: pipelineParams.ARTIFACT_DEST_FOLDER
               ].collect { "${it.key}=${it.value}" }
               withEnv(envVarsList) {
                  logger.logInfo("Deploying ${filePath} from ${artifactUrl} to ${pipelineParams.ARTIFACT_DEST_FOLDER}")
                  withCredentials([usernamePassword(credentialsId: "${pipelineParams.ARTIFACT_CREDENTIALS}", passwordVariable: 'ARTIFACT_REPO_PASSWORD', usernameVariable: 'ARTIFACT_REPO_USERNAME')]) {
                     if (pipelineParams.ANSIBLE_REPO_TYPE == 'source') {
                        dir ("${pipelineParams.ANSIBLE_DIRECTORY}") {
                           sh "${ansibleCommand} --extra-vars " + '"artifact_repo_un=${ARTIFACT_REPO_USERNAME} artifact_repo_pw=${ARTIFACT_REPO_PASSWORD}"'
                        }
                     }
                     else {
                        // Use the Bitbucket credentials that the user added to their system.
                        // NOTE: The credentials in Jenkins must have a credentials ID of BITBUCKET_CREDENTIALS for this to work.
                        dir ("$WORKSPACE/ansible_git") {
                           git branch: "${pipelineParams.ANSIBLE_BRANCH}", credentialsId: 'BITBUCKET_CREDENTIALS', url: "${pipelineParams.ANSIBLE_URL}"
                           sh "${ansibleCommand} --extra-vars " + '"artifact_repo_un=${ARTIFACT_REPO_USERNAME} artifact_repo_pw=${ARTIFACT_REPO_PASSWORD}"'
                        }
                     }
                  }
               }
            }
            else {
               logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": Windows")
            }
            break
         default:
            logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.ARTIFACT_DEPLOYMENT_TOOL, this.getClass().getSimpleName())
            break
      }
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the artifact deployment stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postArtifactDeployment(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}