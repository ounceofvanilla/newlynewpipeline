// Methods in this file will end up as object methods on the object that load returns.
@groovy.transform.Field
def originalScript
@groovy.transform.Field
def overriddenParamChecked = false

def doThisAlways(Map pipelineParams, Closure method)
{
   // Load original program-specific script if necessary
   // Reset ORIGINAL_PROGRAM_SPECIFIC_SCRIPT if parsing rendered null as a string
   def originalScriptParam = pipelineParams.ORIGINAL_PROGRAM_SPECIFIC_SCRIPT == 'null' ? '' : pipelineParams.ORIGINAL_PROGRAM_SPECIFIC_SCRIPT
   if (!originalScript && originalScriptParam)
   {
      try
      {
         logger.logInfo("Loading original script: ${originalScriptParam}")
         originalScript = load(originalScriptParam)
      }
      catch (Exception e)
      {
         logger.logError("""Unable to load original program-specific script: ${originalScriptParam}
                 |   ${e.getMessage()}""".stripMargin())
      }
   }
   
   // Check the parameter that should have been overridden by the job-level parameter
   if (!overriddenParamChecked && pipelineParams.OVERRIDDEN_PARAMETER != 'OVERRIDDEN!')
   {
      logger.logError("pipelineParams.OVERRIDDEN_PARAMETER was not overridden as expected")
      overriddenParamChecked = true
   }
   
   // Call any applicable method from the originalScript
   // No such method errors will be caught by the devops_pipelines's prePostProcessing.groovy
   method(pipelineParams)
}

def preBuild(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preBuild(it) }
}

def postBuild(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postBuild(it) }
}

def preFpgaCompile(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preFpgaCompile(it) }
}

def postFpgaCompile(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postFpgaCompile(it) }
}

def preUnitTest(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preUnitTest(it) }
}

def postUnitTest(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postUnitTest(it) }
}

def preSCA(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preSCA(it) }
}

def postSCA(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postSCA(it) }
}

def preSAST(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preSAST(it) }
}

def postSAST(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postSAST(it) }
}

def preFpgaLint(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preFpgaLint(it) }
}

def postFpgaLint(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postFpgaLint(it) }
}

def preFpgaVerifyCDC(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preFpgaVerifyCDC(it) }
}

def postFpgaVerifyCDC(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postFpgaVerifyCDC(it) }
}

def preCodeCoverage(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preCodeCoverage(it) }
}

def postCodeCoverage(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postCodeCoverage(it) }
}

def preArtifactManagement(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preArtifactManagement(it) }
}

def postArtifactManagement(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postArtifactManagement(it) }
}

def preArtifactDeployment(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preArtifactDeployment(it) }
}

def postArtifactDeployment(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postArtifactDeployment(it) }
}

def preFunctionalTest(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preFunctionalTest(it) }
}

def postFunctionalTest(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postFunctionalTest(it) }
}

def preDAST(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preDAST(it) }
}

def postDAST(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postDAST(it) }
}

def preMergeBranch(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preMergeBranch(it) }
}

def postMergeBranch(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postMergeBranch(it) }
}

def preTagSourceCode(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preTagSourceCode(it) }
}

def postTagSourceCode(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postTagSourceCode(it) }
}

def prePromoteArtifact(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.prePromoteArtifact(it) }
}

def postPromoteArtifact(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postPromoteArtifact(it) }
}

def preStyleCheck(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preStyleCheck(it) }
}

def postStyleCheck(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postStyleCheck(it) }
}

def preArchiveArtifacts(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preArchiveArtifacts(it) }
}

def postArchiveArtifacts(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postArchiveArtifacts(it) }
}

def prePackageForDelivery(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.prePackageForDelivery(it) }
}

def postPackageForDelivery(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postPackageForDelivery(it) }
}

def preDockerImageManagement(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.preDockerImageManagement(it) }
}

def postDockerImageManagement(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.postDockerImageManagement(it) }
}

def sendNotification(Map pipelineParams)
{
   doThisAlways(pipelineParams) { originalScript?.sendNotification(it) }
}

/* This bare checkout command will get run when this script is loaded.
 * It will make sure tool repo is checked out by the time stage processing starts. */
checkout([$class: 'GitSCM',
          branches: [[name: env.GIT_TOOL_VERSION]],
          extensions: [[$class: 'LocalBranch',
                        localBranch: '**'],
                       [$class: 'RelativeTargetDirectory',
                        relativeTargetDir: '.'],
                       [$class: 'CloneOption', 
                        noTags: true, 
                        shallow: true, 
                        reference: '']],
          userRemoteConfigs: [[url: env.GIT_TOOL_URL]]
         ])

return this