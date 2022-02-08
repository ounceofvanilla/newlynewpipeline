// This is to be used when the environment is unable to be supported
// due to incompatibility with the tool.
@groovy.transform.Field
final static String INCOMPATIBLE_ENVIRONMENT_ERROR = "Selected tool cannot be run in the current environment"

// This is to be used when that environment is able to be
// supported but we have not included the logic yet to do so.
// Although, we have implemented the logic in a different environment,
// which is suggested to the user.
@groovy.transform.Field
final static String UNSUPPORTED_ENVIRONMENT_ERROR = "Selected tool is not supported for the current environment at this time"

// This is used when a tool has not been supported with a build tool yet.
@groovy.transform.Field
final static String UNIMPLEMENTED_BUILD_TOOL_ERROR = "Selected tool is not implemented for the following build tool at this time"

// This is to be used when an error case was reached that should have been
// caught by error checking logic earlier. An example is when an improper
// choice is selected but gets to the default case in the switch statement.
// The error should have been caught in setup environment.
@groovy.transform.Field
final static String ENVIRONMENT_ERROR = "An environment/logic error occured."

// Standard text for prePostProcessing log messages when calling a programs method
@groovy.transform.Field
final static String NO_PROGRAM_SPECIFIC_METHOD = "No Program Specific method defined: "

// Standard text for prePostProcessing log messages when calling a programs method
@groovy.transform.Field
final static String CALL_PROGRAM_METHOD = "Calling Program Specific method: "

// Standard text for prePostProcessing log messages when calling a programs method
@groovy.transform.Field
final static String COMPLETE_PROGRAM_METHOD = "Completed Program Specific method: "

// This is to be used when a test stage has run into test failures.
@groovy.transform.Field
final static String TEST_FAILURE_WARNING = " tests failed."

// Invalid tool specified error message (expects stage and tool to be provided to logger method)
@groovy.transform.Field
final static String INVALID_TOOL_FOR_STAGE = "Invalid tool '{0}' specified for stage {1}"


// This is to be used when a test stage has a failure not relates to test results.
@groovy.transform.Field
final static String TOOL_EXECUTION_FAILURE = " failure when calling tool."

// This is the initial stage data that defines the full set of base stages
// supported by the pipeline. Each key represents a stage and maps to a
// stage object, which is the main shared library object that contains the
// processing specific to that stage.
@groovy.transform.Field
final Map INITIAL_STAGE_DATA = [BUILD: [stageObject: buildSourceCode],
                                CREATE_INSTALLER: [stageObject: createInstaller],
                                FPGA_COMPILE: [stageObject: fpgaCompile],
                                UNIT_TEST: [stageObject: unitTest],
                                STATIC_CODE_ANALYSIS: [stageObject: staticCodeAnalysis],
                                STATIC_APPLICATION_SECURITY: [stageObject: staticApplicationSecurity],
                                FPGA_LINT: [stageObject: fpgaLint],
                                FPGA_VERIFY_CDC: [stageObject: fpgaVerifyCDC],
                                CODE_COVERAGE: [stageObject: codeCoverage],
                                STYLE_CHECK: [stageObject: styleCheck],
                                ARTIFACT_MANAGEMENT: [stageObject: artifactManagement],
                                DEPLOY_ARTIFACTS: [stageObject: artifactDeployment],
                                FUNCTIONAL_TEST: [stageObject: functionalTest],
                                DYNAMIC_APPLICATION_SECURITY: [stageObject: dynamicApplicationSecurity],
                                TAG_SOURCE_CODE: [stageObject: tagSourceCode],
                                PROMOTE_ARTIFACT: [stageObject: promoteArtifact],
                                MERGE_BRANCH: [stageObject: mergeBranch],
                                ARCHIVE_ARTIFACT: [stageObject: archiveFilteredArtifacts],
                                PACKAGE_FOR_DELIVERY: [stageObject: packageForDelivery],
                                DOCKER_IMAGE_MANAGEMENT: [stageObject: dockerImageManagement],
                                DOCKER_IMAGE_BUILD: [stageObject: dockerImageBuild],
                                DOCKER_IMAGE_PUSH: [stageObject: dockerImagePush],
                                CONTAINER_SCAN: [stageObject: containerScan]]