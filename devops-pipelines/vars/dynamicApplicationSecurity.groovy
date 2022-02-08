def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preDAST(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {
   switch(pipelineParams.DYNAMIC_APPLICATION_SECURITY_CHOICE) {
      case 'zap':
         // If a non-Unix machine is executing the stage, notify the user that it is not compatible yet.
         if (!isUnix())
         {
            logger.logError(constants.UNSUPPORTED_ENVIRONMENT_ERROR + ": ZAP, Windows")
         }
         else
         {
            //Perform ZAP scan and generate HTML report
            def configFile = pipelineParams.DYNAMIC_APPLICATION_SECURITY_CONFIG_FILE ? " -configfile \"${pipelineParams.DYNAMIC_APPLICATION_SECURITY_CONFIG_FILE}\"" : ''
            def target = pipelineParams.DYNAMIC_APPLICATION_SECURITY_TARGET ? " -quickurl \"${pipelineParams.DYNAMIC_APPLICATION_SECURITY_TARGET}\"" : ''
            def reportFile = pipelineParams.DYNAMIC_APPLICATION_SECURITY_REPORT_FILE ? " -quickout \"${env.WORKSPACE}/${pipelineParams.DYNAMIC_APPLICATION_SECURITY_REPORT_FILE}\"" : ''
            sh """
               zap -cmd ${configFile} ${target} ${reportFile} ${pipelineParams.DYNAMIC_APPLICATION_SECURITY_ARGS}
            """ 
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.DYNAMIC_APPLICATION_SECURITY_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   // This block will properly display the static code analysis results depending on the tool that was used.
   switch(pipelineParams.DYNAMIC_APPLICATION_SECURITY_CHOICE) {
      case 'zap':
         def zapPublishArgs = [allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: "${env.WORKSPACE}",
                               reportFiles: "${pipelineParams.DYNAMIC_APPLICATION_SECURITY_REPORT_FILE}",
                               reportName: 'Dynamic Application Security Report', reportTitles: 'Dynamic Application Security']
         publishHTML(zapPublishArgs + pipelineParams.DYNAMIC_APPLICATION_SECURITY_PUBLISH_ARGS)
         break
      default:
         logger.logError(constants.ENVIRONMENT_ERROR)
         break
   }
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postDAST(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}