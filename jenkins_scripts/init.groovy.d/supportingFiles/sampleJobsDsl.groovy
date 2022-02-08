String sampleJobsDir = 'DOCPF_Sample_Jobs'
String jenkinsfileTemplate = 'JenkinsfileTemplate'
folder(sampleJobsDir) {
   description("""<p>This sample folder contains a collection of basic Hello-World-esque Jenkins jobs to demonstrate
                  |running a DOCPF pipeline.</p>
                  |For instructions on creating and configuring your own Jenkins job, visit the <a
                  |href="https://confluenceopen01/display/DOCPF/Configure+Jenkins+to+run+DOCPF+sample+jobs">
                  |DOCPF Confluence Page</a>.</p>
                  |<p>For additional assistance with your pipelines, please contact: <a
                  |href="mailto:DOCPF_Leads@L3Harris.com">DOCPF_Leads@L3Harris.com</a>.</p>""".stripMargin())
}

def jobList = [jenkinsfileTemplate, 'make', 'cmake', 'qmake', 'debian', 'maven', 'gradle', 'msbuild', 'setuptools', 'test_pipelines', 'vxworks', 'release', 'sloc', 'msbuild_cpp', 'quartus', 'vivado']

// If startupPlugins.txt was not present, this is an EIT-provisioned instance; skip test_pipelines
def pluginList = new File("/var/jenkins_home/init.groovy.d/supportingFiles/startupPlugins.txt")
if (!pluginList.exists())
{
   jobList -= jenkinsfileTemplate
   jobList -= 'test_pipelines'
}

