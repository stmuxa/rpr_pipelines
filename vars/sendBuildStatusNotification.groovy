
def call(String buildStatus = 'STARTED', String channel = '', String baseUrl = '', String token = '', Map info)
{
  echo "sending information about build status: ${buildStatus}"
  
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'
  buildStatus = info.CBR ?: buildStatus
  info.commitMessage = info.commitMessage ?: 'undefiend'
  String BRANCH_NAME = env.BRANCH_NAME ?: info.projectBranch
  
  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  
  // Override default values based on build status
  if (buildStatus == 'SUCCESSFUL')
  {
    color = 'GREEN'
    colorCode = '#00FF00'
  }
  else if (buildStatus == 'ABORTED')
  {
    colorCode = '#ff8833'
  }
  else if (buildStatus == 'SKIPPED')
  {
    color = 'BLUE'
    colorCode = '#0000FF'
  }
  else
  {
    color = 'RED'
    colorCode = '#FF0000'
  }
  
  // if env.CHANGE_BRANCH not empty display pull request link
  String INIT_BRANCH = env.CHANGE_BRANCH ? "\\nSource branch: *${env.CHANGE_BRANCH}*" : ''
  // if reportName not empty display link to html report
  String HTML_REPORT_LINK = info.reportName ? "${env.BUILD_URL}${info.reportName}" : ''
  
  String testsStatus = """
  ,{
    "mrkdwn_in": ["text"],
    "title": "Brief info",
    "pretext": "AutoTests Results",
    "text": ${info.testsStatus},
    "color": "#07f700",
    "footer": "LUX CIS",
    "actions": [
      {"text": "Report",
      "type": "button",
      "url": "${HTML_REPORT_LINK}"
      }]
  }"""
  
  String slackMessage = """[{		
		"fallback": "${buildStatus} ${env.JOB_NAME}",
		"title": "${buildStatus}\\nCIS: ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
		"title_link": "${env.BUILD_URL}",
		"color": "${colorCode}",
    "text": ">>> Branch: *${BRANCH_NAME}*${INIT_BRANCH}\\nAuthor: *${info.AUTHOR_NAME}*\\nCommit message:\\n```${info.commitMessage.replace('\n', '\\n')}```",
		"mrkdwn_in": ["text", "title"],
		"attachment_type": "default",
		"actions": [
			{"text": "PullRequest on GitHub",
			"type": "button",
			"url": "${env.CHANGE_URL}"
			}
		]
	 }${testsStatus}]""".replace('%2F', '_')
  
  println(slackMessage)
  
  // Send notifications
  slackSend(attachments: slackMessage, channel: 'cis_notification_test', baseUrl: 'https://luxcis.slack.com/services/hooks/jenkins-ci/', token: "${SLACK_LUXCIS_TOKEN}")
  // slackSend (attachments: slackMessage, channel: channel, baseUrl: baseUrl, token: token) 
}
