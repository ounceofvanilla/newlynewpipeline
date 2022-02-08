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


/* Main system intitialization processing. */
if (Jenkins.getInstance().isQuietingDown())
{
   // A previous script issued a restart; bail
   Logger.getLogger("").info("Jenkins restart is pending; will continue processing after restart")
   return
}

// Import groovy classes
String jenkinsHome = "/var/jenkins_home"
String supportingFiles = "${jenkinsHome}/init.groovy.d/supportingFiles"
String libDir = "${supportingFiles}/utils"
importGroovyClasses(libDir,
                    "EnvironmentUtils")

def instance = Jenkins.getInstance()
def envVars = EnvironmentUtils.getSafeEnvVars(instance)
def logger = EnvironmentUtils.getDevOpsLogger(this.class.name)
String initKey = "configFinalizationComplete"

//Update plugins
def plugins = Jenkins.getInstance().pluginManager.activePlugins.findAll {
  it -> it.hasUpdate()
}.collect {
  it -> it.getShortName()
}

Jenkins.getInstance().pluginManager.install(plugins, false).each { f ->
  f.get()
}

// Only perform initialization if we haven't done it before.
if (envVars[initKey] != 'true')
{
   // Set a DOCPF description on the landing page
   instance.getView("All").description = """<h2>Now with DOCPF!</h2>
      |<p>This Jenkins instance comes prepackaged with the DevOps Common Pipeline 
      |Framework (DOCPF) Sample Jobs collection to get you started. Navigate to 
      |the jobs inside the DOCPF_Sample_Jobs folder to see what a job might look 
      |like with various tools configured.</p>
      |<p>For instructions on creating and configuring your own Jenkins job, visit 
      |the <a 
      |href="https://confluenceopen01/display/DOCPF/Configure+Jenkins+to+run+DOCPF+sample+jobs">
      |DOCPF Confluence Page</a>.</p>
      |<p>For additional assistance with your pipelines, please contact: <a 
      |href="mailto:DOCPF_Leads@L3Harris.com">DOCPF_Leads@L3Harris.com</a>.</p>
      |<p>For issues with your Jenkins instance, please contact EIT.</p>""".stripMargin()

   envVars[initKey] = 'true'
   logger.info("Initial configuration was completed; saving one last time")
   instance.save()

   // Create a file that can be monitored externally to indicate completion
   def completedFile = new File("${jenkinsHome}/initComplete")
   completedFile.createNewFile()

   logger.info("Save complete; restarting Jenkins")
   // Restart Jenkins to complete all config
   instance.doSafeRestart(null)
}