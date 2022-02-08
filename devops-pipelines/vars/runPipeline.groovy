def call(Map pipelineParams) {   

   node('master') {

         // Handle deprecated parameter values for existing pipelines
      pipelineParams.JENKINS_AGENT = pipelineParams.JENKINS_AGENT ?: pipelineParams.JENKINS_SLAVE
      
      def jobAgent = params["OVERRIDE_JENKINS_AGENT"] ?: params["OVERRIDE_JENKINS_SLAVE"]

      if (jobAgent)
      {
         logger.logInfo("Overriding Jenkinsfile parameter of JENKINS_AGENT with job-level parameter value '${jobAgent}'")
         pipelineParams.JENKINS_AGENT = jobAgent
      }

      if( !( checkAgentExistence("${pipelineParams.JENKINS_AGENT}") ) ) 
      { 
         // Neither the Node or Docker Agent exists as named
         logger.logError("""No node named ${pipelineParams.JENKINS_AGENT}. 
                           |Please check the JENKINS_AGENT parameter in your Jenkinsfile 
                           |and configure Jenkins with the appropriate node.""".stripMargin())
      }
   }

   pipeline {
      options {
         buildDiscarder(logRotator(numToKeepStr: "${pipelineParams.NUMBER_BUILDS_TO_KEEP ?: ''}", daysToKeepStr: "${pipelineParams.DAYS_BUILDS_TO_KEEP ?: ''}", artifactNumToKeepStr: "${pipelineParams.NUMBER_ARTIFACTS_TO_KEEP ?: '5'}"))
         timeout(time: "${pipelineParams.TIMEOUT_TIME ?: 9}" as Integer, unit: "${pipelineParams.TIMEOUT_UNITS ?: 'HOURS'}")
         skipDefaultCheckout()
      }

      agent {label "${pipelineParams.JENKINS_AGENT}"}
      stages{          
         stage ('DOCPF') {
            steps {
               script {

                  // Clean workspace according to clean up parameter
                  // Check override of parameter first
                  def cleanup = params["OVERRIDE_PRE_WORKSPACE_CLEANUP_STATUS"]
                  if (cleanup)
                  {
                    logger.logInfo("Overriding Jenkinsfile parameter of PRE_WORKSPACE_CLEANUP_STATUS with job-level parameter value '${cleanup}'")
                    pipelineParams.PRE_WORKSPACE_CLEANUP_STATUS = cleanup
                  }
                  if (pipelineParams.PRE_WORKSPACE_CLEANUP_STATUS == 'true') {
                    step([$class: 'WsCleanup'])
                    echo "Cleaned Workspace Pre-Build"
                  }

                  // Check out from the scm config and capture the environment variables returned
                  checkout(scm).each { key, val ->
                     env.setProperty(key, val)
                  }
                  
                  // IMPORTANT: The call to initPrePostProcessing MUST be the first thing processed after checkout to insure that the 
                  // post block can successfully execute program specific processing in case of an error.
                  echo "Initializing Pre/Post Processing"
                  prePostProcessing.initPrePostProcessing(pipelineParams)
                  
                  // All of the variables defined in the Jenkinsfile are set,
                  // displayed and validated here
                  echo "Setting Up Environment"
                  printContactInfo(pipelineParams)
                  jenkinsEnvironment.setupVariables(pipelineParams)
                  jenkinsEnvironment.printVariables(pipelineParams)
                  
                  echo "Executing Stages"
                  
                  def aStageFailed = false
                  def stageFailedException = null
                  def stageGroups = [:]

                  // STAGE_DATA is built in jenkinsEnvironment
                  jenkinsEnvironment.getStageData().each { stageName, stageData ->
                     def stageGroupKey = stageData.parallelStageGroup

                     if (!stageGroups.containsKey(stageGroupKey))
                     {
                        stageGroups.put(stageGroupKey, [:])
                     }
                     stageGroups[stageGroupKey].put(stageName, 
                        {
                           stage (stageName) {
                              // will execute using the declared node specified by customAgentLabel, otherwise executed on the node defined by the agent block of the pipeline.
                              conditionalNode (stageData.customAgentLabel) {
                                 // Mark subsequent stages as skipped if a stage fails
                                 when (!aStageFailed) {
                                    try {
                                       def stageParams = pipelineParams.findAll { !(it.value instanceof Map) }
                                       stageParams.putAll( pipelineParams.findAll { it.value instanceof Map && it.value[stageName] }.collectEntries { [(it.key): it.value[stageName]] } )
                                       def stageObject = stageData.stageObject
                                       stageObject.doPreStageProcessing(prePostProcessing, stageParams)
                                       if (stageData.enabled) {
                                          stageObject.doMainStageProcessing(stageParams)
                                          stageObject.doMainStageAnalysis(stageParams)
                                       }
                                       stageObject.doPostStageProcessing(prePostProcessing, stageParams)
                                       // Copy any param value changes from stage processing back into pipelineParams
                                       stageParams.each { key, val ->
                                          if (pipelineParams[key] instanceof Map)
                                          {
                                             pipelineParams[key][stageName] = val
                                          }
                                          else
                                          {
                                             pipelineParams[key] = val
                                          }
                                       }
                                    }
                                    catch(e) {
                                       aStageFailed = true
                                       stageFailedException = e
                                    }
                                 }
                              }
                           }
                        }
                     )
                  }
                     
                  stageGroups.each { groupId, stageMap ->
                     if (stageMap.size() > 1) {
                        logger.logInfo("RUNNING PARALLEL GROUP: ${groupId}...")
                        parallel(stageMap)
                        logger.logInfo("FINISHED RUNNING PARALLEL GROUP: ${groupId}...")
                     } 
                     else {
                        parallel(stageMap)
                     }
                  }

                  if (aStageFailed) {
                    throw stageFailedException
                  }
               }
            }
         }
      }
      
      post{
         always{
            script {
               //Check if the pipeline-model-definition plugin is outdated
               if (utilMethods.checkPluginVersion("pipeline-model-definition","1.3.9", pipelineParams) < 0) 
               {
                  logger.logWarning('''
                     ------------ATTENTION---------
                     BUILD STATUS MIGHT NOT BE ACCURATE DUE TO pipeline-model-definition PLUGIN BEING OUTDATED
                     It needs to be updated to a version >= 1.3.9
                  ''')
               } 
               printBuildStatus(pipelineParams)
               printContactInfo(pipelineParams)
               if (pipelineParams.SEND_NOTIFICATION == 'always') {
                  prePostProcessing.sendNotification(pipelineParams)
               }
               prePostProcessing.alwaysPost(pipelineParams)

               if (pipelineParams.POST_WORKSPACE_CLEANUP_STATUS == 'always') {
				      step([$class: 'WsCleanup'])
			      }
 
               if(pipelineParams.JIRA_COMMENT_CRITERIA == 'always'|| pipelineParams.JIRA_COMMENT_CRITERIA == 'never') {
                  jiraAddComment(pipelineParams)
               }
            }
         }
         success{
            script {
               prePostProcessing.successPost(pipelineParams)
               
               if(pipelineParams.JIRA_COMMENT_CRITERIA == 'success' || pipelineParams.JIRA_COMMENT_CRITERIA == 'not-failure') {
                  jiraAddComment(pipelineParams)
               }

               if (pipelineParams.POST_WORKSPACE_CLEANUP_STATUS == 'success' || pipelineParams.POST_WORKSPACE_CLEANUP_STATUS == 'not-failure') {
				      step([$class: 'WsCleanup'])
			      }
            }
         }
         unstable{
            script {
               prePostProcessing.unstablePost(pipelineParams)

               if(pipelineParams.JIRA_COMMENT_CRITERIA == 'unstable' || pipelineParams.JIRA_COMMENT_CRITERIA == 'not-failure') {
                  jiraAddComment(pipelineParams)
               }               

               if (pipelineParams.SEND_NOTIFICATION == 'unstable') {
                  prePostProcessing.sendNotification(pipelineParams)
               }
               
               if (pipelineParams.POST_WORKSPACE_CLEANUP_STATUS == 'unstable' || pipelineParams.POST_WORKSPACE_CLEANUP_STATUS == 'not-failure') {
                  step([$class: 'WsCleanup'])
			      }
            }
         }
         changed{
            script{
               if (pipelineParams.SEND_NOTIFICATION == 'changed') {
                  prePostProcessing.sendNotification(pipelineParams)
               }
            }
         }
         failure{
            script {
               prePostProcessing.failurePost(pipelineParams)
            
               if (pipelineParams.SEND_NOTIFICATION == 'failure') {
                  prePostProcessing.sendNotification(pipelineParams)
               }
            }
         }
      }
   }
}

