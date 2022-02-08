// Methods in this file will be used at the appropriate time during the build
// Only methods that the program wants to implement need to be added. Un-implemented methods
// will use the default pipeline framework processing

def preFpgaCompile(Map pipelineParams) {
   // Sample implementation for the preFpgaCompile method
   echo "INFO - Vivado specific pre FPGA Compile processing"

   // The following logic loads the tools required by this pipeline before the time of
   // execution to simulate the user having their tools set up before running a pipeline.
   // This is done to avoid the need to procure extra tool licenses solely in order to
   // show that the *framework* itself does not require the PRJ tool for the FPGA stages.
   def docpfPRJFile = 'docpf.fpga.sample'

   def initialPRJFile = sh (script: """#!/bin/tcsh
                                       prj current
                                       """, returnStdout: true)

   if (initialPRJFile.contains("No project is currently loaded")) {
      echo "INFO - No PRJ file currently loaded."
   } else {
      echo "INFO - Initial PRJ file: ${initialPRJFile}"
      pipelineParams.INITIAL_PRJ_FILE = "${initialPRJFile}"
   }

   echo "INFO - Loading DOCPF PRJ file: ${docpfPRJFile}"
   sh """#!/bin/tcsh
         set echo
         chp ${docpfPRJFile}
   """
}

def postFpgaVerifyCDC(Map pipelineParams) {
   // Sample implementation for the postFpgaVerifyCdc method
   echo "INFO - Vivado specific post FPGA Verify CDC processing"
   echo "INFO - Loading initial PRJ file: ${pipelineParams.INITIAL_PRJ_FILE}"
   sh """#!/bin/tcsh
         set echo
         chp ${pipelineParams.INITIAL_PRJ_FILE}
   """
}

return this