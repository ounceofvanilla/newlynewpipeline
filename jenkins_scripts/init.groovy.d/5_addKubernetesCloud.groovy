import java.util.logging.Logger
import hudson.model.*
import jenkins.model.*
import org.csanchez.jenkins.plugins.kubernetes.*
import org.csanchez.jenkins.plugins.kubernetes.volumes.workspace.EmptyDirWorkspaceVolume
import org.csanchez.jenkins.plugins.kubernetes.volumes.HostPathVolume
import org.csanchez.jenkins.plugins.kubernetes.model.KeyValueEnvVar

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
                    "EnvironmentUtils")

def instance = Jenkins.getInstance()
def envVars = EnvironmentUtils.getSafeEnvVars(instance)
def logger = EnvironmentUtils.getDevOpsLogger(this.class.name)
String initKey = "kubernetesCloudConfigComplete"
String jenkins_URL = new File("${jenkinsHome}/jenkins_URL").text

if (envVars[initKey] != 'true')
{
   logger.info("Initial system startup; setting up Kubernetes cloud")

   def kc
   try {
        
      //Set cloud parameters
      logger.info("===> Configuring k8s...")
      String agentImage = "lnvle1865.gs.myharris.net:5000/centos_7.7_jenkins_slave:4.0.0"

      kc = new KubernetesCloud("kubernetes")
      instance.clouds.add(kc)
      logger.info("Cloud config added: ${Jenkins.instance.clouds}")
      
      //Set pod parameters
      def podTemplate = new PodTemplate()
      podTemplate.setLabel("MINIKUBE_AGENT")
      podTemplate.setName("DOCPF Container")

      podTemplate.setNodeUsageMode('EXCLUSIVE')
      podTemplate.setWorkspaceVolume(new EmptyDirWorkspaceVolume(false))

      //Set container parameters 
      ContainerTemplate ct = new ContainerTemplate("jnlp", agentImage)
      ct.setAlwaysPullImage(true)
      ct.setPrivileged(false)
      ct.setTtyEnabled(false)
      ct.setWorkingDir("/home/jenkins")
      ct.setCommand('/usr/local/bin/kube-startup')
      ct.setArgs("${jenkins_URL} " + '${computer.jnlpmac} ${computer.name}')
      podTemplate.setContainers([ct])
      kc.templates << podTemplate
      kc = null
      logger.info("===> Configuring k8s completed")
      envVars[initKey] = 'true'
      instance.save()
      instance.doSafeRestart(null)
      }
   catch(Exception e) {
      logger.info("===> Failed to configure kubernetes: ", e)
      System.exit(1)
   }
}
else { 
logger.info("Kubernetes cloud setup was already completed; skipping.")
}