def doPreStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.preCodeCoverage(pipelineParams)
}

def doMainStageProcessing(Map pipelineParams) {  
   def commandStatus
   switch(pipelineParams.CODE_COVERAGE_CHOICE) {
      case 'opencover':
         // OpenCover is only compatible with Windows so notify the user of this if they are trying to run it in a Unix environment.
         if (isUnix()) {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": OpenCover, Linux")
         }

         //Set report and folder values to default if no value set
         pipelineParams.CODE_COVERAGE_REPORT_FILE = """${pipelineParams.CODE_COVERAGE_REPORT_FILE ?: "Cobertura.xml"}"""
         pipelineParams.CODE_COVERAGE_REPORT_FOLDER = "${pipelineParams.CODE_COVERAGE_REPORT_FOLDER ?: 'coverage'}"
         pipelineParams.UNIT_TEST_RESULTS_FILE = "${pipelineParams.UNIT_TEST_RESULTS_FILE ?: 'UnitTestResults.trx'}"
         
         // Run OpenCover on all binaries found that match the pattern specified by the user.
         // Since we output the binaries to the workspace, we know that the unit test binaries will be located in the workspace.
         def vstestArgs = """\\"!CONTAINERS!\\" /ResultsDirectory:\\"${pipelineParams.OUTPUT_PATH ?: env.workspace}\\" /logger:trx;LogFileName=\\"${pipelineParams.UNIT_TEST_RESULTS_FILE}\\" """
         commandStatus = bat(script:"""
                                       SETLOCAL enableDelayedExpansion
                                       FOR %%f IN (${pipelineParams.TEST_BINARY_PATTERN}) do (SET CONTAINERS=!CONTAINERS! %%f)
                                       OpenCover.Console.exe -register -target:"vstest.console.exe" -targetargs:"${vstestArgs}" -output:"${pipelineParams.CODE_COVERAGE_REPORT_FILE}" ^
                                          -returntargetcode:0
                                       echo %errorlevel% > commandStatus.log
            ReportGenerator.exe -reports:"${pipelineParams.CODE_COVERAGE_REPORT_FILE}" -targetdir:"${pipelineParams.OUTPUT_PATH ?: env.workspace}/${pipelineParams.CODE_COVERAGE_REPORT_FOLDER}" -reporttypes:Cobertura
                                       SETLOCAL disableDelayedExpansion
                                    """, returnStatus: true)
         
         // Read file to get opencover return status
         def returnStatus = readFile 'commandStatus.log'
         returnStatus = returnStatus.trim()
         if (returnStatus == "1")
         {
            // Set it to unstable and continue
            logger.logWarning("Code Coverage" + constants.TEST_FAILURE_WARNING)
            currentBuild.result = 'UNSTABLE'
         }
         else if (commandStatus != 0 || returnStatus != '0')
         {  
            logger.logError("OpenCover" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      case 'opencppcoverage':
         // OpenCppCoverage is only compatible with Windows so notify the user of this if they are trying to run it in a Unix environment.
         if (isUnix()) {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": OpenCppCoverage, Linux")
         }

         //Set report value to default if no value set
         pipelineParams.CODE_COVERAGE_REPORT_FILE = "${pipelineParams.CODE_COVERAGE_REPORT_FILE  ?: 'HelloWorldTestCoverage.xml'}"

         if (pipelineParams.USE_NUGET == 'true') {
            commandStatus = bat(script: """  OpenCppCoverage.exe --sources "%CD%\\${pipelineParams.SOURCE_CODE_DIRECTORY}" --excluded_sources "%CD%\\${pipelineParams.SOURCE_CODE_DIRECTORY}\\packages" --export_type=cobertura -- "${pipelineParams.GT_TEST_BINARY}" 
                                        """, returnStatus: true)
         }
         else {
            commandStatus = bat(script: """  OpenCppCoverage.exe --sources "%CD%\\${pipelineParams.SOURCE_CODE_DIRECTORY}" --export_type=cobertura -- "${pipelineParams.GT_TEST_BINARY}" 
                                        """, returnStatus: true)
         }

         if(commandStatus == 1) 
         {
            // Do not fail the build if unit tests fail
            // Set it to unstable and continue
            logger.logWarning("Code Coverage" + constants.TEST_FAILURE_WARNING)
            currentBuild.result = 'UNSTABLE'
         }
         else if (commandStatus != 0)
         {
            logger.logError("OpenCppCoverage" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      case 'cobertura':
         //Set report and folder values to default if no value set
         pipelineParams.CODE_COVERAGE_REPORT_FILE = """${pipelineParams.CODE_COVERAGE_REPORT_FILE ?: "**/cobertura/coverage.xml"}"""

         // Use the same command to run code coverage but execute with shell or Batch depending on the environment.
         def cobertura_command = "mvn cobertura:cobertura -gs settings.xml -Dcobertura.report.format=xml"
         if (isUnix()) {
            commandStatus = sh(script: "${cobertura_command}", returnStatus: true)
         }
         else {
            commandStatus = bat(script: "${cobertura_command}", returnStatus: true)
         }

         if (commandStatus != 0)
         {
            logger.logError("Cobertura" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      case 'jacoco':
         // All processing handled by Maven and JacoCo plugin and executed when running unit tests
         break
      case 'gcov':
         // GCov is only compatible with Unix, so notify the user of this if they are trying to run it in a Windows environment.
         if (!isUnix()) {
            logger.logError(constants.INCOMPATIBLE_ENVIRONMENT_ERROR + ": GCov, Windows")
         }

         //Set report and folder values to default if no value set
         pipelineParams.CODE_COVERAGE_REPORT_FOLDER = "${pipelineParams.CODE_COVERAGE_REPORT_FOLDER ?: 'coverage'}"
         try {
            // GCOV will produce a code coverage result in text on the Jenkins log file.
            sh "gcov \$(find '${pipelineParams.SOURCE_CODE_DIRECTORY}' -name '${pipelineParams.GCOV_FILENAME_PATTERN}')"
            // LCOV is used to display the GCOV results in a graphical manner.
            sh "lcov --capture --directory . --output-file coverage.info --no-external"
            // Generate an HTML file so Jenkins can display it to the user.
            sh "genhtml coverage.info --output-directory '${env.workspace}/${pipelineParams.CODE_COVERAGE_REPORT_FOLDER}'"
         } catch (e) {
            logger.logError("GCov" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      case 'coverage':
         // Set report name value default if no name set
         pipelineParams.CODE_COVERAGE_REPORT_FILE = "${pipelineParams.CODE_COVERAGE_REPORT_FILE ?: 'coverage.xml'}"

         // Use the same commands to run code coverage but execute with shell or Batch depending on the environment.
         def coverage_command = """-m coverage run --source "${pipelineParams.SOURCE_CODE_DIRECTORY}" setup.py test"""
         def coverage_html_command = """-m coverage xml -o "${pipelineParams.CODE_COVERAGE_REPORT_FILE}" """
         try {
            if (isUnix()) {
               sh """
                  python3 ${coverage_command}
                  python3 ${coverage_html_command}
               """
            }
            else {
               bat """
                  python ${coverage_command}
                  python ${coverage_html_command}
               """
            }
         } catch (e) {
            logger.logError("Coverage" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }

         
         break
      case 'pytest-cov':
         try {
         // Set report name value default if no name set
            pipelineParams.CODE_COVERAGE_REPORT_FILE = "${pipelineParams.CODE_COVERAGE_REPORT_FILE ?: 'coverage.xml'}"

            def testTypes = pipelineParams.UNIT_TEST_TYPES ? " -m \"${pipelineParams.UNIT_TEST_TYPES}\" " : ''
            // Use the same commands to run code coverage but execute with shell or Batch depending on the environment.
            def pytest_command = "-m pytest \"${pipelineParams.UNIT_TEST_DIRECTORY}\" --cov=\"${pipelineParams.SOURCE_CODE_DIRECTORY}\" --cov-report=xml:\"${pipelineParams.CODE_COVERAGE_REPORT_FILE}\" -v${testTypes} "
            if (isUnix()) {
                sh "python3 ${pytest_command}"
            }
            else {
                bat """
                    python ${pytest_command}
            """
            }
         } catch (e) {
            logger.logError("PyTest-cov" + constants.TOOL_EXECUTION_FAILURE)
            currentBuild.result = 'FAILURE'
         }
         break
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.CODE_COVERAGE_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doMainStageAnalysis(Map pipelineParams) {
   // Default variables that are manipulated depending on the code coverage tool.
   def PUBLISH_HTML_REPORT_FILES = "index.html"
   def COVERAGE_REPORT_FILE = "${pipelineParams.CODE_COVERAGE_REPORT_FILE}"

   // This section will change the above default values if necessary.
   // These need to be done before the publishHTML plugin is run.
   switch(pipelineParams.CODE_COVERAGE_CHOICE) {
      case 'opencover':
         // publish and archive VSTest results created in this stage
         toolArguments = [[$class: 'MSTestJunitHudsonTestType', deleteOutputFiles: true, failIfNotNew: true, pattern: '*.trx', skipNoTestFiles: false, stopProcessingIfError: true]]
         if (utilMethods.checkPluginVersion("xunit","2.0.0", pipelineParams) < 0) {
            xunitClass = 'XUnitBuilder'
         }
         else {
            xunitClass = 'XUnitPublisher'
         }
         def unittestPublishArgs =  [$class: "${xunitClass}", testTimeMargin: '3000', thresholdMode: 1, thresholds: [
                                       [$class: 'FailedThreshold', failureNewThreshold: "${pipelineParams.FAILED_UNIT_TEST_FAILURE_NEW_THRESHOLD}", failureThreshold: "${pipelineParams.FAILED_UNIT_TEST_FAILURE_THRESHOLD}", unstableNewThreshold: "${pipelineParams.FAILED_UNIT_TEST_UNSTABLE_NEW_THRESHOLD}", unstableThreshold: "${pipelineParams.FAILED_UNIT_TEST_UNSTABLE_THRESHOLD}"], 
                                       [$class: 'SkippedThreshold', failureNewThreshold: "${pipelineParams.SKIPPED_UNIT_TEST_FAILURE_NEW_THRESHOLD}", failureThreshold: "${pipelineParams.SKIPPED_UNIT_TEST_FAILURE_THRESHOLD}", unstableNewThreshold: "${pipelineParams.SKIPPED_UNIT_TEST_UNSTABLE_NEW_THRESHOLD}", unstableThreshold: "${pipelineParams.SKIPPED_UNIT_TEST_UNSTABLE_THRESHOLD}"]],
                                     tools: toolArguments]
         archiveArtifacts allowEmptyArchive: true, artifacts: "${pipelineParams.UNIT_TEST_RESULTS_FILE}"
         step(unittestPublishArgs + pipelineParams.UNIT_TEST_PUBLISH_ARGS)
         
         // archive and publish OpenCover tests
         PUBLISH_HTML_REPORT_FILES = "index.htm"
         vstestPublishArgs = [$class: 'XUnitPublisher', testTimeMargin: '3000', thresholdMode: 1, thresholds: [], 
                                          tools: [MSTest(deleteOutputFiles: true, failIfNotNew: true, pattern: '*.trx', skipNoTestFiles: false, stopProcessingIfError: true)]]
         step(vstestPublishArgs + pipelineParams.UNIT_TEST_PUBLISH_ARGS)
         break
      case 'opencppcoverage':
      case 'cobertura':
      case 'pytest-cov':
      case 'coverage':
         def codeCoveragePublishArgs = [autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: "${COVERAGE_REPORT_FILE}", 
                                        conditionalCoverageTargets: "${pipelineParams.CONDITIONAL_COVERAGE_TARGETS}", lineCoverageTargets: "${pipelineParams.LINE_COVERAGE_TARGETS}", methodCoverageTargets: "${pipelineParams.METHOD_COVERAGE_TARGETS}", 
                                        failNoReports: false, failUnhealthy: "${pipelineParams.COVERAGE_FAIL_UNHEALTHY}".toBoolean(), failUnstable: "${pipelineParams.COVERAGE_FAIL_UNSTABLE}".toBoolean(), maxNumberOfBuilds: 0,
                                       onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false]

         cobertura codeCoveragePublishArgs + pipelineParams.CODE_COVERAGE_PUBLISH_ARGS
         break
      case 'gcov':
      case 'jacoco':
      // Run the publishHTML plugin now that the fields have been updated correctly.
      def codeCoveragePublishArgs = [allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: "${pipelineParams.CODE_COVERAGE_REPORT_FOLDER}", 
                                       reportFiles: "${PUBLISH_HTML_REPORT_FILES}", reportName: 'Code Coverage Report', reportTitles: 'Code Coverage']
      publishHTML(codeCoveragePublishArgs + pipelineParams.CODE_COVERAGE_PUBLISH_ARGS)
      default:
         logger.logError(constants.INVALID_TOOL_FOR_STAGE, pipelineParams.CODE_COVERAGE_CHOICE, this.getClass().getSimpleName())
         break
   }
}

def doPostStageProcessing(def prePostProcessing, Map pipelineParams) {
   prePostProcessing.postCodeCoverage(pipelineParams)
}

def call(Map pipelineParams) { 
   doMainStageProcessing(pipelineParams)
}