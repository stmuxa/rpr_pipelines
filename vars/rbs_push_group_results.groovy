def call(String url, String token, String branchTag, String toolName, Map options)
{
    String report = readFile("Results/${toolName}/${options.tests}/report_compare.json")

    String machine_info = readFile("temp_machine_info.json")

    String requestData = """{"job": "${env.BUILD_NUMBER}",
                            "group": "${options.tests}",
                           "tool": "${toolName}",
                           "branch": "${branchTag}",
                           "machine_info": ${machine_info},
                           "test_results": ${report}}"""
    requestData = requestData.replaceAll("\n", "")
    writeFile encoding: 'UTF-8', file: 'temp_group_report.json', text: requestData

    if(isUnix())
    {
        sh """
        curl -H "Authorization: token ${token}" -X POST -F file=@temp_group_report.json ${url}
        """
    }
    else
    {
        bat """
        curl -H "Authorization: token ${token}" -X POST -F file=@temp_group_report.json ${url}
        """
    }
}