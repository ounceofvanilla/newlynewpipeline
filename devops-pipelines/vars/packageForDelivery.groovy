def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.prePackageForDelivery(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) { 
   // Save passed in pipelineParams map that will be manipulated locally.
   def localPipelineParams = pipelineParams.clone()
   // Set artifact management parameters to appropriate values for packaging, which may be different from whatever
   // the program needs in the main Jenkinsfile.
   localPipelineParams.ARTIFACT_MANAGEMENT_CHOICE = 'generic'
   localPipelineParams.ARTIFACT_RECURSIVE_SEARCH = 'false'
   localPipelineParams.FORMAL_VERSION = 'true'
   localPipelineParams.ARTIFACT_LOCATION = '.'
   localPipelineParams.ARTIFACT_ID = './'
   localPipelineParams.VERSION_NUMBER = './'
   // Read and parse YAML file specified by program. 
   packageData = parseYaml(readYaml(file: "${localPipelineParams.YAML_FILE}"))
   // Retrieve GLOBALS section from YAML file and override related pipelineParams
   if (packageData.get("GLOBALS")) {
      printDictionary(packageData.get("GLOBALS"))
      localPipelineParams.ARTIFACT_URL = packageData.get("GLOBALS").getOrDefault("base_artifactory_url", pipelineParams.ARTIFACT_MANAGEMENT_TOOL_URL)
      // Remove the GLOBALS key and all values so they are not processed in the main loop
      packageData.remove("GLOBALS")
   }

   // Iterate through all the remaining (non-GLOBALS) root level keys and process each as a separateRetrieve
   // package to be created and stored back in Artifactory.
   packageData.each{ key, value -> 
      logger.logInfo("Processing key -> ${key}")
      printDictionary(value)
      processPackage(value, localPipelineParams)
   }
}

// Process the keys to create each defined output archive in the YAML file
def processPackage(def packageEntry, pipelineParams) {
   // archiveName will be the output file name of the created package section being processed.
   // Additionally, the archiveName without the extension will serve as a working folder for 
   // storing the retrieved artifacts until time to create the archive (zip_name deprecated)
   def archiveName = packageEntry.get("zip_name")
   if (archiveName) {
      logger.logInfo("Note: deprecated 'zip_name' found, new key is 'archive_name'")
   }
   else
   {
      archiveName = packageEntry.get("archive_name")
      if (!archiveName) {
         logger.logError("Invalid YAML file, no archive_name or zip_name found")
      }
   }

   // destinationPath is the storage location in Artifactory where the generated archive will be stored.
   def destinationPath = variableExpansion(packageEntry.get("destination_path"))
   if (!destinationPath) {
      logger.logError("Invalid YAML file, no destination_path found")
   }
   def archiveFolderName = "${archiveName}" - ~/\.\w+$/
   dir (archiveFolderName) {
      // Iterate through the "structure" key contents to retrieve and save the required artifacts.
      packageEntry.get("structure").each{ key, value -> 
         processEntry(key, value, pipelineParams)
      }
   }

   // Create the archive that contains the contents for this package.
   def archiveType = packageEntry.getOrDefault("archive_type", "zip")
   logger.logInfo("Creating ${archiveType} package")
   switch(archiveType.toLowerCase()) {
      case 'tgz':
         sh """
            tar cfz "${archiveName}" "${archiveFolderName}"
         """
         break
      case 'tar':
         sh """
            tar cf "${archiveName}" "${archiveFolderName}"
         """
         break
      case 'zip':
         sh """
            zip -r -q "${archiveName}" "${archiveFolderName}"
         """
         break
      default:
         logger.logError(constants.ENVIRONMENT_ERROR + " an unknown archive_type was found: ${archiveType}, expected one of [zip, tar, tgz]")
         break
   }

   // Store created archive in Artifactory.
   pipelineParams.GROUP_ID = destinationPath
   pipelineParams.ARTIFACT_FILENAME_PATTERN = archiveName
   artifactManagement.publish(pipelineParams)
}

