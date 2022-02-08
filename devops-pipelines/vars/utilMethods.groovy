/** This file contains utility methods that can be used in multiple other files. **/

enum ParamRequiredType {
   REQUIRED,
   OPTIONAL
   
   // Need public constructor or enum will be blocked by security sandbox
   public ParamRequiredType() { }
}


/* Use this method to verify named parameters for methods that use them.
   It will aggregate all validation failure messages together so they can
   all be fixed without iteratively failing. */
def validateMethodNamedParams(String callingMethodName, Map paramsToValidate, Map validParamNames) {
   def badParamsMessage
   
   // First check for params that aren't in the valid names
   def badParams = paramsToValidate.findAll { !validParamNames.containsKey(it.key) }
   if (badParams) {
      badParamsMessage = "The following parameter(s) were not recognized for method ${callingMethodName}: "
      badParams.each {
         badParamsMessage = """${badParamsMessage}
                              |   ${it.key}""".stripMargin()
      }
   }
   
   // Now check for missing required params
   def requiredParams = validParamNames.findAll { it.value == ParamRequiredType.REQUIRED }
   def missingParamsMessage
   requiredParams.each {
      if (!requiredParams.containsKey(it.key)) {
         missingParamsMessage = missingParamsMessage ?: "The following parameter(s) were required for method ${callingMethodName} but were missing: "
         missingParamsMessage = """${missingParamsMessage}
                              |   ${it.key}""".stripMargin()
      }
   }
   
   if (badParamsMessage || missingParamsMessage) {
      logger.logError("""${badParamsMessage}
                        |${missingParamsMessage}""".stripMargin())
   }
}


