def postArtifactManagement(Map pipelineParams) {
   echo "INFO - Using custom DevOps Factory Docker postArtifactManagement Processing"
   
   script {
      docker.withRegistry("${pipelineParams.REGISTRY_URL}") {
         def TAG = "${GIT_BRANCH}".substring("${GIT_BRANCH}".lastIndexOf('/') + 1)
         sh "echo ${pipelineParams.DOCKER_IMAGE}:${TAG} > ${pipelineParams.DOCKER_IMAGE}/docker/image/DOCPF_DOCKER_VERSION"
         def currentImage = docker.build("${pipelineParams.DOCKER_IMAGE}", "${pipelineParams.SOURCE_CODE_DIRECTORY}")
         currentImage.push("${TAG}")

         PUSH_LATEST = "${params.TAG_LATEST?.toLowerCase() ?: 'false'}"

         if (PUSH_LATEST == 'true') {
            currentImage.push('latest')
         }
      }
   }
}
return this