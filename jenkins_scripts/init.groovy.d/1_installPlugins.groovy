import jenkins.model.Jenkins
import java.util.logging.Logger


/** Dynamically import classes defined in Groovy files. This will add a 
  * property to the script for each class name specified. Each property 
  * will contain the Class object for that class. New instances of these 
  * classes can be created with the newInstance() method of Class.
  *
  * @param libPath     path to the directory containing the groovy classes
  * @param classNames  the list of class names to import
  **/
void importGroovyClasses(String libPath, String... classNames)
{
   GroovyClassLoader gcl = this.class.getClassLoader().parent
   /* add libPath to the classpath so dependencies will be 
      automatically found */
   gcl.addClasspath(libPath)

   classNames.each { className ->
      String path = "${libPath}/${className}.groovy"
      this.metaClass[className] = gcl.parseClass(new File(path))
   }
}

/* Main system intitialization processing. */
if (Jenkins.getInstance().isQuietingDown())
{
   // A previous script issued a restart; bail
   Logger.getLogger("").info("Jenkins restart is pending; will continue processing after restart")
   return
}

// Import groovy classes
String jenkinsHome = "/var/jenkins_home"
String libDir = "${jenkinsHome}/init.groovy.d/supportingFiles/utils"
importGroovyClasses(libDir,
                    "EnvironmentUtils",
                    "ArtifactoryConnection")

def instance = Jenkins.getInstance()
def envVars = EnvironmentUtils.getSafeEnvVars(instance)
def logger = EnvironmentUtils.getDevOpsLogger(this.class.name)
String initKey = "initialPluginConfigComplete"
String artifactoryHost = "lnsvr0310.gcsd.harris.com"
int artifactoryPort = 8443

// Only perform initialization if we haven't done it before.
if (envVars[initKey] != 'true')
{
   logger.info("Initial system startup; installing required plugins")

   String repoFilePath = "artifactory/devops_factory_jenkins/plugins"
   String localPluginDir = "${jenkinsHome}/plugins"
   // The list of short plugin names to install from Artifactory
   def pluginList = ["code-coverage-api",   //dependency
                     "cobertura",
                     "dtkit-api",           //dependency
                     "xunit",
                     "docker-java-api",     //dependency
                     "docker-plugin",
                     "jaxb",                //dependency
                     "cppcheck",
                     "htmlpublisher",
                     "jquery",              //dependency
                     "git-parameter",
                     "pipeline-utility-steps-2.6.1",
                     "sloccount",
                     "TestComplete",
                     "trilead-api"          //dependency
                     ]
   int numInstalled = 0
   boolean pluginInstallSkipped = false
   
   String secretsPath = "${jenkinsHome}/secrets"
   def svcUserFile = new File("${secretsPath}/svcUser")
   def svcApiKeyFile = new File("${secretsPath}/svcApiKey")
   def creds = [:]
   if (svcUserFile.exists() && svcApiKeyFile.exists())
   {
      creds["username"] = svcUserFile.text.trim()
      creds["password"] = svcApiKeyFile.text.trim()

      def repo = ArtifactoryConnection.newInstance(artifactoryHost,
                                                artifactoryPort,
                                                creds)

      pluginList.each { pluginName ->
         logger.info("Installing '$pluginName' to '$localPluginDir'")
         if (repo.download("${repoFilePath}/${pluginName}.jpi", "${localPluginDir}/${pluginName}.jpi"))
         {
            numInstalled++
         }
      }
   }
   else
   {
      // Assume failure to provide credentials means install should be skipped altogether
      pluginInstallSkipped = true
      logger.warning("The secrets files for the plugin repo account were not found; skipping install of plugins")
   }

   if (pluginInstallSkipped || numInstalled == pluginList.size())
   {
      logger.info("Initial plugin config complete; saving and safely restarting Jenkins")
      // Persist the new environment variable so we don't do the startup process again.
      envVars[initKey] = 'true'
      instance.save()
      instance.doSafeRestart(null)
   }
   else
   {
      logger.warning("Not all plugins were installed during startup (please check system log); skipping restart")
   }
}
else
{
   logger.info("Initial system startup was already completed; skipping.")
}