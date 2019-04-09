def call(String url, String credentialId)
{
    def response = httpRequest consoleLogResponseBody: true, httpMode: 'POST', authentication: "${credentialId}",  url: "${url}", validResponseCodes: '200'
    def token = readJSON text: "${response.content}"
    return "${token}"
}