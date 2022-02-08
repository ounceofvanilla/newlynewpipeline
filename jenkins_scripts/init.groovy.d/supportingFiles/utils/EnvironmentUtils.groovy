import hudson.security.*
import java.util.logging.*
import jenkins.install.*
import jenkins.model.*
import hudson.slaves.EnvironmentVariablesNodeProperty
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement
import jenkins.security.s2m.AdminWhitelistRule


/** A utility class for retrieving Jenkins environment data **/
class EnvironmentUtils
{
   /** Get a reference to the environment variables, creating a new one if it doesn't exist.
     *
     * @param instance   the Jenkins instance containing the variables
     * @return envVars   the collection of environment variables
     **/
   static def getSafeEnvVars(def instance)
   {
      def globalNodeProperties = instance.getGlobalNodeProperties()
      def envVarsNodePropertyList = globalNodeProperties.getAll(EnvironmentVariablesNodeProperty.class)
      def envVars = null

      if (envVarsNodePropertyList == null || envVarsNodePropertyList.size() == 0)
      {
         def newEnvVarsNodeProperty = new EnvironmentVariablesNodeProperty()
         globalNodeProperties.add(newEnvVarsNodeProperty)
         envVars = newEnvVarsNodeProperty.getEnvVars()
      }
      else
      {
         envVars = envVarsNodePropertyList.get(0).getEnvVars()
      }

      return envVars
   }

   /** Add a Jenkins administrator account.
     *
     * @param description a text description of the credentials
     * @param username    the username to add
     * @param passkey     the password or encrypted API key for the username
     **/
   static void addJenkinsAdmin(def instance, String username, String password)
   {
      if (!(instance.getSecurityRealm() instanceof HudsonPrivateSecurityRealm))
      {
         instance.setSecurityRealm(new HudsonPrivateSecurityRealm(false))
      }

      if (!(instance.getAuthorizationStrategy() instanceof FullControlOnceLoggedInAuthorizationStrategy))
      {
         instance.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy())
      }

      instance.getSecurityRealm().createAccount(username, password)
      instance.getAuthorizationStrategy().setAllowAnonymousRead(false)
      instance.getInjector().getInstance(AdminWhitelistRule.class).setMasterKillSwitch(false)

      InstallUtil.proceedToNextStateFrom(InstallState.INITIAL_SETUP_COMPLETED)

      instance.save()
   }

   /** Run a job-dsl plugin script.
     *
     * @param workspace     a directory to use as the workspace
     * @param jobDslScript  the job-dsl script to run
     **/
   static void runJobDslScript(File workspace, File jobDslScript)
   {
      def jobManagement = new JenkinsJobManagement(System.out, [:], workspace)
      def scriptLoader = new DslScriptLoader(jobManagement)
      scriptLoader.runScript(jobDslScript.text)
   
   }

   /** Get a logger with a handler that will prepend messages with a DevOps identifier
     * for easier filtering.
     *
     * @param   the logger name (preferably the class or source name)
     * @return  the wrapped logger
     **/
   static def getDevOpsLogger(String srcName)
   {
      def logger = LogManager.getLogManager().getLogger("hudson.WebAppMain")

      // Remove any previously added handlers
      logger.getHandlers().each { handler ->
         logger.removeHandler(handler)
      }

      // Add DevOps handler for the specified source name
      logger.addHandler(new ConsoleHandler() {
         private String prepend = "DevOpsLogger - [${srcName}]:"
         @Override
         public void publish(LogRecord record)
         {
            record.setMessage("${this.prepend} ${record.getMessage()}")
	    super.publish(record)
	 }
      })

      return logger
   }
}
