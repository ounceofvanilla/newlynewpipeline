import java.util.logging.Logger

/** A general repository class to handle API requests to a repo within an OS process **/
class RepoConnection
{
   String host
   int port
   def creds
   def logger = Logger.getLogger("hudson.WebAppMain")

   /** Constructor **/
   RepoConnection(String host, int port, def creds)
   {
      this.host = host
      this.port = port
      this.creds = creds
   }

   /** Download a file from the repo using the provided repo-specific closure.
     *
     * @param sourcePath      the path inside the repo to the source file
     * @param targetPath      the local path where the file will be downloaded
     * @param overwrite       whether to overwrite the local file if it exists
     * @param getDownloadCmd  a closure that returns the repo-specific command to execute
     * @return whether or not the command executed without errors
     **/
   boolean downloadFile(String sourcePath, String targetPath = null, boolean overwrite = false, Closure getDownloadCmd)
   {
      File targetFile = targetPath ? new File(targetPath) : new File('.')

      String fullTargetPath = targetFile.getAbsolutePath()
      if (targetFile.isDirectory())
      {
         fullTargetPath = sourcePath.split('/')[-1]
         targetFile = File(fullTargetPath)
      }

      if (targetFile.exists() && !overwrite)
      {
         logger.warning("The file '${fullTargetPath}' already exists and overwrite was set to false; skipping")
         // Return true here because it is not a failure if the file exists and we don't want it overwritten.
         return true
      }

      def process = getDownloadCmd(fullTargetPath)?.execute()

      if (!process)
      {
         logger.warning("Unable to build repo command; ${sourcePath} was not downloaded")
         return false
      }

      process.waitFor()

      if (process.exitValue())
      {
         logger.warning("An error was returned attempting to download a file from ${host}: ${process.text}")
         return false
      }

      if (!targetFile.exists())
      {
         logger.warning("The file ${fullTargetPath} could not be found; assuming download failed")
         return false
      }

      return true
   }
}