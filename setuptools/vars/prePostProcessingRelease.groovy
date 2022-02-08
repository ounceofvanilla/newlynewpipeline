/** Here is a list of the methods that are currently available to override:
      preTagSourceCode(Map pipelineParams)
      postTagSourceCode(Map pipelineParams)
      prePromoteArtifact(Map pipelineParams)
      postPromoteArtifact(Map pipelineParams)
      alwaysPost(Map pipelineParams)
      failurePost(Map pipelineParams)
      successPost(Map pipelineParams)
      unstablePost(Map pipelineParams)
      sendNotification(Map pipelineParams)
**/

// Methods in this file will end up as object methods on the object that load returns.

def preTagSourceCode(Map pipelineParams) {
 echo "INFO - No SETUPTOOLS specific preTagSourceCode processing"
}

def postTagSourceCode(Map pipelineParams) {
 echo "INFO - No SETUPTOOLS program specific postTagSourceCode processing defined"
}

def prePromoteArtifact(Map pipelineParams) {
 echo "INFO - No SETUPTOOLS program specific prePromoteArtifact processing defined"
}

def postPromoteArtifact(Map pipelineParams) {
 echo "INFO - No SETUPTOOLS program specific postPromoteArtifact processing defined"
}

def alwaysPost(Map pipelineParams) {
 echo "INFO - No SETUPTOOLS program specific alwaysPost processing defined"
}

def failurePost(Map pipelineParams) {
 echo "INFO - No SETUPTOOLS program specific failurePost processing defined"
}

def successPost(Map pipelineParams) {
 echo "INFO - No SETUPTOOLS program specific successPost processing"
}

def unstablePost(Map pipelineParams) {
 echo "INFO - No SETUPTOOLS program specific unstablePost processing defined"
}

def sendNotification(Map pipelineParams) {
 echo "INFO - Notify program team"
 //The default is to send an email to the submitter of the job and 
 //all email addresses in the EMAIL_RECIPIENTS parameter on the job. 
 //A program can add additional notification processing here and skip the 
 //default processing by deleting the following line, or add program specific
 //notification processing and also send the default email.
 prePostProcessingRelease.sendNotification(pipelineParams)
}
return this