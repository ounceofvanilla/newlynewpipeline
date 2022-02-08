#!/bin/bash

scriptPath=$(dirname "$(readlink -f "$0")")
secretsPath="/var/jenkins_home/secrets"
jenkinsHome="/var/jenkins_home"
adminUser="devops"
adminPwFile="${scriptPath}/.jenkinsAdminPass"
adminEmail="DoNotReply@L3Harris.com"
artifactoryUser="svc-devfac"
artifactoryPwFile="${scriptPath}/.artifactoryPass"
bitbucketUser="svc-devfac"
bitbucketPwFile="${scriptPath}/.bitbucketPass"
CRUMB=""
slavePort="58000"

#Spin up Jenkins instance on a Kubernetes pod 
kubectl create namespace jenkins || true; #Create namespace if not present
kubectl apply -f ${scriptPath}/kubernetes_files/jenkins-sa.yaml -n jenkins
kubectl create -f ${scriptPath}/kubernetes_files/jenkins-deployment.yaml -n jenkins;
kubectl create -f ${scriptPath}/kubernetes_files/jenkins-service.yaml -n jenkins;

#Get Jenkins instance URL and pod name 
jsonpath="{.spec.ports[0].nodePort}"
jenkinsPort=$(kubectl get -n jenkins -o jsonpath=$jsonpath services jenkins)
jsonpath="{.items[0].status.addresses[0].address}";
NODE_IP=$(kubectl get nodes -n jenkins -o jsonpath=$jsonpath);
echo http://$NODE_IP:$jenkinsPort > ./jenkins_URL;
podName=$(kubectl get pods -n jenkins -o jsonpath='{.items[?(@.metadata.labels.app=="jenkins")].metadata.name}'); 
echo $podName;

#Wait for pod to spin up
while [[ $(kubectl -n jenkins get pods -l app=jenkins -o 'jsonpath={..status.conditions[?(@.type=="Ready")].status}') != "True" ]]; do echo "waiting for pod" && sleep 1; done
kubectl exec --stdin  $podName -n jenkins -- /bin/bash -c "
   while [ ! -e "${secretsPath}" ]
   do
      echo -n '.'
      sleep 3
   done
   #Populate temporary initConfig.properties file
   echo > "${jenkinsHome}/initConfig.properties"
   echo "slaveAgentPort=${slavePort}" > "${jenkinsHome}/initConfig.properties"
   echo "rootUrl=http://${NODE_IP}:${jenkinsPort}/" >> "${jenkinsHome}/initConfig.properties"
   echo "adminEmail=${adminEmail}" >> "${jenkinsHome}/initConfig.properties" "

#Transfer files into pod 
kubectl cp "${scriptPath}" jenkins/$podName:"${jenkinsHome}";
kubectl cp "${scriptPath}/init.groovy.d" jenkins/$podName:"${jenkinsHome}";
echo "$adminUser" > "./adminUser";
echo "$artifactoryUser" > "./artifactoryUser";
echo "$bitbucketUser" > "./bitbucketUser";
kubectl cp "./adminUser" jenkins/$podName:"${secretsPath}/adminUser";
kubectl cp "./artifactoryUser" jenkins/$podName:"${secretsPath}/svcUser";
kubectl cp "./bitbucketUser" jenkins/$podName:"${secretsPath}/bitbucketUser";
kubectl cp "./jenkins_URL" jenkins/$podName:"${jenkinsHome}/jenkins_URL"

#Get passwords if not in a file
if !  [ -f "$adminPwFile" ]
then
   read -p "Please enter password for Jenkins admin user '$adminUser': " -s adminPass
   echo $adminPass > $adminPwFile
   echo
fi

if ! [ -f "$artifactoryPwFile" ]
then
   read -p "Please enter password for Artifactory user '$artifactoryUser': " -s artifactoryPass
   echo $artifactoryPass > $artifactoryPwFile
   echo
fi

if ! [ -f "$bitbucketPwFile" ]
then
   read -p "Please enter password for Bitbucket user '$bitbucketUser': " -s bitbucketPass
   echo $bitbucketPass > $bitbucketPwFile
   echo
fi

kubectl cp "${adminPwFile}" jenkins/$podName:"${secretsPath}/adminPassword";
kubectl cp "${artifactoryPwFile}" jenkins/$podName:"${secretsPath}/svcApiKey";
kubectl cp "${bitbucketPwFile}" jenkins/$podName:"${secretsPath}/bitbucketKey";

#Reset pod to update changes
kubectl exec $podName -n jenkins -c jenkins -- /sbin/killall5
sleep 5

#Wait for initialization to finish
while [[ $(kubectl -n jenkins get pods -l app=jenkins -o 'jsonpath={..status.conditions[?(@.type=="Ready")].status}') != "True" ]]; do echo "waiting for pod to restart" && sleep 10; done
kubectl exec --stdin  $podName -n jenkins -- /bin/bash -c "
   while [ ! -e "${jenkinsHome}/initComplete" ]
   do
      echo 'waiting for jenkins to initialize'
      sleep 20
   done "

echo Jenkins URL: http://$NODE_IP:$jenkinsPort;
adminPass=$(<${adminPwFile})

#Get Jenkins crumb to start 'make' job
echo 'Waiting for crumb'

while [[ $CRUMB != Jenkins-Crumb* ]] 
do 
   CRUMB=$(curl -s --user $adminUser:$adminPass http://${NODE_IP}:${jenkinsPort}/crumbIssuer/api/xml?xpath=concat\(//crumbRequestField,%22:%22,//crumb\))
done

#To use the MINIKUBE_AGENT label: Set the OVERRIDE_JENKINS_AGENT parameter to 'MINIKUBE_AGENT', or change pipelineParams.JENKINS_AGENT in the agent section of vars/runPipeline.groovy to 'MINIKUBE_AGENT' 
curl "http://${NODE_IP}:${jenkinsPort}/job/DOCPF_Sample_Jobs/job/make/buildWithParameters?token=token&PIPELINE_VERSION=release%2F4.0.0&GIT_BRANCH=release%2F4.0.0" --user ${adminUser}:${adminPass} -H "$CRUMB"

echo Starting 'make' job

#Wait for 'make' job to finish
kubectl exec --stdin  $podName -n jenkins -- /bin/bash -c "
   while [ ! -e '${jenkinsHome}/jobs/DOCPF_Sample_Jobs/jobs/make/htmlreports' ]
   do
      echo 'waiting for 'make' job to finish'
      sleep 20
   done "

#Collect results
echo 'Make' job finished
kubectl cp jenkins/$podName:"${jenkinsHome}/jobs/DOCPF_Sample_Jobs/jobs/make/builds/1/log" "${scriptPath}/makeLog"

#Tear down environment
kubectl delete namespace jenkins