jobList.each { jobName ->
   pipelineJob("${sampleJobsDir}/${jobName}") {
      logRotator {
         numToKeep(5)
      }
      authenticationToken("token")
      description("""<p>This is a basic Hello-World-esque ${jobName} Jenkins job to demonstrate a running DOCPF 
                     |pipeline.</p>
                     |<p>For instructions on creating and configuring your own Jenkins job, visit the <a
                     |href="https://confluenceopen01/display/DOCPF/Configure+Jenkins+to+run+DOCPF+sample+jobs">
                     |DOCPF Confluence Page</a>.</p>
                     |<p>For additional assistance with your pipelines, please contact: <a
                     |href="mailto:DOCPF_Leads@L3Harris.com">DOCPF_Leads@L3Harris.com</a>.</p>""".stripMargin())
      def repoName = jobName
      if (repoName == 'msbuild_cpp') {
         repoName = 'make'
      }
      parameters {
         buildParameterNodes['GIT_BRANCH'] = NodeBuilder.newInstance().'net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition' {
            name('GIT_BRANCH')
            type('PT_BRANCH_TAG')
            defaultValue('origin/master')
            branchFilter('.*')
            tagFilter('*')
            sortMode('DESCENDING_SMART')
               selectedValue('TOP')
               quickFilterEnabled(true)
         }

         if (jobName != 'sloc') {
            stringParam('PIPELINE_VERSION', 'origin/master', '')
            stringParam('EMAIL_RECIPIENTS', '', '')
            if (jobName == jenkinsfileTemplate) {
               stringParam('PARAMS_MAP', '[:]', """The pipeline params to pass into the Jenkinsfile template
                  |Note: a parameter called TEST_JOB_NAME can be included in the map to add to the build's display name.
                  |Example: if PARAMS_MAP['TEST_JOB_NAME'] for build #42 is 'NIX:make', the build's display name will be '42 (NIX:make)'.""".stripMargin())
               repoName = 'test_pipelines'
            }
            if (['msbuild', 'setuptools', 'maven'].contains(jobName)) {
               stringParam('CUSTOM_BUILD_NUMBER', '', '')
            }
            if (jobName == 'release') {
               repoName = 'setuptools'
               stringParam('PATH_TO_ARTIFACT', 'devops_factory_generic/Setuptools/HelloWorld/1.0.0-25', '')
               stringParam('FILE_NAME_OF_ARTIFACT', '', '')
               choiceParam('ARCHIVE_MODE', ['model', 'archive'], "Options: model or archive. Model will print all file paths that would be archived. Archive will move the files.")
               choiceParam('ARCHIVE_CHOICE', ['name', 'properties'], "Options: name or properties. If the name or properties match the pattern, it will NOT be archived.")
               stringParam('PATTERN_TO_KEEP', '(.*)_r.pyc', 'You can specify either a file/directory name pattern or properties. If the file does NOT match the pattern, it will be archived. If you are using file/directory name, use a regex. If you are using properties, be sure to follow this format: { "Key": ["Value"], "Key": ["Value"] }')
            }
            if (jobName == 'cmake' || jobName == 'debian')
            {
               repoName = 'make'
            }
            if (jobName == 'quartus' || jobName == 'vivado')
            {
               repoName = 'fpga'
            }
            if (jobName == 'test_pipelines') {
               textParam('TOOL_VERSIONS', '', """A list of tools and versions to override default versions used by testing.
                  |Example:
                  |make=feature/DOJ-174
                  |vxworks=release/3.0
                  |maven=release/2.0""".stripMargin())
               booleanParam('RUN_PARAMETER_TESTS', true, "Whether to run the parameter validation tests")
	            booleanParam('RUN_STAGE_TESTS', true, "Whether to test all tools for every stage")
               booleanParam('RUN_ENVIRONMENT_TESTS', true, "Whether to run the environment tests")
               stringParam('SAMPLE_JOBS_TO_TEST', '', "OPTIONAL: A comma-separated list of the sample jobs to include in the environment test. Use this to limit which tests are run while debugging.")
               booleanParam('EXCLUDE_SAMPLE_JOBS_TO_TEST', false, "Invert the meaning of SAMPLE_JOBS_TO_TEST (these tests will NOT be run)")
               stringParam('DISPLAY_NAME', '', """OPTIONAL: Sets a custom build name that is appended to the unique build number. The resulting format is "DISPLAY_NAME#BUILD_NUMBER" """)
               ['DEBIAN', 'CENTOS', 'UBUNTU', 'UNIX_SC'].each { agentName -> 
                  stringParam("${agentName}_AGENT", '', "Optional label to use for the ${agentName}-based node or cloud template.")
               }
            }
            if (jobName == 'make') {
               choiceParam('PACKAGE_ARCHIVE_TYPE', ['zip', 'tar', 'tgz'], "Type of package file to create")
            }
            if (jobName == 'gradle') {
               repoName = 'maven'
            }             
         } else if (jobName == 'sloc') {            
            stringParam('SCM_CHOICE', 'BitBucket', 'Source Code Management tool used to store program\'s source code, valid values are BitBucket or ClearCase. Bitbucket is the default')		
            stringParam('BITBUCKET_URL', 'https://lnsvr0329.gcsd.harris.com:8443/bitbucket/scm/devp/devops-pipelines.git', 'For BITBUCKET only: URL of BitBucket repo containing the source code to be counted.')
            stringParam('BITBUCKET_BRANCH', 'master', 'For BITBUCKET only: Branch to use for counts.')
            stringParam('CLEARCASE_VIEW_NAME', '', 'For CLEARCASE only: Name of ClearCase view. It must already exist.')
            stringParam('CLEARCASE_VOB_NAME', '', 'For CLEARCASE only: VOB name - automatically appends \\.')
            stringParam('CLEARCASE_VIEW_PATH', '', 'For CLEARCASE only: Path to repository that contains Jenkinsfile in ClearCase. Relative to the VOB.')
            stringParam('BITBUCKET_CREDENTIALS', 'BITBUCKET_CREDENTIALS', 'CredentialsID to use to access source code in Bitbucket repo.')
            stringParam('SOURCE_FOLDERS', '', 'Folders in the repo that contains the source code to be counted, space separated. Default would be.')
            stringParam('IGNORE_FOLDERS', 'none', '-k option for LOCC, comma separated list of folders to ignore for counts.')
            stringParam('IGNORE_REGEX', '', '-X option for LOCC, comma separated list of regular expressions (regex\'s) for file/folders be ignored, e.g. .ABCD.,.DEFG. will skip any files or folders containing ABCD or DEFG.')
            stringParam('LANGUAGE_LIST', '', '-o LOCC option - comma separated list of programming languages to count, e.g., cpp,java,fortran77.')
            stringParam('LOCC_OPTIONS', '-x', 'LOCC specific flags, space separated. Flags are: -m,-n,-r,-s,-t,-x,-clearcase.')
            stringParam('EMAIL_RECIPIENTS', '', 'Comma separated list of email addresses to receive job notification emails.')
            stringParam('PROGRAM_UID', '1234', 'UID of the program.')
            stringParam('CSCI', 'PIPELINE', 'CSCI of the program.')
            booleanParam('UPDATE_METRICS', false, 'Option to update the centralized metrics (true/false).')
            credentialsParam('CM_USER_CREDENTIALS') {
               type('com.cloudbees.plugins.credentials.common.StandardCredentials')
               description('Credentials ID to use to update the centralized metrics.')
            }
            stringParam('JENKINS_NODE', 'master', 'Jenkins node/agent to be used to run the counts.')
            stringParam('CUSTOM_PATH', '', 'Paths (colon separated) to required versions of Java/Python/etc., if not already in the system PATH.')	
         }
      }
      definition {
         cpsScm {
               scm {
                  git {
                     remote {
                        url("https://lnsvr0329.gcsd.harris.com:8443/bitbucket/scm/devp/${repoName}.git")
                     }

                     branch('\${GIT_BRANCH}')

                     extensions {
                        if (jobName == 'sloc') {
                           wipeOutWorkspace()
                        }

                        if (jobName == 'cmake' || jobName == 'debian' || jobName == 'make') {
                            submoduleOptions {
                                parentCredentials(true)
                                recursive(true)
                            }
                        }
                     }
                  }
               }
               
            if (jobName == jenkinsfileTemplate) {
               scriptPath(jenkinsfileTemplate)
            }
            if (jobName == 'release') {
               scriptPath('JenkinsfileRelease')
            }
            if (jobName == 'cmake')
            {
               scriptPath('JenkinsfileCMake')
            }
            if (jobName == 'debian')
            {
               scriptPath('JenkinsfileDebian')
            }
            if (jobName == 'msbuild_cpp') {
               scriptPath('Jenkinsfile_msbuild')
            }
            if (jobName == 'quartus' || jobName == 'vivado') {
               scriptPath("${jobName}/Jenkinsfile")
            }
            if (jobName == 'gradle') {
               scriptPath('JenkinsfileGradle')
            }
         }
      }
   }
}