def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preSAST(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {
   switch(pipelineParams.STATIC_APPLICATION_SECURITY_CHOICE) {
      case 'fortify':
         logger.logWarning("Use of Fortify requires that you have separately installed the tool in your build environment; it is a commercial package that is not currently installed on DOCPF containers or VLEs")
         // Rebuilds the source code with Fortify SCA with appropriate
         // considerations for the current BUILD_TOOL
         if(pipelineParams.BUILD_TOOL == "make") {
            logger.logError(constants.UNIMPLEMENTED_BUILD_TOOL_ERROR + ": Fortify, Make")
         }

         if(pipelineParams.BUILD_TOOL == "maven") {
            logger.logError(constants.UNIMPLEMENTED_BUILD_TOOL_ERROR + ": Fortify, Maven")
         }

         if(pipelineParams.BUILD_TOOL == "msbuild") {
            if (isUnix()) {
               logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": MSBuild, Linux")
            }
            else {
               bat """
                  sourceanalyzer -b SAST-${pipelineParams.BUILD_NUMBER} msbuild /t:Rebuild "${pipelineParams.SOLUTION_FILE}" /p:Configuration=${pipelineParams.BUILD_CONFIGURATION} /p:Platform="${pipelineParams.BUILD_PLATFORM}"\
               """
            }
         }

         if(pipelineParams.BUILD_TOOL == "setuptools") {
            logger.logError(constants.UNIMPLEMENTED_BUILD_TOOL_ERROR + ": Fortify, Setuptools")
         }

         if(pipelineParams.BUILD_TOOL == "vxworks") {
            logger.logError(constants.UNIMPLEMENTED_BUILD_TOOL_ERROR + ": Fortify, VxWorks")
         }

         // Analyzes the built source code and outputs into a text-formatted .fpr file
         def fortify_scan_command = """sourceanalyzer -b SAST-${pipelineParams.BUILD_NUMBER} -scan -f "${pipelineParams.STATIC_APPLICATION_SECURITY_RESULTS_FILE}" -format text"""
         if (isUnix()) {
            sh "${fortify_scan_command}"
         }
         else {
            
            bat "${fortify_scan_command}"
         }
         break
      case 'coverity':
         if (['maven', 'gradle', 'setuptools', 'vxworks'].contains(pipelineParams.BUILD_TOOL)) {
            logger.logError(constants.UNIMPLEMENTED_BUILD_TOOL_ERROR + ": Coverity, ${pipelineParams.BUILD_TOOL}")
         }
         if (isUnix()) {
            def covBinPath = pipelineParams.COVERITY_BIN ? "${pipelineParams.COVERITY_BIN}/" : ""
            if(pipelineParams.BUILD_TOOL == "make") {
               sh """
                   make -f '${pipelineParams.MAKEFILE_NAME}' clean
                   "${covBinPath}cov-configure" --compiler "\$(which '${pipelineParams.COVERITY_MAKE_COMPILER_TYPE}')" --comptype "${pipelineParams.COVERITY_MAKE_COMPILER_TYPE}" --config ./coverity-config.xml 
                   "${covBinPath}cov-build" --config ./coverity-config.xml --dir CoveritySAST make -f '${pipelineParams.MAKEFILE_NAME}' '${pipelineParams.MAKEFILE_TARGET}'
                   "${covBinPath}cov-analyze" --dir CoveritySAST | tee "${pipelineParams.STATIC_APPLICATION_SECURITY_RESULTS_FILE}"
                  """
            }
         }
         else {
             def covBinPath = pipelineParams.COVERITY_BIN ? "${pipelineParams.COVERITY_BIN}\\" : ""
             bat """
                "${covBinPath}cov-build" --dir CoveritySAST MSBuild.exe "${pipelineParams.SOLUTION_FILE}" /t:Rebuild /p:Configuration=${pipelineParams.BUILD_CONFIGURATION} /p:Platform=\"${pipelineParams.BUILD_PLATFORM}\"
                "${covBinPath}cov-analyze" --dir CoveritySAST > "${pipelineParams.STATIC_APPLICATION_SECURITY_RESULTS_FILE}"
                type "${pipelineParams.STATIC_APPLICATION_SECURITY_RESULTS_FILE}"
             """
         }
         break
      case 'klocwork':
         // Notify the user that this case is only compatible with Windows.
         if (isUnix()) {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": Klocwork, Linux")
         }
         // Run SAST analysis if not run in SCA stage
         if (pipelineParams.STATIC_CODE_ANALYSIS_CHOICE != 'klocwork') {
            // Run SCA Klocwork code with SAST parameters
            SASTParams = pipelineParams.clone()
            SASTParams.STATIC_CODE_ANALYSIS_CHOICE = 'klocwork'
            staticCodeAnalysis.doMainStageProcessing(SASTParams)
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.STATIC_APPLICATION_SECURITY_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   // This block will properly display the static code analysis results depending on the tool that was used.
   switch(pipelineParams.STATIC_APPLICATION_SECURITY_CHOICE) {
      case 'fortify':
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.STATIC_APPLICATION_SECURITY_RESULTS_FILE}"
         break
      case 'coverity':
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.STATIC_APPLICATION_SECURITY_RESULTS_FILE}"
      case 'klocwork':
         if (pipelineParams.STATIC_CODE_ANALYSIS_CHOICE == 'klocwork') {
            echo "Klocwork analysis performed in SCA, skipping Klocwork analysis in SAST"
         }
         else {
            // Run SCA Klocwork code with SAST parameters
            SASTParams = pipelineParams.clone()
            SASTParams.STATIC_CODE_ANALYSIS_CHOICE = 'klocwork'
            SASTParams.SCA_PUBLISH_ARGS = SASTParams.SAST_PUBLISH_ARGS
            SASTParams.SCA_NEW_FAILURE_THRESHOLD = SASTParams.SAST_NEW_FAILURE_THRESHOLD
            SASTParams.SCA_FAILURE_THRESHOLD = SASTParams.SAST_FAILURE_THRESHOLD
            SASTParams.SCA_NEW_UNSTABLE_THRESHOLD = SASTParams.SAST_NEW_UNSTABLE_THRESHOLD
            SASTParams.SCA_UNSTABLE_THRESHOLD = SASTParams.SAST_UNSTABLE_THRESHOLD
            staticCodeAnalysis.doMainStageAnalysis(SASTParams)
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.STATIC_APPLICATION_SECURITY_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postSAST(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}