/* Do a commandline curl call with named parameters */
def curl(Map params) {
   def validNamedParams = [
      url: ParamRequiredType.REQUIRED,
      secureSSL: ParamRequiredType.OPTIONAL,
      credentialsId: ParamRequiredType.REQUIRED,
      outFile: ParamRequiredType.OPTIONAL,
      uploadFile: ParamRequiredType.OPTIONAL,
      requestMethod: ParamRequiredType.OPTIONAL,
      headerList: ParamRequiredType.OPTIONAL,
      globbingEnabled: ParamRequiredType.OPTIONAL,
      returnStatus: ParamRequiredType.OPTIONAL,
      returnStdout: ParamRequiredType.OPTIONAL,
      additionalParams: ParamRequiredType.OPTIONAL,
      errorPatternList: ParamRequiredType.OPTIONAL
   ]
   validateMethodNamedParams('curl', params, validNamedParams)
   
   // Set base curl command to fail silently so errors can more reliably be caught
   def curlCommand = 'curl --fail'
   // Explicitly checking for true here means it assumes false by default
   curlCommand += params.secureSSL == true ? '' : ' -k'
   curlCommand += params.outFile ? " -o \"${params.outFile}\"" : ''
   curlCommand += params.uploadFile ? " -T \"${params.uploadFile}\"" : ''
   // GET, POST, PUT, DELETE, etc.
   curlCommand += params.requestMethod ? " -X ${params.requestMethod}" : ''
   // Explicitly checking for false here means it assumes true by default
   curlCommand += params.globbingEnabled == false ? ' --globoff' : ''
   params.headerList.each { header ->
      curlCommand += " --header \"${header.trim()}\""
   }
   
   // Encode each part of the URL path, ignoring separators and API query symbols (=&?)
   def encodedUrl = params.url.replaceAll(/(?<!:\/)([\/=&?;%\[\]'"])([^\/=&?;%\[\]'"]+)/) { 
      "${it[1]}${java.net.URLEncoder.encode(it[2], 'UTF-8')}"
   }
   
   // Capture the command output, forcing status to be printed for error handling
   def cmdOutput
   def statusString = 'curl STATUS CODE WAS:'
   

   withCredentials([usernamePassword(credentialsId: "${params.credentialsId}", usernameVariable: 'CURL_USER', passwordVariable: 'CURL_PW')]) {
      if (isUnix())
      {
         // If curl fails, the echo would not print if it were on the next line; '&&' only executes if curl succeeds; '||' executes if it fails
         cmdOutput = sh script: "${curlCommand} -u " + '"${CURL_USER}:${CURL_PW}" ' + 
                     "${params.additionalParams ?: ''} \"${encodedUrl}\" && echo ${statusString} \$? || echo ${statusString} \$?", 
                     returnStdout: true
      }
      else
      {
         // Need to turn off echoed commands for Windows in case of returned output parsing
         cmdOutput = bat script: """
                            @echo off
                            ${curlCommand} -u """ + '%CURL_USER%:%CURL_PW%' + """ ${params.additionalParams ?: ''} "${encodedUrl}"
                            echo ${statusString} %errorlevel%
                         """, 
                         returnStdout: true, strictMode: true
      }
   }
   
   def outputList = cmdOutput.trim().split(/ *${statusString} +/)
   // Check for incomplete command output
   if (outputList.size() < 2) 
   {
      logger.logError("The curl command did not return the command status; this likely means there was a failure")
   }
   
   // If returnStatus was true, there's nothing left to do. Just return it
   if (params.returnStatus)
   {
      return outputList[-1]
   }
   
   // Check for nonzero exit code
   if (!(outputList[-1] ==~ /^\s*0\s*$/))
   {
      logger.logError("The curl command returned a failing exit code: ${outputList[-1]}")
   }
   
   /* It can be difficult to determine if there has been a business logic error returned
      from a site with curl. The status code that comes back in the header (e.g. 200 OK,
      404 Not Found, etc.) is technically reserved for IP application layer status, 
      which means something happened while attempting to transfer data to or from the
      server. Business logic errors may correctly respond with a 200 OK, meaning that we
      may need to search the response text for specific error messages. */
   def errorsFound = false
   params.errorPatternList?.each { errorTextPattern ->
      def matcher = (outputList[0] =~ errorTextPattern)
      matcher.each { errorText ->
         errorsFound = true
         logger.logWarning("There was an error found in the curl response: ${errorText}")
      }
   }
   
   if (errorsFound)
   {
      if (params.returnStatus)
      {
         // Return a generic nonzero status
         return -1
      }
      else
      {
         logger.logError("Error strings were detected in the curl response.")
      }
   }
   
   // Return the output if required
   if (params.returnStdout)
   {
      return outputList[0]
   }
}


/* Do a basic commandline curl call with positional parameters */
def curl(String credentialsId, String url) {
   curl(credentialsId: credentialsId, url: url)
}


/* Return a parsed JSON object from provided text */
@NonCPS
def parseJson(String text)
{
   def jsonSlurper = new groovy.json.JsonSlurper()
   return jsonSlurper.parseText(text)
}


def getFileUrisFromJsonText(String jsonText)
{
   def result = parseJson(jsonText)
   def files = []
   def resultFileList = result.files ?: result.children
   resultFileList.each { file ->
      files.add(file.uri)
   }
   return files
}


/* Do a toolCommand with conditinal usage of PRJ */
def invokeFpgaTool(Map pipelineParams, String toolCommand) {
   if (pipelineParams.USE_PRJ_TOOL == 'true') {
      sh """#!/bin/tcsh
            set echo
            change_prj '${pipelineParams.PRJ_FILE}'
            ${toolCommand}
      """
   } else {
      sh "${toolCommand}"
   }
}


/* Check for existence of credentials with specified ID */
def checkCredentialsIdExists(String credentialsId) {
   script 
   {
      try {
         withCredentials([usernameColonPassword(credentialsId: "${credentialsId}", variable: 'NO_CREDENTIALS')]) { return true }
      }
      catch (Exception e)
      {
         return false
      }
   }
}


/* Receives plugin shortName and version as parameter and verifies if the current version of that plugin is outdated.
   shortnames are keynames and they dont have spaces like the longName/DisplayName */
def checkPluginVersion(String pluginShortName, String version, Map pipelineParams)
{
   if (!checkCredentialsIdExists("${pipelineParams.JENKINS_CREDENTIALS}")) 
   {
      logger.logInfo("No credentials with ID '${pipelineParams.JENKINS_CREDENTIALS}' found. Skipping plugin check for '${pluginShortName}' with a version of '${version}'")
	   return -1
   }
   
   def pluginVersionInfo
   try {
      pluginVersionInfo = curl credentialsId: pipelineParams.JENKINS_CREDENTIALS, 
                               globbingEnabled: false, 
                               url: "$JENKINS_URL/pluginManager/api/xml?depth=1&xpath=//plugin[shortName='${pluginShortName}']/version",
                               returnStdout: true
   }
   catch(e) {
      logger.logWarning("There was a problem attempting to verify the ${pluginShortName} plugin version, which may result in unexpected behavior; the error was ${e.getMessage()}")
      return -1
   }
                            
   //Verify that plugin version info was returned
   if(pluginVersionInfo.contains("<version>")) {
      //If CurrentVersion is less than the version variable the compareTo will return a negative number
      //return a 0 if the versions are equal and a positive number if the CurrentVersion is higher than the specifice version.
      def currentVersion = pluginVersionInfo.replaceAll("</?version>", "")
      return currentVersion.compareTo(version)
   }
   else {
      logger.logWarning("""
      Unable to retrieve version information for plugin "${pluginShortName}". Unable to compare to required version "${version}"
      curl output = ${pluginVersionInfo}
      """)
      return -1
   }
}