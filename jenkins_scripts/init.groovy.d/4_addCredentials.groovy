#!/usr/bin/env groovy

import jenkins.model.Jenkins
import java.util.logging.Logger
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.cloudbees.plugins.credentials.CredentialsScope


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
String initKey = "addCredentialsComplete"

String secretsPath = "/var/jenkins_home/secrets"
instance = Jenkins.instance
domain = Domain.global()
store = instance.getExtensionList(
  "com.cloudbees.plugins.credentials.SystemCredentialsProvider")[0].getStore()

if (envVars[initKey] != 'true')
{
   logger.info("Initial system startup; adding credentials")

   def adminUserFile = new File("${secretsPath}/adminUser")
   def svcUserFile = new File("${secretsPath}/svcUser")
   def bitbucketUserFile = new File("${secretsPath}/bitbucketUser")

   def adminPasswordFile = new File("${secretsPath}/adminPassword")
   def svcApiKeyFile = new File("${secretsPath}/svcApiKey")
   def bitbucketPasswordFile = new File("${secretsPath}/bitbucketKey")

   jenkinsCredentials = new UsernamePasswordCredentialsImpl(
     CredentialsScope.GLOBAL,
     "JENKINS_CREDENTIALS",
     "Jenkins credentials for REST calls",
     adminUserFile.text.trim(),
     adminPasswordFile.text.trim()
   )

   artifactCredentials = new UsernamePasswordCredentialsImpl(
     CredentialsScope.GLOBAL,
     "ARTIFACT_CREDENTIALS",
     "Credentials for artifact management (Artifactory)",
     svcUserFile.text.trim(),
     svcApiKeyFile.text.trim()
   )

   bitbucketCredentials = new UsernamePasswordCredentialsImpl(
     CredentialsScope.GLOBAL,
     "BITBUCKET_CREDENTIALS",
     "Credentials for Bitbucket operations",
     bitbucketUserFile.text.trim(),
     bitbucketPasswordFile.text.trim()
   )

   store.addCredentials(domain, jenkinsCredentials)
   store.addCredentials(domain, artifactCredentials)
   store.addCredentials(domain, bitbucketCredentials)

   //Cleanup
   adminUserFile.delete()
   adminPasswordFile.delete()
   svcUserFile.delete()
   svcApiKeyFile.delete()
   bitbucketUserFile.delete()
   bitbucketPasswordFile.delete()

   envVars[initKey] = 'true'
   instance.save()
}
else
{ 
   logger.info("Credentials already added; skipping.")
}