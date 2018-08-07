
def call(String buildStatus = 'STARTED', String channel = '', String baseUrl = '', String token = '', Map info)
{
  echo "sending information about build status: ${buildStatus}"
  
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'
  buildStatus = info.CBR ?: buildStatus
  info.commitMessage = info.commitMessage ?: 'undefiend'
  
  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'".replace('%2F', '_')
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>"""
 
  // Override default values based on build status
  if (buildStatus == 'SUCCESS')
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
  
  testsColorCode = "#322123"
  String testsStatus = """
  ,{
    "title": "Testing results",
    "fields": [
        {
            "title": "Plugin version",
            "value": "2.3.313",
            "short": true
        },
        {
            "title": "Core version",
            "value": "1.3.12",
            "short": true
        },
        {
            "title": "Failed",
            "value": "21/200",
            "short": true
        },
        {
            "title": "Skipped",
            "value": "21/200",
            "short": true
        },
        {
            "title": "Error",
            "value": "31/82",
            "short": true
        },
        {
            "title": "Passed",
            "value": "21/200",
            "short": true
        }
        
    ],
    "actions": [
        {
            "text": "Report",
            "type": "button",
            "url": "${HTML_REPORT_LINK}"
        }
    ],
    "color": "${testsColorCode}",
    "footer": "LUX CIS"
  }
   """


  String slackMessage = """
  [
    {		
  		"fallback": "${buildStatus} ${env.JOB_NAME}",
  		"title": "${buildStatus}\\nCIS: ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
  		"title_link": "${env.BUILD_URL}",
  		"color": "${colorCode}",
      "text": ">>> Branch: *${env.BRANCH_NAME}*${INIT_BRANCH}\\nAuthor: *${info.author}*\\nCommit message:\\n```${info.commitMessage.replace('\n', '\\n')}```",
  		"mrkdwn_in": ["text", "title"],
  		"attachment_type": "default",
  		"actions": [
  			{
          "text": "PullRequest on GitHub",
          "type": "button",
          "url": "https://github.com"
  			}
  		]
  	}${testsStatus}
  ]""".replace('%2F', '_')
  
  println(slackMessage)
  
  // Send notifications
  //slackSend (color: colorCode, message: summary, channel: channel, baseUrl: baseUrl, token: token)
  slackSend(attachments: slackMessage, channel: 'cis_notification_test', baseUrl: 'https://luxcis.slack.com/messages/CANEHHL22', token: "${SLACK_LUXCIS_TOKEN}")
  // slackSend (attachments: slackMessage, channel: channel, baseUrl: baseUrl, token: token) 
}
