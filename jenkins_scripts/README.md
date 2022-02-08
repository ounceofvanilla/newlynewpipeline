General Design Concept

Overview: In the long term, the goal is to create a set of scripts or procedures to configure an EIT-provisioned Jenkins host as required to provide the basic functionality agreed upon by the DevOps team and EIT. The resultant outputs of this effort should require only minimal action on the part of EIT and should be as automated and transparent as possible.

Proposed Design: Utilize Jenkins hook script framework to automatically kick off our set of scripts to configure the instance. The hook utilized in this case is the 'init' hook, which executes scripts in specific locations upon system initialization.

Our scripts will reside in the $JENKINS_HOME/init.groovy.d directory. The initialization process executes scripts in alphanumeric order from that directory. It is recommended that each script be prepended with a number to guarantee order of operation.

The automated configuration scripts will also use persistent environment variables to ensure first-time initialization is only performed once. As a precaution, it is also recommended to check the configuration within the script prior to performing a specific item to prevent unnecessary processing cycles performing the config again.

Example usage: After spinning up a new Jenkins instance configured with the appropriate credentials, EIT drops the init.groovy.d directory containing our automated scripts into the Jenkins home directory. Optionally, log checking may be included to verify all items have completed successfully, but this could also easily be handled in an automated fashion.
