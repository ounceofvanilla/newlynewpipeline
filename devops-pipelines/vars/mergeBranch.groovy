def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preMergeBranch(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) { 
   dir("${pipelineParams.PROJECT_ROOT}") {

      switch(pipelineParams.SOURCE_CODE_TOOL) {
         case 'bitbucket':
            //git commands to merge MERGE_FROM_BRANCH into MERGE_TO_BRANCH branch and push changes to the repo.
            withCredentials([usernamePassword(credentialsId: 'BITBUCKET_CREDENTIALS', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASSWORD')]) 
            {
               def repoUrl = GIT_URL - "https://"

               def osCommand;
               if (isUnix()) {
                  osCommand = "sh"
               } else {
                  osCommand = "bat"
               }

               "${osCommand}" ("""
                  git -c user.name=${GIT_USER} -c user.email=Do-Not-Reply@L3Harris.com checkout ${MERGE_TO_BRANCH}
                  git -c user.name=${GIT_USER} -c user.email=Do-Not-Reply@L3Harris.com pull --no-ff https://${GIT_USER}:${GIT_PASSWORD}@${repoUrl} ${MERGE_FROM_BRANCH}
                  git -c user.name=${GIT_USER} -c user.email=Do-Not-Reply@L3Harris.com push https://${GIT_USER}:${GIT_PASSWORD}@${repoUrl} ${MERGE_TO_BRANCH}
                  """)
            }
            break
         default:
	    logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.SOURCE_CODE_TOOL, this.getClass().getSimpleName())
            break
      }
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the mergeBranch stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postMergeBranch(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}