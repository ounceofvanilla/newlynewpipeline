def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preBuild(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) { 
   dir("${pipelineParams.PROJECT_ROOT}") {
      // Check if prebuild stage was selected
      if (pipelineParams.RUN_PREBUILD == 'true') {
         preBuildSourceCode(pipelineParams)
      }

      // Executing different blocks of code depending on the build tool being used.
      switch(pipelineParams.BUILD_TOOL) {
         case 'gradle':
            def gradle_build_command = "gradle build"
            if (isUnix()) {
               sh ". /etc/profile.d/gradle.sh \
                  && gradle -v \
                  && ${gradle_build_command}"
            }
            else {
               bat "gradle -v \
                  && ${gradle_build_command}"
            }
            break
         case 'msbuild':
            // If a Unix machine is executing the code, notify the user that it is not compatible.
            if (isUnix()) {
               logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": MSBuild, Linux")
            }

            // Don't run StyleCop by default
            def runStyleCop = 'false'

            // Get vcvars batch
            def runVcvarsBatch = pipelineParams.VCVARS_BATCH_FILE ? "call \"${pipelineParams.VCVARS_BATCH_FILE}\" ${pipelineParams.VCVARS_ARCH} ${pipelineParams.VCVARS_PLATFORM} ${pipelineParams.VCVARS_WINSDK} ${pipelineParams.VCVARS_VCVER}" : ""

            // Check if style checking was selected and if the tool choice is StyleCop
            if (jenkinsEnvironment.getStageData('STYLE_CHECK')?.enabled && pipelineParams.STYLE_CHECK_CHOICE == 'stylecop') 
            {
               runStyleCop = 'true'
               // do a restore of the project and get StyleCop package
               bat """
                  ${runVcvarsBatch}
                  msbuild ${pipelineParams.SOLUTION_FILE} /p:Configuration=${pipelineParams.BUILD_CONFIGURATION} /p:Platform=\"${pipelineParams.BUILD_PLATFORM}\" -t:restore /p:RunStyleCop=${runStyleCop}
               """
            }
            
            // Use MSBuild.exe to build the source code. The binaries must also be output to the workspace so the unit tests can be run with the dependencies colocated.
            def outputPath = pipelineParams.OUTPUT_PATH ? "/p:OutputPath=\"${pipelineParams.OUTPUT_PATH}\"" : ''
            bat """
                  ${runVcvarsBatch}
                  MSBuild.exe ${pipelineParams.SOLUTION_FILE} /p:Configuration=${pipelineParams.BUILD_CONFIGURATION} /p:Platform=\"${pipelineParams.BUILD_PLATFORM}\" ${outputPath} /p:ProductVersion=${pipelineParams.VERSION_NUMBER}.${pipelineParams.BUILD_NUMBER} /p:RunStyleCop=${runStyleCop}
            """
            break
         case 'maven':
            // Use the same command to build the source code but execute with shell or Batch depending on the environment.
            def artifact_version = artifactManagement.getVersionNumber(pipelineParams)
            def maven_version_command = "mvn versions:set -DnewVersion=${artifact_version} -gs settings.xml"
            def maven_build_command = "mvn clean install -Dmaven.test.skip=true -gs settings.xml"
            withCredentials([usernamePassword(credentialsId: "${pipelineParams.ARTIFACT_CREDENTIALS}", passwordVariable: 'ARTIFACT_REPO_PASSWORD', usernameVariable: 'ARTIFACT_REPO_USERNAME')]) {
               if (isUnix()) {
                  withEnv (["LINE_COVERAGE_TARGETS=${pipelineParams.LINE_COVERAGE_TARGETS}"]) {
                     sh "${maven_version_command}"
                     sh "${maven_build_command}"
                  }
               }
               else {
                  withEnv (["LINE_COVERAGE_TARGETS=${pipelineParams.LINE_COVERAGE_TARGETS}"]) {
                     bat "${maven_version_command}"
                     bat "${maven_build_command}"
                  }
               }
            }
            break
         case 'make':
            // If a non-Unix machine is executing the code, notify the user that it is not compatible.
            if (!isUnix()) {
               logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": Make, Windows")
            }
            
            // Set additional parameters needed to generate code coverage files
            def envVarsMap = [:]
            if (jenkinsEnvironment.getStageData('CODE_COVERAGE')?.enabled) {
               envVarsMap['JENKINS_CXXFLAGS'] = '--coverage'
               envVarsMap['JENKINS_CFLAGS'] = '--coverage'
               if (pipelineParams.PREBUILD_TOOL == 'qmake') {
                  envVarsMap['JENKINS_LIBS'] = '-lgcov'
               }
            }
            withEnv (envVarsMap.collect { "${it.key}=${it.value}" }) {
               dir(pipelineParams.PREBUILD_DIRECTORY) {
                  sh "make -f '${pipelineParams.MAKEFILE_NAME}' '${pipelineParams.MAKEFILE_TARGET}'"
               }
            }
            break
         case 'vxworks':
            // If a non-Unix machine is executing the code, notify the user that it is not compatible.
            if (isUnix()) {
               logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": VxWorks, Linux")
            }
            // Use wrenv to build the source code.
         bat """
            "${pipelineParams.WIND_HOME}\\wrenv" -p "${pipelineParams.VXWORKS_PACKAGE}" make "${pipelineParams.MAKEFILE_TARGET}" -f "${pipelineParams.MAKEFILE_NAME}" ${pipelineParams.VXWORKS_ADDITIONAL_PARMS}
         """
            break
         case 'setuptools':
            // Use the same commands to build the source code but execute with shell or Batch depending on the environment.
            def python_build_command = "-m setup build"
            def python_install_command = "-m pip install --user ."
            if (isUnix()) {
               sh """
                  python3 ${python_build_command}
                  python3 ${python_install_command}
               """
            }
            else {
               bat """
                   python ${python_build_command}
                   python ${python_install_command}
               """
            }
            break
         default:
            logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.BUILD_TOOL, this.getClass().getSimpleName())
            break
      }
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the build stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postBuild(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}