// Checks to see if the parameter is set to a valid value if necessary and formats it if required
// ----
// Map pipelineParams       : Map of pipeline's parameters
// String paramInQuestion   : Name of parameter currently being evaluated
// convertToLowerCase       : Whether or not to reformat the param value to lower case
// String relevantStageKey  : Stage key for stage in which paramInQuestion is relevant
// validValues              : The list of acceptable values for this parameter
// dependentParamValues     : Only validate if any pipeline param defined by this map's key is one of the values in the list for that key
def call(Map pipelineParams, String paramInQuestion, String defaultValue = '', boolean convertToLowerCase = false, String relevantStageKey = '', List validValues = [], Map dependentParamValues = [DOCPF_DUMMY_KEY: []]) {
   def paramValue = pipelineParams[paramInQuestion]
   // Force
   def paramValueMap = paramValue instanceof Map ? paramValue : [DUMMY_PARAM_STAGE_KEY: paramValue]
   
   paramValueMap.each { key, value ->
      // Handle setting default if null
      value = value ?: defaultValue
      // Handle converting to lower case
      value = convertToLowerCase ? value.toLowerCase() : value
      
      // Check value of relevantStageKey (if necessary) to see if particular stage is even run
      if(!relevantStageKey || jenkinsEnvironment.getStageData(relevantStageKey)?.enabled) {
         // Check dependent params if necessary (DOCPF_DUMMY_KEY allows this to iterate at least once without the dependency error message)
         dependentParamValues.each { paramKey, valueList ->
            def dependentParamValue = pipelineParams[paramKey]
            if (dependentParamValue instanceof Map) {
               dependentParamValue = dependentParamValue[key]
            }
            if (paramKey == "DOCPF_DUMMY_KEY" || valueList.contains(dependentParamValue)) {
               // If a list of valid values were provided, make sure the param is one of them
               if (validValues && !validValues.contains(value)) {
                  logger.logError("${paramInQuestion} is ${value}, but must be one of the following: ${validValues}")
               }
               // If the defaultValue is 'INVALID', that means it must be set to some value
               if (value == 'INVALID') {
                  def dependencyErrorPart = "${relevantStageKey} being 'true'"
                  if (paramKey != "DOCPF_DUMMY_KEY") {
                     dependencyErrorPart = "${paramKey} being ${dependentParamValue}"
                  }
                  logger.logError("Must define ${paramInQuestion} due to ${dependencyErrorPart}")
               }
            }
         }
      }
      
      // Validation passed, set value back into the actual params in case it got defaulted or lowercased
      if (paramValue instanceof Map) {
         paramValue[key] = value
      }
      else {
         pipelineParams[paramInQuestion] = value
      }
   }
}