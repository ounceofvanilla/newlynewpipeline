def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preDockerImageBuild(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {
   checkout changelog:false, poll:false, 
   scm: [$class: 'GitSCM', branches: [[name:"${pipelineParams.DOCKER_REPO_BRANCH}"]], 
         doGenerateSubmoduleConfigurations:false, extensions:[], submoduleCfg:[], userRemoteConfigs: 
         [[credentialsId:"${pipelineParams.BITBUCKET_CREDENTIALS}", url:"${pipelineParams.DOCKER_REPO_URL}"]]
   ]
   docker.withRegistry("${pipelineParams.REGISTRY_URL}") {
      def TAG = "${pipelineParams.DOCKER_REPO_BRANCH}".substring("${pipelineParams.DOCKER_REPO_BRANCH}".lastIndexOf('/') + 1)
      if (fileExists("${pipelineParams.SOURCE_CODE_DIRECTORY}/image")) {
         sh "echo ${pipelineParams.DOCKER_IMAGE}:${TAG} > ${pipelineParams.SOURCE_CODE_DIRECTORY}/image/DOCPF_DOCKER_VERSION"
      }
      pipelineParams.DockerImage = docker.build("${pipelineParams.DOCKER_IMAGE}", "${pipelineParams.SOURCE_CODE_DIRECTORY}")
   } 
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the docker image build stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postDockerImageBuild(pipelineParams)
}