//Creates a comment with a link to the Jenkins build inside the Jira comment section, if directed to do so by JIRA_COMMENT_CRITERIA in the Jenkinsfile.
def jiraAddComment(Map pipelineParams) {
   if (pipelineParams.JIRA_COMMENT_CRITERIA != 'never') {

      def pattern = ~/^(.*\/)?([A-Z]+-[0-9]+).*$/
      def ticketKey
      try {
         ticketKey = pattern.matcher(pipelineParams.ALTERNATE_BRANCH_REPO)[0][2]
      }
      catch(e) {
         logger.logWarning("The specified branch '${pipelineParams.ALTERNATE_BRANCH_REPO}' did not match a Jira ticket key pattern. No Jira comment will be created.")
         return
      }

      def jiraSelectorSelection = jiraIssueSelector(issueSelector: [$class: 'JqlIssueSelector', jql: "issueKey = ${ticketKey}"])
      if (!jiraSelectorSelection) {
         logger.logWarning("No ticket matching ticket key '${ticketKey}' could be found. No Jira comment will be created.")
         return
      }

      def jiraSelectionFinal = jiraSelectorSelection.toString()[1..-2]
      jiraComment(issueKey: jiraSelectionFinal, body: """Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) built. 
      Build status was ${currentBuild.currentResult}. 
      Please go to ${env.BUILD_URL}.""")

      logger.logInfo("JIRA_COMMENT_CRITERIA set to ${pipelineParams.JIRA_COMMENT_CRITERIA}. Jira comment was added")
   } else {
      logger.logInfo('JIRA_COMMENT_CRITERIA set to NEVER.  No Jira comment was added')
   }
}

