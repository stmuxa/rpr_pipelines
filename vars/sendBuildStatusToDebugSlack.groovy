def call(Map options) {
    String BRANCH_NAME = env.BRANCH_NAME ?: options.projectBranch

    // send only failed jobs into separate channel
    // send master PR & Weekly into separate channel

    if (currentBuild.result == "FAILURE" && BRANCH_NAME == "master") {
        String debagSlackMessage = """[{
          "title": "${buildStatus}\\nCIS: ${env.JOB_NAME} [${env.BUILD_NUMBER}]",
          "title_link": "${env.BUILD_URL}",
          "color": "${colorCode}",
          "text": "Failed reason is ${options.failureMessage}"
          }]
        """;

        try {
            slackSend (attachments: debagSlackMessage, channel: env.debagChannel, baseUrl: env.debagUrl, tokenCredentialId: 'debug-channel')
        } catch (e) {
            println("Error during slack notification to debug channel")
            println(e.toString())
        }
    }
}
