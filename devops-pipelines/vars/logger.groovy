// Logging methods

// This is to be used when you need to log a generic error.
def logError(String errorMessage, Object... args) {
   // This plugin will append ERROR to the front of the message
   error(formatString(errorMessage, args))
}

// This is to be used when you need to log a generic info message.
def logInfo(String infoMessage, Object... args) {
   echo "INFO: ${formatString(infoMessage, args)}"
}

// This is to be used when you need to log a generic warning.
def logWarning(String warningMessage, Object... args) {
   echo "WARNING: ${formatString(warningMessage, args)}"
}

/* Format a string with enumerated variables to be interpolated
 * Example: formatString("Value 1 = {0}, value 2 = {1}", 'first', 'second')
 * Output:  Value 1 = first, value 2 = second
 */
def formatString(String message, Object... args) {
   return message.replaceAll(/\{(\d+)\}/) { fullMatch ->
      int argIndex = fullMatch[1] as Integer
      if (argIndex < args.size())
      {
         args[argIndex]
      }
      else
      {
         "UNDEFINED"
      }
   }
}