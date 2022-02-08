/*
This script implements a custom node conditional to set the node to a user 
defined node for one or more stages, which is determined in the 
pipeline params in the jenkinsfile. 
*/

def call(String customAgentLabel, Closure body) {
   def nodeLabelsToCheck = env.NODE_LABELS.split(" ")

   if (!customAgentLabel || nodeLabelsToCheck.contains('${customAgentLabel}') || customAgentLabel == env.NODE_NAME) {
       // executes body on the node defined by the agent block of the pipeline
      body()
   } else {
      //executes body on the customAgentLabel node
      node (customAgentLabel) {
         body()
      }    
   }
}