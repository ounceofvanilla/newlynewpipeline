// Methods in this file will end up as object methods on the object that load returns.
def programSpecificScript
def programSpecificScriptLoaded
def programSpecificMessage

// Method to load the programSpecificScript
void initPrePostProcessing(Map pipelineParams) {
   programSpecificScriptLoaded = false
   // Ensure the default is an empty string instead of null.
   pipelineParams.PROGRAM_SPECIFIC_SCRIPT = "${pipelineParams.PROGRAM_SPECIFIC_SCRIPT ?: ''}"
   pipelineParams.PROGRAM_SPECIFIC_SCRIPT_REPO = "${pipelineParams.PROGRAM_SPECIFIC_SCRIPT_REPO ?: ''}"
   pipelineParams.PROGRAM_SPECIFIC_SCRIPT_BRANCH = "${pipelineParams.PROGRAM_SPECIFIC_SCRIPT_BRANCH ?: 'master'}"

   def programScript = params["OVERRIDE_PROGRAM_SPECIFIC_SCRIPT"]
  
   if (programScript)
   {
      logger.logInfo("Overriding Jenkinsfile parameter of PROGRAM_SPECIFIC_SCRIPT with job-level parameter value '${programScript}'")
      pipelineParams.PROGRAM_SPECIFIC_SCRIPT = programScript
   }
  
   if (pipelineParams.PROGRAM_SPECIFIC_SCRIPT)
   {
      logger.logInfo("PROGRAM_SPECIFIC_SCRIPT = ${pipelineParams.PROGRAM_SPECIFIC_SCRIPT}")
      try
      {
         if (pipelineParams.PROGRAM_SPECIFIC_SCRIPT_REPO) {
            def uuid = UUID.randomUUID().toString()
            dir ("resources-" + uuid)
            {
               checkout([$class: 'GitSCM', branches: [[name: "${pipelineParams.PROGRAM_SPECIFIC_SCRIPT_BRANCH}"]],
                  userRemoteConfigs: [[url: "${pipelineParams.PROGRAM_SPECIFIC_SCRIPT_REPO}"]],
                  extensions: [[$class: 'SparseCheckoutPaths',  sparseCheckoutPaths:[[$class:'SparseCheckoutPath', path:"${pipelineParams.PROGRAM_SPECIFIC_SCRIPT}"]]]]])
               programSpecificScript = load("${pipelineParams.PROGRAM_SPECIFIC_SCRIPT}")
            }
         }
         else {
            programSpecificScript = load("${pipelineParams.PROGRAM_SPECIFIC_SCRIPT}")
         }
         programSpecificScriptLoaded = true
      }
      catch (Exception e)
      {
         logger.logError("Unable to load the program-specific script: ${pipelineParams.PROGRAM_SPECIFIC_SCRIPT}")
         logger.logError(e.toString())
      }
   }
   else {
      logger.logInfo("No PROGRAM_SPECIFIC_SCRIPT defined, using processing from shared library (if exists), or devops-pipeline")
      programSpecificMessage = "ProgramSpecificScript not defined"
   }
}

// Generic pre/post processing method (returns true if program-specific method was implemented)
def doProcessing(Map pipelineParams, String methodName, def methodClosure) 
{
   def infoMessage = constants.NO_PROGRAM_SPECIFIC_METHOD + methodName
   if (programSpecificScriptLoaded)
   {
      try
      {
         logger.logInfo(constants.CALL_PROGRAM_METHOD + methodName)
         methodClosure(pipelineParams)
         logger.logInfo(constants.COMPLETE_PROGRAM_METHOD + methodName)
         return true
      }
      catch (NoSuchMethodError e)
      {
         processException(methodName, infoMessage, e)
      }
   }
   else
   {
      logger.logInfo("${infoMessage} ${programSpecificMessage}")
   }
   return false
}

