import RepoConnection

/** An Artifactory-specific repo connection class **/
class ArtifactoryConnection
{
   RepoConnection repo
   String repoConnectString
   
   /** Constructor **/
   ArtifactoryConnection(String host, int port, def creds)
   {
      this.repo = new RepoConnection(host, port, creds)
      this.repoConnectString = "curl -f -u ${creds.username}:${creds.password} -k"
   }
   

   /** Download a file from Artifactory using the provided API call in curl.
     *
     * @param sourcePath      the path inside the repo to the source file
     * @param targetPath      the local path where the file will be downloaded
     * @param overwrite       whether to overwrite the local file if it exists
     * @return whether or not the command executed without errors
     **/
   boolean download(String sourcePath, String targetPath = null, boolean overwrite = false)
   {
      return repo.downloadFile(sourcePath, targetPath, overwrite) { target ->
         "${repoConnectString} https://${repo.host}:${repo.port}/${sourcePath} -o ${target}"
      }
   }
}