/* Print info on who to contact for support with DOCPF */
def printContactInfo(Map pipelineParams) {
   //Display contact information for DOCPF Team
      logger.logInfo('''
-------------------------------------------------------
      For issues, contact the DOCPF Development Team
      DOCPF_Leads@L3Harris.com

      For more information about the DOCPF, visit
      https://confluenceopen01.gs.myharris.net/display/DOCPF
-------------------------------------------------------
      ''')
}


/* Print the status of the build */
def printBuildStatus(Map pipelineParams) {
   //Display build status
   if (currentBuild.currentResult == 'SUCCESS') {
   logger.logInfo('''
   DOCPF build completed successfully!
                        
          _             
         /(|            
        (  :            
       __\\  \\  _____  
     (____)  `|         
    (____)|   |         
     (____).__|         
      (___)__.|_____    
   ''')
   }
   else {
   logger.logInfo('''
   DOCPF Build failed or unstable!
                         
       ,-----.           
     ,'       `.         
    :   Build   :        
    :   FAILED/ :        
    '. UNSTABLE,'        
      `._____,'          
          ||             
        _,''--.    _____ 
       (/ __   `._|      
      ((_/_)\\     |     
       (____)`.___|      
        (___)____.|_____ 
          ||             
        _\\||/_          
   ''')
   } 
}