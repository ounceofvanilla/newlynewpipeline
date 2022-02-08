#!/bin/bash

scriptPath=$(dirname "$(readlink -f "$0")")
jenkinsHome="/var/jenkins_home"
adminUser="devops"
adminPwFile="${scriptPath}/.jenkinsAdminPass"
adminEmail="DoNotReply@L3Harris.com"
artifactoryUser="svc-devfac"
artifactoryPwFile="${scriptPath}/.artifactoryPass"
jenkinsHost=$(hostname -f 2> /dev/null || hostname)
jenkinsPort=
dockerContainerName=""
slavePort=
REMOTE_USER=

usage() {
   echo "Usage: $0  [-e <adminEmail>] [-j <jenkinsHome>] [-n <dockerContainerName>] [-o <jenkinsHost>] -p <jenkinsPort> [-s <slavePort] [-a <jenkinsAdminUser>]"
   echo "   Note: this script MUST be run as the Jenkins user"
   echo "   -a  the Jenkins admin username (default: ${adminUser})"
   echo "   -e  email address to add for Jenkins admin account (default: ${adminEmail})"
   echo "   -h  print this usage info"
   echo "   -j  the path to the Jenkins home directory (default: ${jenkinsHome})"
   echo "   -n  the name of the docker container, if applicable"
   echo "   -o  the hostname for the Jenkins instance (default: ${jenkinsHost})"
   echo "   -p  the host port for the Jenkins instance"
   echo "   -r  remote host where scripts should be copied/where jenkins runs (optional)"
   echo "   -s  the JNLP slave port to configure in Jenkins"
   echo "   -u  user for copying scripts to remote host (optional)"
   exit 1
}

waitForFile() {
   filePath="$1"
   delay="${2}"
   message="${3:-Waiting for ${filePath} to exist.}"
   usageString="Usage: waitForFile <filePath> <delay> [message]"

   if [ "$filePath" == "" ]
   then
      echo "filePath is required"
      echo "$usageString"
      exit 1
   fi

   if [[ ! $delay =~ ^[0-9]+[hms]?$ ]]
   then
      echo "delay is required and must be in the format [0-9]+[hms]? (default suffix is s for seconds)"
      echo "$usageString"
      exit 1
   fi

   delaySuffix="${delay: -1}"
   case "$delaySuffix" in
   h )
      hours="${delay::${#delay}-1}"
      calculatedSecs=$((${hours} * 60 * 60))
      ;;
   m )
      minutes="${delay::${#delay}-1}"
      calculatedSecs=$((${minutes} * 60))
      ;;
   s )
      calculatedSecs="${delay::${#delay}-1}"
      ;;
   * )
      # Should just be a digit, so this is raw seconds
      calculatedSecs="$delay"
      ;;
   esac

   echo -n "$message"
   now=$(date +%s)
   expiryTime=$(($now + $calculatedSecs))
   while [ ! -e "$filePath" ] && [[ $now -lt $expiryTime ]]
   do
      echo -n '.'
      sleep 3
      now=$(date +%s)
   done
   echo
   if [ -e "$filePath" ]
   then
      echo "Found '${filePath}'"
   else
      echo "Timed out waiting for '${filePath}' to exist"
   fi
}

while getopts "a:e:hj:n:o:p:r:s:u:" option; do
   case "${option}" in
      a )
         adminUser="${OPTARG}"
         ;;
      e )
         adminEmail="${OPTARG}"
         ;;
      h )
         usage
         ;;
      j )
         jenkinsHome="${OPTARG}"
         ;;
      n )
         dockerContainerName="${OPTARG}"
         ;;
      o )
         jenkinsHost="${OPTARG}"
         ;;
      p )
         jenkinsPort=":${OPTARG}"
         ;;
      r )
         REMOTE_HOST=${OPTARG}
         ;;
      s )
         slavePort="${OPTARG}"
         ;;
      u )
         REMOTE_USER="${OPTARG}@"
         ;;
      * )
         usage
         ;;
      : )
         usage
         ;;
    esac
done

if [ -z "${jenkinsPort}" ]
then
   echo "Host port '-p' must be defined."
   usage
fi

# Check if REMOTE_HOST is set and remote
if [ "$REMOTE_HOST" != "" ];
then
   localHostname=$(hostname)
   remoteHostname=$(ssh "${REMOTE_USER}${REMOTE_HOST}" hostname)
   if [ "$remoteHostname" != "" ] && [ "$localHostname" != "$remoteHostname" ]
   then
      scriptDirName=$(basename "$scriptPath")
      scriptName=$(basename "$0")
      echo "Copying ${scriptPath} to ${REMOTE_HOST}..."
      scp -r "$scriptPath" "${REMOTE_USER}${REMOTE_HOST}:~"
      echo "Copy complete, resuming script on remote host..."
      ssh -t "${REMOTE_USER}${REMOTE_HOST}" "~/${scriptDirName}/${scriptName}" "$@"
      exit
   fi
fi

secretsPath="${jenkinsHome}/secrets"
waitForFile "$secretsPath" 1m

if [ -d "$secretsPath" ]
then
   # Create temporary secret admin credentials files
   echo "$adminUser" > "${secretsPath}/adminUser"
   if [ -f "$adminPwFile" ]
   then
      cp "$adminPwFile" "${secretsPath}/adminPassword"
   else
      read -p "Please enter password for Jenkins admin user '$adminUser': " -s adminPass
      echo
      cat <<<"${adminPass}" > "${secretsPath}/adminPassword"
   fi

   # Create temporary secret Artifactory service account files
   echo "$artifactoryUser" > "${secretsPath}/svcUser"
   if [ -f "$artifactoryPwFile" ]
   then
      cp "$artifactoryPwFile" "${secretsPath}/svcApiKey"
   else
      read -p "Please enter password for Artifactory user '$artifactoryUser': " -s artifactoryPass
      echo
      cat <<<"${artifactoryPass}" > "${secretsPath}/svcApiKey"
   fi
fi

# Populate temporary initConfig.properties file
echo > "${jenkinsHome}/initConfig.properties"
if [ "$slavePort" != "" ]
then
   echo "slaveAgentPort=${slavePort}" > "${jenkinsHome}/initConfig.properties"
fi

echo "rootUrl=http://${jenkinsHost,,}${jenkinsPort}/" >> "${jenkinsHome}/initConfig.properties"
echo "adminEmail=${adminEmail}" >> "${jenkinsHome}/initConfig.properties"

# Copy the init directory to jenkins_home
# Note: assumes init.groovy.d is in the same directory as this script (as it should be in the repo)
cp -R "${scriptPath}/init.groovy.d" "${jenkinsHome}"

# Restart the instance so Jenkins kicks off the init
if [ "$dockerContainerName" == "" ]
then
   service jenkins restart
else
   docker restart "${dockerContainerName}"
fi

# Monitor for initComplete
initCompleteFile="${jenkinsHome}/initComplete"
message="Please wait while Jenkins initialization completes; this may take a while."
waitForFile "$initCompleteFile" 10m "$message"

if [ ! -f "${jenkinsHome}/initComplete" ]
then
   echo "Initialization did not complete; log into Jenkins and check logs for issues"
   exit 1
fi

rm -f "${jenkinsHome}/initComplete"
echo "Initialization complete; you may now log in"