// Process the discrete content (folders or files) keys to create each folder/file defined in the "structure" key of the YAML file.
def processEntry(def entryName, def entryContents, pipelineParams) {
   // Verify that the entry has contents, otherwise, nothing to do.
   if ("${entryContents}") {
      // Attempt to get the "contents" sub-key for this entryName. 
      // If it doesn't exist, then this is a file to be retrieved and 
      // saved to be added to the archive.
      if (entryContents.containsKey("contents")) {
         // There is a "contents entry, and the YAML file format defined allows a program to specify an output
         // folder location that can be empty. Create and enter in the entryName folder, then try to iterate
         // through the "contents" sub-keys.
         def parsedEntryName = variableExpansion("${entryName}")
         dir ("${parsedEntryName}") {
            def subContent = entryContents.get("contents")
            if (subContent) {
               subContent.each{ key, value -> 
                  processEntry(key, value, pipelineParams)
               }
            } else {
               // Jenkins will "clean up" a directory created by a dir () block if nothing changed in the
               // directory from the time it was created until the dir() exits. This prevents the 
               // program from being able to specify an empty folder to include in the generated archive.
               // Create and delete an empty file to force Jenkins to leave the empty folder when the dir() block exits.
               sh 'touch empty.txt && rm empty.txt'
            }
         }
         // Jenkins creates a "folder@tmp" directory when processing a shell script. That will show up in the generated
         // archive unless we manually delete it.
         cleanWs deleteDirs: true, disableDeferredWipeout: true, patterns: [[pattern: "${parsedEntryName}?tmp", type: 'INCLUDE']]
       } else {
         processFile(entryName, entryContents, pipelineParams)
      }
   }
}

// Retrieve and process an individual file to be retrieved and stored for packaging.
def processFile(def fileName, def fileContents, pipelineParams) {
   // sourcePath is the absolute location in Artifactory (minus the server URL portion) to retrieve the artifact.
   def sourcePath = fileContents.get("source_path")
   // includeChecksum indicates if the Checksum artifacts should be included in packaging
   def includeChecksum = fileContents.getOrDefault("include_checksum", false)
   if (!sourcePath) {
      logger.logWarning("Invalid YAML file, no source_path found for file: ${fileName}")
      currentBuild.result = 'UNSTABLE'
      return
   }
   // Allow the program to retrieve an artifact (typically a compressed file), and 
   // store the contents of that artifact in a top level folder in the archive with a name
   // that is equal to the fileName.
   def destinationPath = fileContents.get("create_top_dir") ? fileName : "."
   // Retrieve the source base file name without any pathing information.
   def sourceBaseName =  "${sourcePath}".substring("${sourcePath}".lastIndexOf('/') + 1)
   def archiveType = fileContents.getOrDefault("archive_type", "none") 
   dir ("${destinationPath}") {
      pipelineParams.ARTIFACT_DESTINATION_PATH = sourcePath.replaceFirst(/\/${sourceBaseName}\/?$/, '')
      pipelineParams.ARTIFACT_DEPLOYMENT_SEARCH_PATTERN = sourceBaseName
      pipelineParams.ARTIFACT_DEPLOYMENT_SEARCH_REGEX_FLAG = 'false'
      pipelineParams.ARTIFACT_DEST_FOLDER = pwd()
      pipelineParams.ANSIBLE_REPO_TYPE = 'separate'
      pipelineParams.ANSIBLE_URL = 'https://lnsvr0329.gcsd.harris.com:8443/bitbucket/scm/devp/ansible.git'
      pipelineParams.ANSIBLE_BRANCH = 'master'
      pipelineParams.ANSIBLE_PLAYBOOK_NAME = 'generic'
      pipelineParams.ARTIFACT_DEPLOYMENT_TOOL = 'ansible'
      // Retrieve the artifact from Artifactory.
      artifactDeployment(pipelineParams)
      //Get the Checksum of the local file.
      def localMD5 = sh(script: "md5sum '${sourceBaseName}' | awk '{ print \$1 }'", returnStdout: true).trim()
      def localSHA1 = sh(script: "sha1sum '${sourceBaseName}' | awk '{ print \$1 }'", returnStdout: true).trim()
      def localSHA256 = sh(script: "sha256sum '${sourceBaseName}' | awk '{ print \$1 }'", returnStdout: true).trim()
      //Validate artifacts Checksum if specified
      if (pipelineParams.ARTIFACT_PACKAGE_VALIDATE_CHECKSUM == 'true'){
         //Retrieve the Checksum from artifact management using curl
         def curlResponse = utilMethods.curl(credentialsId: pipelineParams.ARTIFACT_CREDENTIALS,
                        url: "${pipelineParams.ARTIFACT_URL}/${sourcePath}",
                        additionalParams: "-I",
                        returnStdout: true)
         //Parse response and find only the lines with Checksum values
         List checksumLines = curlResponse.split( '\n' ).findAll { it.startsWith( 'X-Checksum' ) }       
         def checksumMap = [:]
         //For each checksum line create a map of key values
         for (line in checksumLines){
            def checksumPair = line.split(': ')
            checksumMap."${checksumPair[0].trim()}" = "${checksumPair[1].trim()}"
         }
         // Log local Checksum files
         logger.logInfo("""
         Downloaded File Checksum: 
         md5sum = ${localMD5}
         sha1sum = ${localSHA1}
         sha256sum = ${localSHA256}
         Remote File Checksum:
         md5sum = ${checksumMap["X-Checksum-Md5"]}
         sha1sum = ${checksumMap["X-Checksum-Sha1"]}
         sha256sum = ${checksumMap["X-Checksum-Sha256"]}
         """)
         //Compares and validates Checksum of artifact management file with the local checksum 
         if (checksumMap["X-Checksum-Md5"] == localMD5 && checksumMap["X-Checksum-Sha1"] == localSHA1 && checksumMap["X-Checksum-Sha256"] == localSHA256){
            logger.logInfo("Artifact checksum has been validated")
         }
         else{
            logger.logError("Artifact checksum validation failed")
         }
      }
      // Process retrieved artifact if it is compressed and needs to be unpacked.
      switch(archiveType.toLowerCase()) {
         case 'tgz':
            logger.logInfo("TGZ archive_type is ${archiveType}")
            sh """
              tar xfz "${sourceBaseName}"
              rm -f "${sourceBaseName}"
            """
            break
         case 'tar':
            logger.logInfo("TAR archive_type is ${archiveType}")
            sh """
              tar xf "${sourceBaseName}"
              rm -f "${sourceBaseName}"
            """
            break
         case 'zip':
            logger.logInfo("ZIP archive_type is ${archiveType}")
            unzip(quiet: true, zipFile: "${sourceBaseName}")
            sh """
              rm -f "${sourceBaseName}"
            """
            break
         case 'none':
            logger.logInfo("File is not compressed")
            if (fileName != sourceBaseName) {
               logger.logWarning("Renaming file: ${sourceBaseName} to ${fileName} based on YAML file contents")
               sh """
                  mv ${sourceBaseName} ${fileName}
               """
            }
            break
         default:
            logger.logError(constants.ENVIRONMENT_ERROR + " an unknown archive_type was found: ${archiveType}")
            break
      }
      // Add Checksum files to the archive if specified in the YAML
      if(includeChecksum == true){
         sh """
            echo ${localMD5} > ${sourceBaseName}_checksum.md5
            echo ${localSHA1} > ${sourceBaseName}_checksum.sha1
            echo ${localSHA256} > ${sourceBaseName}_checksum.sha256
         """
      }
   }
   // Jenkins creates a "folder@tmp" directory when processing a shell script. That will show up in the generated
   // archive unless we manually delete it.
   if (destinationPath != '.') {
      cleanWs deleteDirs: true, disableDeferredWipeout: true, patterns: [[pattern: "${destinationPath}?tmp", type: 'INCLUDE']]
   }

}

