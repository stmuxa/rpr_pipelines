
def call(String buildStatus = 'STARTED', String channel = '', String baseUrl = '', String token = '', Map info = [commitMessage:"undefiend",
														  author:"undefiend",
														  reportName:""])
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
  def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>"""
 
  // Override default values based on build status
  if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else if (buildStatus == 'SKIPPED') {
    color = 'BLUE'
    colorCode = '#0000FF'
  } else if (buildStatus == 'TERMINATED') {
    colorCode = '#ff8833'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }
  
  String INIT_BRANCH = env.CHANGE_BRANCH ? "\\nSource branch: *${env.CHANGE_BRANCH}*" : ''
  String HTML_REPORT_LINK = info.reportName ? "${env.BUILD_URL}${info.reportName}" : ''
  
  String slackMessage = """[{		
		"fallback": "Message if attachment disabled",
		"title": "${buildStatus}\\nCIS: ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
		"title_link": "${env.BUILD_URL}",
		"color": "${colorCode}",
    "text": ">>> Branch: *${env.BRANCH_NAME}*${INIT_BRANCH}\\nAuthor *${info.author}*\\nCommit message\\n```${info.commitMessage.replace('\n', '\\n')}```",
		"mrkdwn_in": ["text", "title"],
		"attachment_type": "default",
		"actions": [
			{"text": "Report",
			"type": "button",
			"url": "${HTML_REPORT_LINK}"
			},
			{"text": "PullRequest on GitHub",
			"type": "button",
			"url": "${env.CHANGE_URL}"
			}
		]
	 }]""".replace('%2F', '_')
  
  // Send notifications
  slackSend (color: colorCode, message: summary, channel: channel, baseUrl: baseUrl, token: token)
  if(channel == 'cis_notification_test'){
    slackSend (attachments: slackMessage, channel: channel, baseUrl: baseUrl, token: token) 
  }
}
