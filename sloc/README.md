DOCPF SLOC Repository
This repository contains a Jenkins file to be used for generating SLOC metrics for a given repository using the LOCC tool.

Getting Started
These instructions will explain how to stand up the sample Make pipeline on a Jenkins instance.

Prerequisites
You will need to create a Pipeline job in Jenkins to run this Jenkinsfile. It will require the following parameters:

Parameter Type	Parameter Name	Description
String	SCM_CHOICE	Source Code Management tool used to store program's source code, valid values are BitBucket or ClearCase. Bitbucket is the default
Choice	GIT_BRANCH	Branch to use for SLOC tool (previously called SLOC_BRANCH which is now deprecated)
String	BITBUCKET_URL	For BITBUCKET only: URL of BitBucket repo containing the source code to be counted
String	BITBUCKET_BRANCH	For BITBUCKET only: Branch to use for counts
String	CLEARCASE_VIEW_NAME	For CLEARCASE only: Name of ClearCase view. It must already exist
String	CLEARCASE_VOB_NAME	For CLEARCASE only: VOB name - automatically appends \
String	CLEARCASE_VIEW_PATH	For CLEARCASE only: Path to repository that contains Jenkinsfile in ClearCase. Relative to the VOB
Credentials or String	BITBUCKET_CREDENTIALS	CredentialsID to use to access source code in Bitbucket repo
String	SOURCE_FOLDERS	Folders in the repo that contains the source code to be counted, space separated. Default would be .
String	IGNORE_FOLDERS	-k option for LOCC, comma separated list of folders to ignore for counts.
String	IGNORE_REGEX	-X option for LOCC, comma separated list of regular expressions (regex's) for file/folders be ignored, e.g. .ABCD.,.DEFG. will skip any files or folders containing ABCD or DEFG
String	LANGUAGE_LIST	-o LOCC option - comma separated list of programming languages to count, e.g., cpp,java,fortran77
String or Choice	LOCC_OPTIONS	LOCC specific flags, space separated. Flags are: -m,-n,-r,-s,-t,-x,-clearcase
String	EMAIL_RECIPIENTS	Comma separated list of email addresses to receive job notification emails
String	PROGRAM_UID	UID of the program
String	CSCI	CSCI of the program
Boolean	UPDATE_METRICS	Option to update the centralized metrics
Credentials or String	CM_USER_CREDENTIALS	CredentialsID to use to update the centralized metrics.
String	JENKINS_NODE	Jenkins node/agent to be used to run the counts
String	CUSTOM_PATH	Paths (colon separated) to required versions of Java/Python/etc., if not already in the system PATH
Implementation
For the pipeline job created:

Set the Pipeline to: Pipeline script from SCM
Set SCM to: Git
Set the repository URL to this repository
Set Credentials to: none
Set branch specifier to: master
Set Script Path to: Jenkinsfile
Click Add by Additional Behaviours, then select "Wipe out repository and force clone" to insure full clone of source code for each run.
The program can also set any other program specific job parameters per program requirements. Once done,this job can be run.

Versioning
Except for testing, programs should use the master branch from this repo. During testing, programs will be provided with the appropriate branch to use for testing.

Authors
To contact the DOCPF team, see the following page:

Contact Us