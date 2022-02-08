import java.util.logging.Logger
import jenkins.model.*


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

/** Install a plugin with the given short name on the provided instance.
  *
  * @param instance       the Jenkins instance
  * @param logger         the logger to use for log messages
  * @param shortName      the short name of the plugin
  * @param shouldRestart  whether or not to immediately restart after install
  **/
static void installPlugin(def instance, def logger, String shortName, boolean shouldRestart = false)
{
   logger.info("Installing plugin ${shortName}")

   if (instance.pluginManager.getPlugin(shortName))
   {
      logger.info("Plugin '${shortName}' was already installed; skipping")
      return
   }

   def plugin = instance.updateCenter.getPlugin(shortName)
   if (plugin)
   {
      def error = plugin.deploy().get().getError()
      if (error == null)
      {
         logger.info("Plugin installation successful")
	 if (shouldRestart)
	 {
	    logger.info("Restarting Jenkins instance")
            instance.doSafeRestart(null)
	 }
      }
      else
      {
         logger.warning("Failed to install plugin '${shortName}'; error: ${error.getMessage()}")
      }
   }
   else
   {
      logger.warning("Unable to retrieve plugin '${shortName}' from update center; initialization will fail")
   }
}


/* Main system intitialization processing. */
// Import groovy classes
String jenkinsHome = "/var/jenkins_home"
String supportingFiles = "${jenkinsHome}/init.groovy.d/supportingFiles"
String libDir = "${supportingFiles}/utils"
// Use the regular logger until EnvironmentUtils can be imported
def logger = Logger.getLogger("")
def instance = Jenkins.getInstance()
boolean restartNeeded = false

// Before importing EnvironmentUtils, make sure the required plugins are deployed.
["credentials", "job-dsl"].each { pluginName ->
   if (!instance.pluginManager.getPlugin(pluginName))
   {
      logger.info("Plugin '${pluginName}' needed to proceed; attempting to install")
      instance.updateCenter.updateAllSites()
      installPlugin(instance, logger, pluginName, !restartNeeded)
      restartNeeded = true
   }
}

// Do restart if plugins were installed
if (restartNeeded)
{
   logger.info("Restarting Jenkins to complete installation")
   instance.doSafeRestart(null)
   return
}

// Do dynamic imports
importGroovyClasses(libDir,
                    "EnvironmentUtils")
def envVars = EnvironmentUtils.getSafeEnvVars(instance)
String initKey = "initialPreconfigComplete"
// Re-instantiate logger for DevOps
logger = EnvironmentUtils.getDevOpsLogger(this.class.name)

// Only perform initialization if we haven't done it before.
if (!instance.isQuietingDown() && envVars[initKey] != 'true')
{
   logger.info("Initial system startup; preconfiguring Jenkins")
  
   // Get initProps
   def initProps = new Properties()
   def propsFile = new File("${jenkinsHome}/initConfig.properties")
   if (propsFile.exists())
   {
      propsFile.withInputStream {
         initProps.load(it)
      }

      // Configure root url and dummy admin email
      logger.info("Setting root URL to ${initProps.rootUrl}")
      def locationConfig = JenkinsLocationConfiguration.get()
      locationConfig.setUrl(initProps.rootUrl)

      logger.info("Setting admin address to ${initProps.adminEmail}")
      locationConfig.setAdminAddress(initProps.adminEmail)
      locationConfig.save()

      logger.info("Setting slave agent port to ${initProps.slaveAgentPort}")
      instance.slaveAgentPort = initProps.slaveAgentPort.toInteger()

      // Cleanup
      propsFile.delete()
   }
   else
   {
      logger.warning("Unable to locate initConfig properties file; skipping relevant config")
   }

   // Add admin account from secrets files
   String secretsPath = "${jenkinsHome}/secrets"
   def adminUserFile = new File("${secretsPath}/adminUser")
   def adminPasswordFile = new File("${secretsPath}/adminPassword")
   if (adminUserFile.exists() && adminPasswordFile.exists())
   {
      logger.info("Adding admin account")
      
      EnvironmentUtils.addJenkinsAdmin(instance,
                                       adminUserFile.text.trim(),
				       adminPasswordFile.text.trim())

   }
   else
   {
      logger.warning("Admin user and password secret files not found; admin account setup skipped")
   }
   
   // Install default plugin list
   def pluginList = new File("${supportingFiles}/startupPlugins.txt")
   if (pluginList.exists())
   {
      logger.info("Installing base plugins")
      
      pluginList.eachLine { pluginShortName ->
         installPlugin(instance, logger, pluginShortName)
      }
   }
   else
   {
      logger.warning("Initial plugin list not found; plugin installation skipped")
   }
   
   logger.info("Initial preconfigure complete; saving environment")
   // Persist the new environment variable so we don't do the startup process again.
   envVars[initKey] = 'true'
   instance.save()

   logger.info("Restarting Jenkins before initialization proceeds")
   instance.doSafeRestart(null)
}
else
{
   logger.info("Initial system startup was already completed; skipping.")
}