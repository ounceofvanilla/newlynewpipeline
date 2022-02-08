import hudson.model.*
import hudson.slaves.EnvironmentVariablesNodeProperty
import java.util.logging.Logger
import jenkins.model.Jenkins


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
String initKey = "dockerCloudConfigComplete"

// Only perform initialization if we haven't done it before.
if (envVars[initKey] != 'true')
{
   logger.info("Initial system startup; setting up Docker cloud")
   
   def dockerTemplateBaseParameters = [
      bindAllPorts:       false,
      bindPorts:          '',
      cpuShares:          null,
      dnsString:          '',
      dockerCommand:      '',
      environmentsString: '',
      extraHostsString:   '',
      hostname:           '',
      image:              ['lnvle1865.gs.myharris.net:5000/debian_10.1_jenkins_slave:3.2.0', 'lnvle1865.gs.myharris.net:5000/centos_7.7_jenkins_slave:3.2.0', 'lnvle1865.gs.myharris.net:5000/ubuntu_18.04_jenkins_slave:3.2.0'],
      macAddress:         '',
      memoryLimit:        null,
      memorySwap:         null,
      network:            '',
      privileged:         false,
      pullCredentialsId:  '',
      sharedMemorySize:   null,
      tty:                false,
      volumesFromString:  '',
      volumesString:      ''
   ]

   def DockerTemplateParameters = [
      instanceCapStr: '2147483647',
      labelString:    ['JENKINS_AGENT_DEBIAN_10.1 JENKINS_AGENT DEBIAN_AGENT', 'JENKINS_AGENT_CENTOS_7.7 CENTOS_AGENT', 'JENKINS_AGENT_UBUNTU_18.04 UBUNTU_AGENT'],
      name:           ['docker_debian', 'docker_centos', 'JENKINS_SLAVE_UBUNTU'],
      remoteFs:       '/home/jenkins'
   ]

   def dockerCloudParameters = [
      connectTimeout:   60,
      containerCapStr:  '100',
      credentialsId:    '',
      dockerHostname:   '',
      name:             'docker',
      readTimeout:      60,
      serverUrl:        'tcp://lnvle1864.gs.myharris.net:2021',
      version:          ''
   ]

   /* We need to get these classes by name at runtime because
      the plugin won't be installed the first time this script
      gets called and we don't want "unable to resolve class"
      messages from imports cluttering up the init logs. */
   def dockerTemplateBaseClazz = Class.forName('com.nirima.jenkins.plugins.docker.DockerTemplateBase')
   def dockerTemplateClazz = Class.forName('com.nirima.jenkins.plugins.docker.DockerTemplate')
   def dockerCloudClazz = Class.forName('com.nirima.jenkins.plugins.docker.DockerCloud')
   def dockerComputerAttachConnectorClazz = Class.forName('io.jenkins.docker.connector.DockerComputerAttachConnector')

   // Add the docker agent templates
   def Templates = []
   [DockerTemplateParameters.labelString, dockerTemplateBaseParameters.image, DockerTemplateParameters.name].transpose().each { templateLabel, templateImage, templateName ->
      def dockerTemplateBase = dockerTemplateBaseClazz.newInstance(
         templateImage,
         dockerTemplateBaseParameters.pullCredentialsId,
         dockerTemplateBaseParameters.dnsString,
         dockerTemplateBaseParameters.network,
         dockerTemplateBaseParameters.dockerCommand,
         dockerTemplateBaseParameters.volumesString,
         dockerTemplateBaseParameters.volumesFromString,
         dockerTemplateBaseParameters.environmentsString,
         dockerTemplateBaseParameters.hostname,
         dockerTemplateBaseParameters.memoryLimit,
         dockerTemplateBaseParameters.memorySwap,
         dockerTemplateBaseParameters.cpuShares,
         dockerTemplateBaseParameters.sharedMemorySize,
         dockerTemplateBaseParameters.bindPorts,
         dockerTemplateBaseParameters.bindAllPorts,
         dockerTemplateBaseParameters.privileged,
         dockerTemplateBaseParameters.tty,
         dockerTemplateBaseParameters.macAddress,
         dockerTemplateBaseParameters.extraHostsString
      )

      def dockerComputerAttachConnector = dockerComputerAttachConnectorClazz.newInstance("jenkins")

      def dockerTemplate = dockerTemplateClazz.newInstance(
         dockerTemplateBase,
         dockerComputerAttachConnector,
         templateLabel,
         DockerTemplateParameters.remoteFs,
         DockerTemplateParameters.instanceCapStr
      )

      dockerTemplate.setName(templateName)
      dockerTemplate.mode = Node.Mode.EXCLUSIVE
      dockerTemplate.pullTimeout = 300
      Templates.add(dockerTemplate)
   
      def dockerCloud = dockerCloudClazz.newInstance(
         dockerCloudParameters.name,
         Templates,
         dockerCloudParameters.serverUrl,
         dockerCloudParameters.containerCapStr,
         dockerCloudParameters.connectTimeout,
         dockerCloudParameters.readTimeout,
         dockerCloudParameters.credentialsId,
         dockerCloudParameters.version,
         dockerCloudParameters.dockerHostname
      )
   
      // add cloud configuration to Jenkins
      if (instance.clouds.size() == 0)
      {
         instance.clouds.add(dockerCloud)
      }
   }

   if (instance.clouds.size() > 0)
   {
      // Persist the new environment variable so we don't do the startup process again.
      envVars[initKey] = 'true'
      instance.save()
   }
   else
   {
      logger.warning("Docker cloud config did not complete successfully")
   }
}
else
{
   logger.info("Initial system startup was already completed; skipping.")
}