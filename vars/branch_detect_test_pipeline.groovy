import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;

def sendBuildStatusNotification(String buildStatus = 'STARTED', String channel = '', String baseUrl = '', String token = '', Map info)
{
  echo "sending information about build status: ${buildStatus}"
  
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'
  // if CBR not null - replace buildStatus value
  buildStatus = info.CBR ?: buildStatus
 
  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'

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
	
  if(env.CHANGE_BRANCH)
  {
    String INIT_BRANCH = "\\nSource branch: *${env.CHANGE_BRANCH}*"
  }

  String slackMessage = """[{		
		"fallback": "Message if attachment disabled",
		"title": "*${buildStatus}*\\nCIS: ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
		"title_link": "${env.BUILD_URL}",
		"color": "${colorCode}",
        "text": ">>> Branch: *${info.branch}*${INIT_BRANCH}\\nAuthor *${info.author}*\\nCommit message\\n```${info.commitMessage}```",
		"mrkdwn_in": ["text", "title"],
		"attachment_type": "default",
		"actions": [
			{"text": "Report",
			"type": "button",
			"url": "${env.BUILD_URL}Test_Report"
			},
			{"text": "PullRequest on GitHub",
			"type": "button",
			"url": "${env.CHANGE_URL}"
			}
		]
	 }]""".replace('%2F', '_')
	
  // Send notifications
  //slackSend (color: colorCode, message: '', channel: channel, baseUrl: baseUrl, token: token, attachment: slackMessage)
  slackSend(attachments: slackMessage, channel: channel, baseUrl: baseUrl, token: token) 
}

def call(String projectBranch="")
{
  Map options = [x:2]
  try{
      node('ANDREY_A')
    {
        stage('PreBuild')
        {
            ws("WS/Branch_Prebuild")
            {
                checkOutBranchOrScm(projectBranch, 'https://github.com/luxteam/branch_detect_test.git')

                AUTHOR_NAME = bat (
                      script: "git show -s --format=%%an HEAD ",
                      returnStdout: true
                      ).split('\r\n')[2].trim()

                commitMessage = bat ( script: "git log --format=%%B -n 1", returnStdout: true ).split('\r\n')[2].trim()
		
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
     
            sendBuildStatusNotification(currentBuild.result,
            'cis_notification_test', 
            'https://luxcis.slack.com/services/hooks/jenkins-ci/',
            "${env.SLACK_LUXCIS_TOKEN}",
            [CBR:options.CBR,
            branch:BRANCH_NAME,
            author:AUTHOR_NAME,
            commitMessage:commitMessage,
            htmlLink:'Test_Report'])
        
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