// In all the pre/post methods we check for the programSpecificScriptLoaded if true, 
// we do a try and catch while executing the programSpecificScript, this will catch any
// NoSuchMethod Error, if the methods are missing it will echo the default message.
def preBuild(Map pipelineParams)
{
   def methodName = "preBuild"
   def methodClosure = { params ->
      programSpecificScript.preBuild(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postBuild(Map pipelineParams) {
   def methodName = "postBuild"
   def methodClosure = { params ->
      programSpecificScript.postBuild(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preCreateInstaller(Map pipelineParams)
{
   def methodName = "preCreateInstaller"
   def methodClosure = { params ->
      programSpecificScript.preCreateInstaller(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postCreateInstaller(Map pipelineParams) {
   def methodName = "postCreateInstaller"
   def methodClosure = { params ->
      programSpecificScript.postCreateInstaller(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preFpgaCompile(Map pipelineParams)
{
   def methodName = "preFpgaCompile"
   def methodClosure = { params ->
      programSpecificScript.preFpgaCompile(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postFpgaCompile(Map pipelineParams) {
   def methodName = "postFpgaCompile"
   def methodClosure = { params ->
      programSpecificScript.postFpgaCompile(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preUnitTest(Map pipelineParams){
   def methodName = "preUnitTest"
   def methodClosure = { params ->
      programSpecificScript.preUnitTest(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postUnitTest(Map pipelineParams){
   def methodName = "postUnitTest"
   def methodClosure = { params ->
      programSpecificScript.postUnitTest(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preSCA(Map pipelineParams){
   def methodName = "preSCA"
   def methodClosure = { params ->
      programSpecificScript.preSCA(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postSCA(Map pipelineParams){
   def methodName = "postSCA"
   def methodClosure = { params ->
      programSpecificScript.postSCA(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preSAST(Map pipelineParams){
   def methodName = "preSAST"
   def methodClosure = { params ->
      programSpecificScript.preSAST(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postSAST(Map pipelineParams){
   def methodName = "postSAST"
   def methodClosure = { params ->
      programSpecificScript.postSAST(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preFpgaLint(Map pipelineParams)
{
   def methodName = "preFpgaLint"
   def methodClosure = { params ->
      programSpecificScript.preFpgaLint(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postFpgaLint(Map pipelineParams)
{
   def methodName = "postFpgaLint"
   def methodClosure = { params ->
      programSpecificScript.postFpgaLint(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preFpgaVerifyCDC(Map pipelineParams)
{
   def methodName = "preFpgaVerifyCDC"
   def methodClosure = { params ->
      programSpecificScript.preFpgaVerifyCDC(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postFpgaVerifyCDC(Map pipelineParams)
{
   def methodName = "postFpgaVerifyCDC"
   def methodClosure = { params ->
      programSpecificScript.postFpgaVerifyCDC(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preCodeCoverage(Map pipelineParams){
   def methodName = "preCodeCoverage"
   def methodClosure = { params ->
      programSpecificScript.preCodeCoverage(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postCodeCoverage(Map pipelineParams){
   def methodName = "postCodeCoverage"
   def methodClosure = { params ->
      programSpecificScript.postCodeCoverage(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preArtifactManagement(Map pipelineParams) {
   def methodName = "preArtifactManagement"
   def methodClosure = { params ->
      programSpecificScript.preArtifactManagement(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postArtifactManagement(Map pipelineParams) {
   def methodName = "postArtifactManagement"
   def methodClosure = { params ->
      programSpecificScript.postArtifactManagement(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preArtifactDeployment(Map pipelineParams) {
   def methodName = "preArtifactDeployment"
   def methodClosure = { params ->
      programSpecificScript.preArtifactDeployment(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postArtifactDeployment(Map pipelineParams) {
   def methodName = "postArtifactDeployment"
   def methodClosure = { params ->
      programSpecificScript.postArtifactDeployment(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preFunctionalTest(Map pipelineParams) {
   def methodName = "preFunctionalTest"
   def methodClosure = { params ->
      programSpecificScript.preFunctionalTest(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postFunctionalTest(Map pipelineParams) {
   def methodName = "postFunctionalTest"
   def methodClosure = { params ->
      programSpecificScript.postFunctionalTest(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preDAST(Map pipelineParams) {
   def methodName = "preDAST"
   def methodClosure = { params ->
      programSpecificScript.preDAST(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postDAST(Map pipelineParams) {
   def methodName = "postDAST"
   def methodClosure = { params ->
      programSpecificScript.postDAST(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preMergeBranch(Map pipelineParams) {
   def methodName = "preMergeBranch"
   def methodClosure = { params ->
      programSpecificScript.preMergeBranch(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postMergeBranch(Map pipelineParams) {
   def methodName = "postMergeBranch"
   def methodClosure = { params ->
      programSpecificScript.postMergeBranch(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preTagSourceCode(Map pipelineParams) {
   def methodName = "preTagSourceCode"
   def methodClosure = { params ->
      programSpecificScript.preTagSourceCode(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postTagSourceCode(Map pipelineParams) {
   def methodName = "postTagSourceCode"
   def methodClosure = { params ->
      programSpecificScript.postTagSourceCode(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def prePromoteArtifact(Map pipelineParams) {
   def methodName = "prePromoteArtifact"
   def methodClosure = { params ->
      programSpecificScript.prePromoteArtifact(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postPromoteArtifact(Map pipelineParams) {
   def methodName = "postPromoteArtifact"
   def methodClosure = { params ->
      programSpecificScript.postPromoteArtifact(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preStyleCheck(Map pipelineParams) {
   def methodName = "preStyleCheck"
   def methodClosure = { params ->
      programSpecificScript.preStyleCheck(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postStyleCheck(Map pipelineParams) {
   def methodName = "postStyleCheck"
   def methodClosure = { params ->
      programSpecificScript.postStyleCheck(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preArchiveArtifacts(Map pipelineParams) {
   def methodName = "preArchiveArtifacts"
   def methodClosure = { params ->
      programSpecificScript.preArchiveArtifacts(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postArchiveArtifacts(Map pipelineParams) {
   def methodName = "postArchiveArtifacts"
   def methodClosure = { params ->
      programSpecificScript.postArchiveArtifacts(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def prePackageForDelivery(Map pipelineParams) {
   def methodName = "prePackageForDelivery"
   def methodClosure = { params ->
      programSpecificScript.prePackageForDelivery(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postPackageForDelivery(Map pipelineParams) {
   def methodName = "postPackageForDelivery"
   def methodClosure = { params ->
      programSpecificScript.postPackageForDelivery(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preDockerImageManagement(Map pipelineParams) {
   def methodName = "preDockerImageManagement"
   def methodClosure = { params ->
      programSpecificScript.preDockerImageManagement(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postDockerImageManagement(Map pipelineParams) {
   def methodName = "postDockerImageManagement"
   def methodClosure = { params ->
      programSpecificScript.postDockerImageManagement(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preDockerImageBuild(Map pipelineParams) {
   def methodName = "preDockerImageBuild"
   def methodClosure = { params ->
      programSpecificScript.preDockerImageBuild(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postDockerImageBuild(Map pipelineParams) {
   def methodName = "postDockerImageBuild"
   def methodClosure = { params ->
      programSpecificScript.postDockerImageBuild(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preDockerImagePush(Map pipelineParams) {
   def methodName = "preDockerImagePush"
   def methodClosure = { params ->
      programSpecificScript.preDockerImagePush(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postDockerImagePush(Map pipelineParams) {
   def methodName = "postDockerImagePush"
   def methodClosure = { params ->
      programSpecificScript.postDockerImagePush(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def preContainerScan(Map pipelineParams){
   def methodName = "preContainerScan"
   def methodClosure = { params ->
      programSpecificScript.preContainerScan(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def postContainerScan(Map pipelineParams){
   def methodName = "postContainerScan"
   def methodClosure = { params ->
      programSpecificScript.postContainerScan(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
} 


def alwaysPost(Map pipelineParams){
   def methodName = "alwaysPost"
   def methodClosure = { params ->
      programSpecificScript.alwaysPost(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def failurePost(Map pipelineParams){
   def methodName = "failurePost"
   def methodClosure = { params ->
      programSpecificScript.failurePost(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def successPost(Map pipelineParams){
   def methodName = "successPost"
   def methodClosure = { params ->
      programSpecificScript.successPost(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def unstablePost(Map pipelineParams){
   def methodName = "unstablePost"
   def methodClosure = { params ->
      programSpecificScript.unstablePost(params)
   }
   doProcessing(pipelineParams, methodName, methodClosure)
}

def sendNotification(Map pipelineParams) {
   def methodName = "sendNotification"
   def methodClosure = { params ->
      programSpecificScript.sendNotification(params)
   }

   if(!doProcessing(pipelineParams, methodName, methodClosure))
   {
      logger.logInfo("Send Job Notification") 
      logger.logInfo("Build Status: ${currentBuild.currentResult}")
      buildStatus = currentBuild.currentResult ?: 'SUCCESSFUL'

      // If the EMAIL_RECIPIENTS parameter is specified, use it, otherwise email will just go to Submitter
      def emailTo 
      try {
         if (EMAIL_RECIPIENTS) {
            emailTo = EMAIL_RECIPIENTS
         } 
      } catch (Exception e) {
         emailTo = 'Do-Not-Reply@L3Harris.com'
      }

      // Default values
      def subject = "${buildStatus}: ${env.JOB_NAME}:${env.BUILD_NUMBER} - ${GIT_BRANCH}"
      def summary = "${subject} (${env.BUILD_URL})"
      def details = """
         See attached build log output or check console output at "${env.JOB_NAME} [${env.BUILD_NUMBER}]" 
         ${BUILD_URL}
      """

      emailext (
         attachLog: true,
         compressLog: true,
         subject: subject,
         body: details,
         recipientProviders: [[$class: 'RequesterRecipientProvider']],
         to: emailTo
      )
   }

   // Do this always just to make sure programs know about newer versions
   sendReleaseNotification()
}

def sendReleaseNotification() {
   logger.logInfo("Send DOCPF Release Notification")
   def docpfReleaseFile = "/programs/DOCPF/DOCPF_RELEASE.txt"

   if(fileExists("${docpfReleaseFile}")) {
      def releaseInformation = readFile("${docpfReleaseFile}")
      def subject = "New DOCPF release is available"

      def emailTo
      if (EMAIL_RECIPIENTS) {
         emailTo = EMAIL_RECIPIENTS
      }

      emailext(
            subject: subject,
            body: releaseInformation,
            recipientProviders: [
               [$class: 'RequesterRecipientProvider']
            ],
            to: emailTo
            )
   } else {
      logger.logInfo("No New DOCPF release.")
   }
}

// Verify whether the exception is thrown because the method was not overridden, 
// or because it was overridden but has errors. If the latter, throw the exception
// up the stack so that the developer can debug.
// The message returned when the overridden method is not found is:
// "No such DSL method 'methodName' found". To reduce reliance on a very specific
// message, only search for the minimum to insure a match.
def processException(methodName, infoMessage, exc) {
   if (exc.toString().contains("method '" + methodName + "' found")) {
      logger.logInfo(infoMessage)
   } 
   else {
      throw exc
   }
}