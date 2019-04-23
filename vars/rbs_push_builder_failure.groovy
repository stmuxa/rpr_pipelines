def call(String url, String token, String branchTag, String toolName)
{
    def response = httpRequest consoleLogResponseBody: true, customHeaders: [[name: 'Authorization', value: "Token ${token}"]], httpMode: 'POST', ignoreSslErrors: true, url: "${url}?name=${env.BUILD_NUMBER}&tool=${toolName}&branch=${branchTag}&status=FAILURE", validResponseCodes: '200'

    echo "Status: ${response.status}\nContent: ${response.content}"
}