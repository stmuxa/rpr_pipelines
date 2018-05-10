import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;

def sendBuildStatusNotification(String buildStatus = 'STARTED', String channel = '', String baseUrl = '', String token = '', Map info)
{
  echo "sending information about build status: ${buildStatus}"
  
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'
  buildStatus = info.CBR ?: buildStatus
 
  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'".replace('%2F', '_')
  def summary = "${subject} (${env.BUILD_URL})"
  
  def details = """${summary}
> Branch: *${info.branch}*
> Author: *${info.author}*
> Commit message: ```${info.commitMessage}```
"""

def slackMessage = """${details}
*Test Report*: ${env.BUILD_URL}${info.htmlLink}"""

 
 
  // Override default values based on build status
  if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else if (buildStatus == 'SKIPPED') {
    color = 'BLUE'
    colorCode = '#0000FF'
  } else if (buildStatus == 'ABORTED') {
    colorCode = '#ff8833'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }
  
   def attachments = """[
        {
            "text": "${slackMessage}",
            "fallback": "Book your flights at https://flights.example.com/book/r123456",
            "actions": [
                {
                    "type": "button",
                    "text": "Report",
                    "url": "${env.BUILD_URL}"
                }
            ]
        }
    ]"""

  // Send notifications
  slackSend (color: colorCode, message: slackMessage, channel: channel, baseUrl: baseUrl, token: token)
}

def call(String projectBranch="")
{
  String CBR = "ABORTED"
  try{
      node('ANDREY_A')
    {
        stage('PreBuild')
        {
            ws("WS/Branch_Prebuild")
            {

              echo "Prebuld"
              echo "=============="
              
              //bat "set"
              echo "${BRANCH_NAME}"
              build = false
              checkOutBranchOrScm(projectBranch, 'https://github.com/luxteam/branch_detect_test.git')

              AUTHOR_NAME = bat (
                      script: "git show -s --format=%%an HEAD ",
                      returnStdout: true
                      ).split('\r\n')[2].trim()

              commitMessage = bat ( script: "git log --format=%%B -n 1", returnStdout: true )
              commitSecond = bat ( script: "git log --format=%%B -n 1", returnStdout: true ).split('\r\n')[2].trim()

              
                  sendBuildStatusNotification(currentBuild.result, 
        'cis_notification_test', 
        'https://luxcis.slack.com/services/hooks/jenkins-ci/',
        'xJp9cOWkS77o74KC0xZOqn4g',
                                [CBR:"${CBR}",
         branch:"${BRANCH_NAME}",
         author:"${AUTHOR_NAME}",
         commitMessage:"${commitMessage}",
        htmlLink:'Test_Report'])
    
        sendBuildStatusNotification(currentBuild.result, 
        'cis_notification_test', 
        'https://luxcis.slack.com/services/hooks/jenkins-ci/',
        'xJp9cOWkS77o74KC0xZOqn4g',
        [CBR:"${CBR}",
         branch:"${BRANCH_NAME}",
         author:"${AUTHOR_NAME}",
         commitMessage:"${commitSecond}",
        htmlLink:'Test_Report'])
            }
        }
    }
  }
  catch(FlowInterruptedException e)
  {
    CBR = "ABORTED"
  }
  finally
  {
    
   /* bat'''
    echo good > report.html
    '''
            
    publishHTML([allowMissing: false,
                reportDir: '.',
                 alwaysLinkToLastBuild: false, 
                 keepAll: true, 
                 reportFiles: 'report.html', reportName: 'Test Report', reportTitles: 'Summary Report'])
                         
    */        

        
  }
}


/*def call(String projectBranch = "") {
  node("ANDREY_A") {
    stage('PreBuild') {
      ws("WS/Branch_Prebuild") {
        echo "Prebuld"
        echo "=============="
        //bat "set"
        echo "${BRANCH_NAME}"
        build = false
        checkOutBranchOrScm(projectBranch, 'https://github.com/luxteam/branch_detect_test.git')
        
        AUTHOR_NAME = bat (
                script: "git show -s --format=%%an HEAD ",
                returnStdout: true
                ).split('\r\n')[2].trim()

        echo "The last commit was written by ${AUTHOR_NAME}."
        if("${BRANCH_NAME}" == "master" && "${AUTHOR_NAME}" != "radeonprorender"){
          echo "master from ${AUTHOR_NAME}"
        } else {
          //def commitHash = checkout(scm).GIT_COMMIT
          //checkout(scm).each { name, value -> println "Name: $name -> Value $value" }
          echo "${BRANCH_NAME} isn't master branch. Parsing commit message..."
          
          if (env.CHANGE_URL){
            echo "it's PR"
          }
          
          commitMessage = bat ( script: "git log --format=%%B -n 1", returnStdout: true )
          commitSecond = bat ( script: "git log --format=%%B -n 1", returnStdout: true ).split('\r\n')[2].trim()

          echo "trim: ${commitSecond}"
          echo "Commit message: ${commitMessage}"
          println commitMessage.getClass()

          if (commitMessage.matches("(.*)CIS:BUILD(.*)")){
            build = true
            echo "found CIS:BUILD by matches"
          }

          if (commitSecond.contains("CIS:BUILD")){
            build = true
            echo "found CIS:BUILD by contains"
          }
        }
       }
     }
    stage('Build') {
      echo "Build"
      echo "=============="
      if(build) {
        ws("WS/Test"){
          echo "true"
        }
      }
    }
    stage('Deploy') {
      echo "Deploy"
      /*checkout([$class: 'GitSCM',
                userRemoteConfigs: [[url: 'https://github.com/luxteam/branch_detect_test.git']]])
      
      AUTHOR_NAME = bat ( script: "git show -s --format='%%an' HEAD ",
                          returnStdout: true
                          ).split('\r\n')[2].trim()

      build = false
      
      if("${BRANCH_NAME}" == "master" && "${AUTHOR_NAME}" != "radeonprorender"){
        def commitHash = checkout(scm).GIT_COMMIT
        echo "${BRANCH_NAME} is master branch. build it by sha: ${commitHash}"

        echo "Commit author: ${AUTHOR_NAME}"
        bat """
        del auto.code
        echo "${commitHash}" > auto.code
        git add auto.code
        git commit -m "buildmaster: version update to sha"
        git push origin HEAD:master
        """
        bat """
        del auto.code
        echo "trym" > auto.code
        git add auto.code
        git commit -m "buildmaster: version update to trym"
        git push origin HEAD:master
        """ 
        build = true
        
        commitHashN = bat ( script: "git log --format=%%H -1 ",
                           returnStdout: true).split('\r\n')[2].trim()
        echo "++++++++++++++++++++++"
        echo "${BRANCH_NAME} is master branch. build it by sha: ${commitHashN}"
    }
  }
}
*/
