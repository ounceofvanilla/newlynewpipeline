def call(Map pipelineParams) {
    
    switch(pipelineParams.PREBUILD_TOOL) {

        case 'cmake':

            // If a Windows machine is executing the code, notify the user that it is not compatible.
            if (!isUnix()) {
                logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": cmake, Windows")
            }
            
            // Run CMake with set compiler flags when using code coverage
            String coverageOption = "${jenkinsEnvironment.getStageData('CODE_COVERAGE')?.enabled ? '--coverage' : ''}"
            withEnv (["JENKINS_CXXFLAGS=${coverageOption}", "JENKINS_CFLAGS=${coverageOption}"]) {
                dir(pipelineParams.PREBUILD_DIRECTORY) {
                    sh "cmake '${pipelineParams.CMAKE_LISTS_PATH}'"
                }
            }
            break

        case 'nuget':
            
            // If a Unix machine is executing the code, notify the user that it is not compatible.
            if (isUnix()) {
                logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": Nuget, Linux")
            }
            
            // If the type is a string, convert it to be a map so that it works with the loops below.
            if (pipelineParams.ARTIFACTORY_DEPENDENCY_URL instanceof String) 
            { 
                pipelineParams.ARTIFACTORY_DEPENDENCY_URL = ["${pipelineParams.NUGET_SOURCE_NAME}":"${pipelineParams.ARTIFACTORY_DEPENDENCY_URL}"]
            }

            // Remove the sources if they already existed before adding any. This ensures that we are adding the correct sources under the correct names.
            // If the sources do not exist, it will throw an error when trying to remove them which is why we ignore errors.
            pipelineParams.ARTIFACTORY_DEPENDENCY_URL.each { name, url ->
                bat """
                    nuget sources remove -Name "${name}" -Source "${url}" || exit 0
                """
            }
            
            // Add the sources that will be used to restore the solution file.
            withCredentials([usernamePassword(credentialsId: "${pipelineParams.ARTIFACT_CREDENTIALS}", passwordVariable: 'ARTIFACT_REPO_PASSWORD', usernameVariable:'ARTIFACT_REPO_USERNAME')]) {
                pipelineParams.ARTIFACTORY_DEPENDENCY_URL.each { name, url ->
                    bat """
                        nuget.exe sources add -Name "${name}" -Source "${url}" -UserName %ARTIFACT_REPO_USERNAME% -Password %ARTIFACT_REPO_PASSWORD%
                    """
                }
            } 
	    	
            // Use Nuget to restore the dependencies before the build begins.
            bat """
                nuget.exe restore "${pipelineParams.SOLUTION_FILE}"
            """

            // Remove the Nuget sources that were just added. This ensures a clean run next time.
            pipelineParams.ARTIFACTORY_DEPENDENCY_URL.each { name, url ->
                bat """
                    nuget sources remove -Name "${name}" -Source "${url}" || exit 0
                """
            }
            break
            
        case 'qmake':
            // If a Windows machine is executing the code, notify the user that it is not compatible.
            if (!isUnix()) {
                logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": qmake, Windows")
            }
            
            dir(pipelineParams.PREBUILD_DIRECTORY) {
                sh "qmake -o '${pipelineParams.MAKEFILE_NAME}' '${pipelineParams.QMAKE_FILE}'"
            }

            break

        default:
	    logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.PREBUILD_TOOL, this.getClass().getSimpleName())
            break
    }
}