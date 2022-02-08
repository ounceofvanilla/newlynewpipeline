def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preTagSourceCode(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) { 

   switch(pipelineParams.SOURCE_CODE_TOOL) {
      case 'bitbucket':
         //git commands to tag the current commit with tag provided in Jenkinsfile and push to the repo.
         withCredentials([usernamePassword(credentialsId: "${pipelineParams.BITBUCKET_CREDENTIALS}", usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASSWORD')]) 
         {
            def repoUrl = GIT_URL - "https://"
            repoUrl = "${repoUrl}".substring("${repoUrl}".lastIndexOf('@') + 1)

            env.URL_ENCODED_GIT_PASSWORD=URLEncoder.encode(GIT_PASSWORD, "UTF-8")

            def osCommand;
            if (isUnix()) {
               sh ("""
                  set +x
                  git -c user.name=""" + '${GIT_USER}' + """ -c user.email=Do-Not-Reply@L3Harris.com tag -a ${pipelineParams.RELEASE_TAG} ${GIT_COMMIT} -m "Tag ${pipelineParams.RELEASE_TAG} Created"
                  git -c user.name=""" + '${GIT_USER}' + """ -c user.email=Do-Not-Reply@L3Harris.com push https://""" + '${GIT_USER}:${URL_ENCODED_GIT_PASSWORD}' + """@${repoUrl} --tags
                  """)

                  echo """
                  git -c user.name=${GIT_USER} -c user.email=Do-Not-Reply@L3Harris.com tag -a ${pipelineParams.RELEASE_TAG} ${GIT_COMMIT} -m "Tag ${pipelineParams.RELEASE_TAG} Created"
                  git -c user.name=${GIT_USER} -c user.email=Do-Not-Reply@L3Harris.com push https://""" + '${GIT_USER}:*****' + """@${repoUrl} --tags
                  """

            } else {
               bat ("""
                  @echo off
                  git -c user.name=""" + '%GIT_USER%' + """ -c user.email=Do-Not-Reply@L3Harris.com tag -a ${pipelineParams.RELEASE_TAG} ${GIT_COMMIT} -m "Tag ${pipelineParams.RELEASE_TAG} Created"
                  git -c user.name=""" + '%GIT_USER%' + """ -c user.email=Do-Not-Reply@L3Harris.com push https://""" + '%GIT_USER%:%URL_ENCODED_GIT_PASSWORD%' + """@${repoUrl} --tags
                  """)

                  echo"""
                  git -c user.name=%GIT_USER% -c user.email=Do-Not-Reply@L3Harris.com tag -a ${pipelineParams.RELEASE_TAG} ${GIT_COMMIT} -m "Tag ${pipelineParams.RELEASE_TAG} Created"
                  git -c user.name=%GIT_USER% -c user.email=Do-Not-Reply@L3Harris.com push https://""" + '%GIT_USER%:*****' + """@${repoUrl} --tags
                  """
            }
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.SOURCE_CODE_TOOL, this.getClass().getSimpleName())
         break
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the tag source code stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postTagSourceCode(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}