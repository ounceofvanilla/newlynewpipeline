import jenkins.model.Jenkins
import java.util.logging.Logger
import hudson.markup.RawHtmlMarkupFormatter


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
// Import groovy classes
if (Jenkins.getInstance().isQuietingDown())
{
   // A previous script issued a restart; bail
   Logger.getLogger("").info("Jenkins restart is pending; will continue processing after restart")
   return
}

// Import groovy classes
String jenkinsHome = "/var/jenkins_home"
String supportingFilesDir = "${jenkinsHome}/init.groovy.d/supportingFiles"
String libDir = "${supportingFilesDir}/utils"
importGroovyClasses(libDir,
                    "EnvironmentUtils")

def instance = Jenkins.getInstance()
def envVars = EnvironmentUtils.getSafeEnvVars(instance)
def logger = EnvironmentUtils.getDevOpsLogger(this.class.name)
String initKey = "sampleJobConfigComplete"

// Only perform initialization if we haven't done it before.
if (envVars[initKey] != 'true')
{
   logger.info("Initial system startup; adding sample jobs pipelines")

   if (!instance.getItem("DOCPF_Sample_Jobs"))
   {
      def jobDslScript = new File("${supportingFilesDir}/sampleJobsDsl.groovy")
      def workspace = new File("${jenkinsHome}")

      // Set the instance to use safe HTML markup
      instance.markupFormatter = new RawHtmlMarkupFormatter(false)

      EnvironmentUtils.runJobDslScript(workspace, jobDslScript)

      logger.info("Sample job installation complete; saving environment")
      // Persist the new environment variable so we don't do the startup process again.
      envVars[initKey] = 'true'
      instance.save()
   }
   else
   {
      logger.info("DOCPF_Sample_Jobs folder already exists; skipping sample job installation.")
   }
}
else
{
   logger.info("Initial system sample job installation was already completed; skipping.")
}