# DevSecOps Common Pipeline Framework (DSOCPF)
Configure Jenkins to run DSOCPF sample jobs
Prerequisite steps
1. For linux sample jobs (make, maven, setuptools)
  a. setup docker agent host
  b. setup docker cloud on Jenkins Controller
2. For Windows sample jobs (msbuild, vxworks)
  a. setup Windows Agent Host
  b. setup Windows Agent Host on Jenkins Controller
  c. launch Windows Agent
  
Crate Jenkins Pipeline Job
This section decribes how to create a Jenkins pipline job that will run one of the DSOCPF sample jobs. The process is the same for all samples. Just repeat for each Bitbucket URL.

  1. Optional: Create a folder to contain the sample jobs
  2. Click New item, insert a name, choose Pipeline, and click OK
  3. Select This project is parameterized (Note: You must install the GIT plugin for this step)
    a. Click Add Parameter and select Git Parameter
      i. Under Name, Type:
        GIT_BRANCH
      ii. Under Parameter Type, select Branch or Tag
      iii. Under Default Value, put: 
        Origin/master
    b. Click add Parameter and select String Parameter
      i. Under Name, use: 
       PIPELINE_VERSION
      ii. Under Default Value, use: 
       Master
    c. Click Add Parameter and select String Parameter
      i. Under Name, use: 
       EMAIL_RECIPTIENTS 
      ii. Under Default Value, specify a comma seprated list of users to notify when this job completes. This can be blank. 
  4. Scroll down to Pipeline and for definition select Pipeline from SCM. 
  5. Select Fit as the SCM. 
  6. For the DSOCPF sample jobs, use the following URL's in Repository URL. You will need to create a pipeline job fro each repository. 
  
  RUN SAMPLE JOBS
  1. select one fo the sample jobs created above 
  2. select build with parameters
  3. ensure that the GUIT_BRANCH parameter matches the branch to run the source code on 
  4. click the build button 
  5. wait for the magic to happen
    a. for make:
      URL: https://xxx/make.git
    b. for Maven: 
      URL: https://xxx/maven.git
    c. For MsBuild:
    d. For Python: 
    e. For VxWorks:
    f. for QMake: 
 7. Change Branch Sepcifier to the following: 
  ${GIT_BRANCH}
8. Uncheck th eLightweight checkout option 
9. Click Save. 
