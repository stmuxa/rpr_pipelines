def call(String url, String token, String branchTag, String toolName, Map options)
{
    String requestData = """{"name": "${env.BUILD_NUMBER}",
                            "primary_time": "${options.JOB_STARTED_TIME}",
                            "branch": "${branchTag}",
                            "tool": "${toolName}",
                            "groups": ["${options.testsList.join('","')}"],
                            "count_test_machine" : ${options.gpusCount}}"""

    requestData = requestData.replaceAll("\n", "")

    def response = httpRequest acceptType: 'APPLICATION_JSON', consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', customHeaders: [[name: 'Authorization', value: "Token ${token}"]], httpMode: 'POST', ignoreSslErrors: true, url: "${url}?data=${java.net.URLEncoder.encode(requestData, 'UTF-8')}", validResponseCodes: '200'

    echo "Status: ${response.status}\nContent: ${response.content}"
}