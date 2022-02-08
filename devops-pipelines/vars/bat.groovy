// Runs bat and checks every line for errrors
def call(Map params = [:]) {
    String script = params.script
    // Convert the value of returnStatus to a string and only interpret "true" as true; anything else is false
    boolean returnStatus = "${params['returnStatus']}" == "true"
    boolean returnStdout = "${params['returnStdout']}" == "true"
    String encoding = params.get('encoding',null)
    // If strict mode is true, no error handling is injected
    boolean strictMode = "${params['strictMode']}" == "true"

    if(strictMode == false){
        // Initialize the list where we're going to collect all the new script lines
        def newScriptLines = []

        // Patterns to look for in each line
        def pattern = /^ *(rem +|:|@)|(\(|\^) *$/

        // Loop over old scripts lines and inject error handling
        script.split(/(\r\n|\r|\n)/).each { line ->
            // Check if the line contains one of the patterns, if none of the patterns are found inject error handling 
            if(!(line =~ pattern)){
                newScriptLines.add(line)
                newScriptLines.add('@IF NOT %ERRORLEVEL% == 0 EXIT /b %ERRORLEVEL%')
            }
            else{
                newScriptLines.add(line)
            }
        }
        // Create the new script string
        String newScript = newScriptLines.join('\n')

        // Use steps.bat to execute the new script string
        return steps.bat(script: newScript, returnStatus: returnStatus, returnStdout: returnStdout, encoding: encoding)
    }
    else{
        return steps.bat(script: script, returnStatus: returnStatus, returnStdout: returnStdout, encoding: encoding)
    }   

}
// Convenience overload
def call(String script) {
    return bat(script: script)
}