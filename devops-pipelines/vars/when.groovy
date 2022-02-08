import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException

/*
This script implements a when directive that can be used in 
imperative pipelines

Source: https://github.com/comquent/imperative-when/blob/master/vars/when.groovy
*/

def call(boolean condition, Closure body) {
   def config = [:]
   body.resolveStrategy = Closure.OWNER_FIRST
   body.delegate = config

   if (condition) {
      body()
   } else {
      try {
         logger.logWarning("Skipping stage due to previous failures")
         Utils.markStageSkippedForConditional(STAGE_NAME)
      }
      catch(RejectedAccessException e) {
         // In older versions of Jenkins markStageSkippedForConditional is not whitelisted
         return
      }
   }
}