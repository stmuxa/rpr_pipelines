def call(String url, String token, String branchTag, String toolName)
{
    String requestData = """{"name" : "${env.BUILD_NUMBER}",
                            "branch": "${branchTag}",
                            "tool": "${toolName}",
                            "status": "${currentBuild.result}"}"""

    def response = httpRequest acceptType: 'APPLICATION_JSON', consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', customHeaders: [[name: 'Authorization', value: "Token ${token}"]], httpMode: 'POST', ignoreSslErrors: true, url: "${url}?data=${java.net.URLEncoder.encode(requestData, 'UTF-8')}", validResponseCodes: '200'
    
    echo "Status: ${response.status}\nContent: ${response.content}"
}