// Parse the entire YAML file and perofrm variable expansion for all non-map entries
// Changing the key of a map will cause issues. For entries in the map that will create
// folder structure, the folder name will call variableExpansion at the point of use.
def parseYaml(def root, def depth=0) {
   // Evaluate the current root key passed in to the method, and if it is itself a map, call this method recursively.
   root.each{ key, value -> 
      if (value instanceof Map) {
         parseYaml(value, depth+1)
      } else {
         root[key] = variableExpansion(value)
      }
   }
}

// This method will replace all ${SOME_VARIABLE_NAME} values with the corresponding
// value from the environment variable SOME_ENVIRONMENT_VARIABLE. It will replace multiple
// environment variable blocks per string supplied, and if the corresponding environment variable is 
// not defined, will simply strip the ${} from ${SOME_VARIABLE_NAME}, allowing for a program
// to use an environment variable, or a default equal to the environment variable name. 
@NonCPS
def variableExpansion (inputString) {
   // Find all strings matching ${SOME_VARIABLE_NAME} and replace with the value from the same named environment variable
   // If no matching environment variable, strip the ${} and leave the environment variable name
   def expandedString = inputString
   def matches = expandedString =~ /\$\{([^}]+)\}/
   matches.each { match, envVariable ->
      expandedString = expandedString.replaceAll("\\\$[\\{]([" + envVariable + "^}]*)[\\}]", (env."$envVariable" ?: "$envVariable"))
   }
   return expandedString
}

// This method will print the yaml structure starting with the key passed.
// The method will call itself recursively to iterate over each key contained in the root key and its children
// and automatically indent children under their parents to show relationships.
def printDictionary(def root, depth=0) {
   root.each{ key, value -> 
      if (value instanceof Map) {
         println ("  "*depth + key)
         printDictionary(value, depth+1)
      } else {
         println ("  "*depth + "${key} : ${value}")
      }
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the packageForDelivery stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postPackageForDelivery(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}