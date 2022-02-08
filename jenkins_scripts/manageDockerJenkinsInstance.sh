#!/bin/bash

PORT_SUF=80
CONFIG='false'
REMOTE_USER=
scriptPath=$(dirname "$(readlink -f "$0")")
usage() {
   echo "Usage: $0 -a <action> [-c] -n <container name> -p <container password> -s <slave port> -t <port prefix>"
   echo "   Note: this script MUST be run as the jenkins user"
   echo "   -a  the action to perform (one of create, delete, restart)"
   echo "   -c  on creation, automatically configure the instance"
   echo "   -h  this usage output"
   echo "   -n  the container name (for create, defaults to jenkins<port prefix>${PORT_SUF}"
   echo "   -p  optional password for the container"
   echo "   -r  remote host where scripts should be copied/where container runs (optional)"
   echo "   -s  slave port"
   echo "   -t  prefix for the container's web port (suffix ${PORT_SUF} will be appended)"
   echo "   -u  user for copying scripts to remote host (optional)"
   exit 1
}

while getopts "a:chn:p:r:s:t:u:" option; do
   case "${option}" in
      a )
         ACTION=${OPTARG^^}
         ;;
      c )
         CONFIG='true'
         ;;
      h )
         usage
         ;;
      n )
         DOCKER_HOST=${OPTARG}
         ;;
      p )
         SSH_CONTAINER_PASS=${OPTARG}
         ;;
      r )
         REMOTE_HOST=${OPTARG}
         ;;
      s )
         SLV_PRT=${OPTARG}
         ;;
      t )
         PORT_PRE=${OPTARG}
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

 
if [ -z "${ACTION}" ]
then
   echo "-a <action> is required"
   usage
fi

if [ "${ACTION}" != "RESTART" ] && [[ "$USER" != "jenkins" ]]
then
   echo "Action ${ACTION,,} requires running as the jenkins user."
   exit 1
fi

if [ "${ACTION}" == "CREATE" ]
then
   if [ -z "${PORT_PRE}" ]
   then
      echo "-t <port prefix> is required for ${ACTION,,} action"
      usage
   elif [ -z "${SLV_PRT}" ]
   then
      echo "-s <slave port> is required for ${ACTION,,} action"
      usage
   fi
fi

# Combine port prefix and suffix
WEB_PRT="${PORT_PRE}${PORT_SUF}"

if  [ -z "${DOCKER_HOST}" ]
then
   if [ "${ACTION}" != "CREATE" ]
   then
      echo "-n <container name> is required for ${ACTION,,} action"
      usage
   else
      DOCKER_HOST="jenkins${WEB_PRT}"
   fi
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
      if [ "$CONFIG" == "true" ]
      then
         echo "Copying ${scriptPath} to ${REMOTE_HOST}..."
         scp -r "$scriptPath" "${REMOTE_USER}${REMOTE_HOST}:~"
         echo "Copy complete, resuming script on remote host..."
         ssh -t "${REMOTE_USER}${REMOTE_HOST}" "~/${scriptDirName}/${scriptName}" "$@"
      else
         echo "Copying ${scriptPath}/${scriptName} to ${REMOTE_HOST}..."
         scp "${scriptPath}/${scriptName}" "${REMOTE_USER}${REMOTE_HOST}:~"
         echo "Copy complete, resuming script on remote host..."
         ssh -t "${REMOTE_USER}${REMOTE_HOST}" "~/${scriptName}" "$@"
      fi
      exit
   fi
fi


# Default password for containers
if [ -z "${SSH_CONTAINER_PASS}" ]
then
   SSH_CONTAINER_PASS='DevOps2020'
fi

dockerJenkinsPath="/home/jenkins/${DOCKER_HOST}_home"
# Parse each argument and run deployments
case "${ACTION}" in
   DELETE )
      echo "Removing Docker Container for ${DOCKER_HOST}"
      # remove ssh keys
      ssh-keygen -R ${DOCKER_HOST}
      # force remove docker containers
      docker stop ${DOCKER_HOST} > /dev/null;
      docker rm -f -v ${DOCKER_HOST} > /dev/null;
      rm -rf "${dockerJenkinsPath}"
      ;;
   CREATE )
      # Forward local host port to docker container
      FORWARD="-p ${WEB_PRT}:8080 -p ${SLV_PRT}:${SLV_PRT}"

      echo "Creating Docker Container for ${DOCKER_HOST} with forwarded ports ${FORWARD}"

      mkdir "${dockerJenkinsPath}"

      docker run -d ${FORWARD} \
      -v ${dockerJenkinsPath}:/var/jenkins_home \
      --restart always \
      --hostname ${DOCKER_HOST} \
      --name ${DOCKER_HOST} \
      -e TZ=America/New_York \
      --memory='1g' \
      --memory-swap='6g' \
      --memory-swappiness='0' \
      jenkins/jenkins:lts
      

      # Start container
      echo "Starting container ${DOCKER_HOST}...."
      docker start ${DOCKER_HOST}
      sleep 1

      # Update root password
      echo "Changing root password..."
      echo -e "${SSH_CONTAINER_PASS}\n${SSH_CONTAINER_PASS}" | docker exec -i ${DOCKER_HOST} passwd

      # Prepare for auto config
      if [ "$CONFIG" == "true" ]
      then
         touch "${scriptPath}/.jenkinsAdminPass"
         cat <<<"${SSH_CONTAINER_PASS}" > "${scriptPath}/.jenkinsAdminPass"
         "${scriptPath}/configJenkinsInstance.sh" -j "$dockerJenkinsPath" -n "${DOCKER_HOST}" -p "${WEB_PRT}" -s "${SLV_PRT}"
      fi
      
      ;;
   RESTART )
      # Restart container
      echo "Restarting container ${DOCKER_HOST}...."
      docker restart ${DOCKER_HOST}
      ;;
   \? )
      usage
      ;;
esac
