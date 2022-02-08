// Methods in this file will be used at the appropriate time during the build
// Only methods that the program wants to implement need to be added. Un-implemented methods
// will use the default pipeline framework processing

def preBuild(Map pipelineParams) {
   // Sample implementation for the preBuild method
   echo "INFO - No MAKE specific pre Build processing"
}

def sendNotification(Map pipelineParams) {
   // The sendNotification method is being overridden (no notifications will be sent)
   echo "INFO - Skipping sendNotification processing"
}

return this