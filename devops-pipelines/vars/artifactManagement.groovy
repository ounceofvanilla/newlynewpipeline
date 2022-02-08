def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preArtifactManagement(pipelineParams)
}

def getVersionNumber(Map pipelineParams, def informalVersionPrefix='-') {
   def versionNumber = "${pipelineParams.VERSION_NUMBER}"
   if (pipelineParams.FORMAL_VERSION == 'false') {
         versionNumber += "${informalVersionPrefix}${pipelineParams.BUILD_NUMBER}"
   }
   return versionNumber
}

// Get the artifact path portion of the URL to be tacked onto the base URL
def getArtifactUrlPath(Map pipelineParams) {
   def versionNumber = getVersionNumber(pipelineParams)
   def defaultDestinationPath = "${pipelineParams.GROUP_ID}/${pipelineParams.ARTIFACT_ID}/${versionNumber}"
   return pipelineParams.ARTIFACT_DESTINATION_PATH ?: defaultDestinationPath
}

def getArtifactFilePaths(Map pipelineParams) {
   def findResults
   def sourceArtifactArray = []
   def location = pipelineParams.ARTIFACT_LOCATION
   def pattern = (pipelineParams.ARTIFACT_REGEX_PATTERN_FLAG == "true") ? "*" : "${pipelineParams.ARTIFACT_FILENAME_PATTERN}"
   
   // If using a regex pattern we will do the find commands using * to gather all the files then perform regex operation after. 
   // Find commands to get the artifacts
   if (isUnix()) {
      def recursiveParameter = pipelineParams.ARTIFACT_RECURSIVE_SEARCH == 'true' ? "" : "-maxdepth 1"
      findResults = sh(script: """
                        find "\$(readlink -f '${location}')" ${recursiveParameter} -type f -name '${pattern}'
                     """, 
                     returnStdout: true)
   } else {
      def recursiveParameter = pipelineParams.ARTIFACT_RECURSIVE_SEARCH == 'true' ? "/R" : ""
      dir (location) {
         findResults = bat(script: """
                              @echo off
                              FOR ${recursiveParameter} %%f IN (${pattern}) do (echo "%%~dpnxf")
                           """, 
                           returnStdout: true)
      }
   }
   
   // Clean up results for processing
   findResults = findResults.trim().replaceAll(/["']/, '')
   sourceArtifactArray = findResults.split("\n")
   
   // Operation to apply regex if the pattern was of regex type.
   if (pipelineParams.ARTIFACT_REGEX_PATTERN_FLAG == "true") {
      // Apply the pattern to filter on matches
      def searchPattern = ~pipelineParams.ARTIFACT_FILENAME_PATTERN
      sourceArtifactArray = sourceArtifactArray.findAll { searchPattern.matcher(it).matches() }
   }
   
   if (!sourceArtifactArray) {
       logger.logError("No artifacts found matching pattern ${pipelineParams.ARTIFACT_FILENAME_PATTERN}")
   }
   
   return sourceArtifactArray
}

def publish(Map pipelineParams) { 
   // Executing different blocks of code depending on the artifact management tool being used.
   switch(pipelineParams.ARTIFACT_MANAGEMENT_CHOICE) {
      case 'generic':
         //Retrieves an array of the artifacts found based on the pattern.
         def sourceArtifactArray = getArtifactFilePaths(pipelineParams)
         def artifactUrl = "${pipelineParams.ARTIFACT_URL}"

         //This will add properties to tag an artifact for traceability
         if(pipelineParams.APPLY_OPTIONAL_ARTIFACT_MANAGEMENT_PROPERTIES == "true"){
            def encodedGitBranch = GIT_BRANCH.replaceAll('/', '%2F')
            def encodedDisplayUrl = RUN_DISPLAY_URL.replaceAll('/', '%2F')
            artifactUrl += ";GIT_BRANCH=${encodedGitBranch};BUILD_NUMBER=${BUILD_NUMBER};BUILD_URL=${encodedDisplayUrl}"
         }
         
         artifactUrl += "/${getArtifactUrlPath(pipelineParams)}"
            
         // Process each file found and store in Artifactory.
         sourceArtifactArray.each { localFilePath ->
            // Calculate checksum header if necessary
            def checksumMap = [:]
            if (pipelineParams.ARTIFACT_GENERATE_CHECKSUM == 'true') {
               if (isUnix()) {
                  checksumMap["X-Checksum-Md5"] = sh(script: "md5sum '${localFilePath}' | awk '{ print \$1 }'", returnStdout: true)
                  checksumMap["X-Checksum-Sha1"] = sh(script: "sha1sum '${localFilePath}' | awk '{ print \$1 }'", returnStdout: true)
                  checksumMap["X-Checksum-Sha256"] = sh(script: "sha256sum '${localFilePath}' | awk '{ print \$1 }'", returnStdout: true)
               } else {
                  checksumMap["X-Checksum-Md5"] = bat(script: """
                     @echo off
                     certutil -hashfile "${localFilePath}" md5 | find /i /v "hash"
                  """, returnStdout: true).trim()
                  checksumMap["X-Checksum-Sha1"] = bat(script: """
                     @echo off
                     certutil -hashfile "${localFilePath}" sha1 | find /i /v "hash"
                  """, returnStdout: true).trim()
                  checksumMap["X-Checksum-Sha256"] = bat(script: """
                     @echo off
                     certutil -hashfile "${localFilePath}" sha256 | find /i /v "hash"
                  """, returnStdout: true).trim()
               }
            }
            // Build the header list (will be empty if checksum map is empty)
            def headerList = checksumMap.collect { "${it.key}: ${it.value}" }

            // See if we need to append the local directory structure
            def fullArtifactUrl = artifactUrl
            if (pipelineParams.ARTIFACT_REPLICATE_LOCAL_PATH_FLAG == "true") {
               // First trim off workspace from the front of the path
               def relativeFilePath = localFilePath - WORKSPACE
               // Make sure to convert forward slashes for possible Windows paths
               relativeFilePath = relativeFilePath.replaceAll("\\\\", "/")
               // Now just get the directory
               def relativeBaseDir = relativeFilePath.substring(0, relativeFilePath.lastIndexOf('/'))
               // As long as the basename of the localFilePath isn't in the root workspace, we can tack it onto the artifactUrl
               if (relativeBaseDir != relativeFilePath) {
                  fullArtifactUrl = "${artifactUrl}/${relativeBaseDir}".replaceAll(/([^:])\/"?\//, '$1/')
               }
            }
                    
            // Upload using curl
            utilMethods.curl(credentialsId: pipelineParams.ARTIFACT_CREDENTIALS,
                             uploadFile: localFilePath.trim(),
                             url: "${fullArtifactUrl}/",
                             headerList: headerList,
                             errorPatternList: ["\"errors\" :", "curl: -?[1-9]", "Can't open"])
         }
         break
      case 'nuget':
         if (isUnix()) {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": NuGet, Linux")
         }
         // This will remove the source before we try to add it again in case the source information changed.
         // If the source was not added, this will error out. Therefore we want to ignore errors.
         bat "nuget sources remove -Name \"${pipelineParams.NUGET_SOURCE_NAME}\" -Source \"${pipelineParams.ARTIFACT_URL}\" || exit 0"
         withCredentials([usernamePassword(credentialsId: "${pipelineParams.ARTIFACT_CREDENTIALS}", passwordVariable: 'ARTIFACT_REPO_PASSWORD', usernameVariable: 'ARTIFACT_REPO_USERNAME')]) {
            bat """
               rem This will add the source with the necessary information provided by the user.
               nuget sources add -Name "${pipelineParams.NUGET_SOURCE_NAME}" -Source "${pipelineParams.ARTIFACT_URL}" -UserName """ + '%ARTIFACT_REPO_USERNAME% -Password %ARTIFACT_REPO_PASSWORD%' + """
               nuget setapikey """ + '%ARTIFACT_REPO_USERNAME%:%ARTIFACT_REPO_PASSWORD%' + """ -Source "${pipelineParams.ARTIFACT_URL}"
               rem This will create the distribution package using Nuget.
               nuget pack "${pipelineParams.NUSPEC_FILE}" -Version ${getVersionNumber(pipelineParams)}
               rem This will push the distribution package to Artifact.
               nuget push -source "${pipelineParams.NUGET_SOURCE_NAME}" *.nupkg
            """
         }
         // Clean up the source so a stale copy isn't hanging around
         bat "nuget sources remove -Name \"${pipelineParams.NUGET_SOURCE_NAME}\" -Source \"${pipelineParams.ARTIFACT_URL}\" || exit 0"
         break
      case 'maven':
         // This will create the distibution package using Maven.
         maven_package_command="mvn package -gs settings.xml"
         // This will push the distribution package to the Artifact Manager Repo using Maven.
         maven_deploy_command="mvn deploy -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -gs settings.xml"
         withCredentials([usernamePassword(credentialsId: "${pipelineParams.ARTIFACT_CREDENTIALS}", passwordVariable: 'ARTIFACT_REPO_PASSWORD', usernameVariable: 'ARTIFACT_REPO_USERNAME')]) {
            if (isUnix()) {
               sh "${maven_package_command}"
               sh "${maven_deploy_command} " + '-DARTIFACT_REPO_USERNAME=${ARTIFACT_REPO_USERNAME} -DARTIFACT_REPO_PASSWORD=${ARTIFACT_REPO_PASSWORD}'
            }
            else {
               bat "${maven_package_command}"
               bat "${maven_deploy_command} " + '-DARTIFACT_REPO_USERNAME=%ARTIFACT_REPO_USERNAME% -DARTIFACT_REPO_PASSWORD=%ARTIFACT_REPO_PASSWORD%'
            }
         }
         break
      case 'pypi':
         // Write the config file for bumpversion
         writeFile file: '.bumpversion.cfg', text: '''[bumpversion]
            current_version = 0.0.0
            parse = (?P<major>\\d+)\\.(?P<minor>\\d+)\\.(?P<patch>\\d+)(\\.(?P<release>[a-z]+)(?P<build>\\d+))?
            serialize =
               {major}.{minor}.{patch}.{release}{build}
               {major}.{minor}.{patch}'''

            // Bump the major version with what the user specified. Must be done before the package is created.
            def bump_version = "bumpversion --current-version 0.0.0 --new-version ${getVersionNumber(pipelineParams, '.a')} --allow-dirty --list major setup.py"
            // This will create the python distribution pacakge in a wheel format.
            def create_artifact_command = "setup.py sdist bdist_wheel"
            // This will push the artifact to the Artifact Manager using Twine
            def push_artifact_command = "twine upload --repository-url \"${pipelineParams.ARTIFACT_URL}\""
            withCredentials([usernamePassword(credentialsId: "${pipelineParams.ARTIFACT_CREDENTIALS}", passwordVariable: 'ARTIFACT_REPO_PASSWORD', usernameVariable: 'ARTIFACT_REPO_USERNAME')]) {
               // The same command is run on both Linux and Windows
               if (isUnix()) {
                  sh """
                     ${bump_version}
                     python3 ${create_artifact_command}
                     ${push_artifact_command} """ + '--username ${ARTIFACT_REPO_USERNAME} --password ${ARTIFACT_REPO_PASSWORD} dist/*'
               }
               else {
                  bat """
                     ${bump_version}
                     python ${create_artifact_command}
                     ${push_artifact_command} """ + '--username %ARTIFACT_REPO_USERNAME% --password %ARTIFACT_REPO_PASSWORD% dist/*'
               }
            }
         break
      case 'debian':
         if (isUnix())
         {
            //Creates the Artifact Destination URL
            def curlOutputFile = "curl_output.txt"
            def curlErrorString = "error"
            def curlAllErrorsString = "curl: ([0-9]*)"
            def curlFileNotFoundString = "Can't open"
            def defaultDestinationPath = "${pipelineParams.GROUP_ID}/${pipelineParams.ARTIFACT_ID}/${getVersionNumber(pipelineParams)}"
            def artifactUrl = "${pipelineParams.ARTIFACT_URL}/${pipelineParams.ARTIFACT_DESTINATION_PATH ?: defaultDestinationPath}"
            artifactUrl += "/${pipelineParams.ARTIFACT_ID}.deb"

            //This will add properties to tag an artifact for traceability
            if(pipelineParams.APPLY_OPTIONAL_ARTIFACT_MANAGEMENT_PROPERTIES == "true"){
               def encodedGitBranch = GIT_BRANCH.replaceAll('/', '%2F')
               def encodedDisplayUrl = RUN_DISPLAY_URL.replaceAll('/', '%2F')
               artifactUrl += ";GIT_BRANCH=${encodedGitBranch};BUILD_NUMBER=${BUILD_NUMBER};BUILD_URL=${encodedDisplayUrl}"
            }

            //Uploads debian package into artifactory
            utilMethods.curl(credentialsId: pipelineParams.ARTIFACT_CREDENTIALS,
                           requestMethod: 'PUT',
                           url: artifactUrl,
                           uploadFile: "${pipelineParams.ARTIFACT_ID}.deb",
                           errorPatternList: ["\"errors\" :", "curl: -?[1-9]", "Can't open"])
         }
         else {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": debian, Windows")
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.ARTIFACT_MANAGEMENT_TOOL, this.getClass().getSimpleName())
         break
   }
}

def doMainStageProcessing(Map pipelineParams) {
   dir("${pipelineParams.PROJECT_ROOT}") {
      publish(pipelineParams)
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   logger.logInfo("Analysis is not applicable for the artifact management stage.")
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postArtifactManagement(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}