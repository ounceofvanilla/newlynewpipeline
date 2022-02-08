def call(String nodeLabel) {
   if (nodeLabel != 'master') 
   {
      def node_tags = ["name", "label"]
      def docker_tags = ["name", "labelString"]

      // Create Docker grep command
      def docker_grep_command = ""

      if(isUnix()) {
         docker_grep_command = "grep -F $JENKINS_HOME/config.xml"
         docker_tags.each {
            docker_grep_command += " -e ${it}"
         }
      } else {
         docker_grep_command = "findstr /r \""
         docker_tags.each {
         docker_grep_command += " ${it}"
      }
      docker_grep_command += "\" $JENKINS_HOME\\config.xml"
      }

      def node_grep_command = ""
      // Check if nodes directory exists
      if(fileExists("$JENKINS_HOME/nodes/")) {
         // Create node grep command if nodes directory is non-empty
         if(isUnix() && !sh(returnStdout: true, script: 'find "${JENKINS_HOME}"/nodes/ -maxdepth 0 -empty')) {
            node_grep_command = " && grep -F -r $JENKINS_HOME/nodes/*"
            node_tags.each {
               node_grep_command += " -e ${it}"
            }
         } else if(!isUnix() && bat(returnStdout: true, script: "dir /b $JENKINS_HOME\\nodes\\")) {
            node_grep_command = " && findstr /s /r \""
            node_tags.each {
               node_grep_command += " ${it}"
            }
            node_grep_command += "\" $JENKINS_HOME\\nodes\\*"
         }
      }

      // Create grep command for shell invocation
      // regex is used to find exact nodeLabel while
      // still accounting for multiple labels
      // i.e. desired nodeLabel should only ever 
      // touch angle brackets or whitespace
      def grep_command = "${docker_grep_command}${node_grep_command}"
      def searchResult

      if (isUnix()) {
         searchResult = sh returnStdout: true, script: "${grep_command}"
      }
      else {
         searchResult = bat returnStdout: true, script: "${grep_command}"
      }

      // Node (?m) is a multiline regex search
      return searchResult =~ /(?m)(^| |>)${nodeLabel}(<| |$)/
   }
   else 
   {
      return true 
   }
}