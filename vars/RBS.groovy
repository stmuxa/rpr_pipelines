import java.text.SimpleDateFormat;


class RBSInstance {
    def context
    String url
    String credentialId
    String token

    RBSInstance(settings, context) {
        this.url = settings["url"]
        this.credentialId = settings["credentialId"]
        this.context = context
    }

    def tokenSetup() {
        def response = this.context.httpRequest consoleLogResponseBody: true, httpMode: 'POST', authentication: "${this.credentialId}",  url: "${this.url}/api/login", validResponseCodes: '200'
        def token = this.context.readJSON text: "${response.content}"
        this.token = "${token['token']}"
    }

}


class RBS {

    def instances = []
    def context
    def instancesConfig = [
        [
            "url": "https://rbsdb.cis.luxoft.com",
            "credentialId": "ddd49290-412d-45c3-9ae4-65dba573b4c0"
        ],
        [
            "url" : "https://rbsdbdev.cis.luxoft.com",
            "credentialId": "847a5a5d-700d-439b-ace1-518f415eb8d8"
        ]
    ]

    // context from perent pipeline
    RBS(context) {
        this.context = context
        for (iConfig in this.instancesConfig) {
            this.instances += [new RBSInstance(iConfig, context)]
        }
    }


    def startBuild(jobName, tool, options, env) {
        // get tokens for all instances
        for (i in this.instances) {
            i.tokenSetup()

            def branchTag = "manual"
            String requestData = """
                {"name": "${env.BUILD_NUMBER}",
                "primary_time": "${options.JOB_STARTED_TIME}",
                "branch": "${branchTag}",
                "tool": "${tool}",
                "groups": ["${options.testsList.join('","')}"],
                "count_test_machine" : ${options.gpusCount}}
            """.replaceAll("\n", "")

            def response = this.context.httpRequest acceptType: 'APPLICATION_JSON', consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', customHeaders: [[name: 'Authorization', value: "Token ${i.token}"]], httpMode: 'POST', ignoreSslErrors: true, url: "${i.url}/report/job?data=${java.net.URLEncoder.encode(requestData, 'UTF-8')}", validResponseCodes: '200'
            this.context.echo "Status: ${response.status}\nContent: ${response.content}"
        }
    }


    def setTester(options, env) {
        // TODO: replcae password and login with token from this object
        for (i in this.instances) {
            String tests = (options.tests != "") ? """--tests ${options.tests}""" : ""
            String testsPackage = (options.testsPackage != "none") ? """--tests_package ${options.testsPackage}""" : ""

            def branchTag = "manual" 
            this.context.python3("""jobs_launcher/rbs.py --tool ${options.TESTER_TAG} --branch ${branchTag} --build ${env.BUILD_NUMBER} ${tests} ${testsPackage} --login ${env.RBS_LOGIN} --password ${env.RBS_PASSWORD}""")
        }
    }


    def sendSuiteResult(sessionReport, options, env) {
        def toolName = "Maya"
        def branchTag = "manual"

        String report = this.context.readFile("Results/${toolName}/${options.tests}/report_compare.json")
        this.context.writeJSON file: 'temp_machine_info.json', json: sessionReport.machine_info
        String machine_info = this.context.readFile("temp_machine_info.json")
        String requestData = """
            {
                "job": "${env.BUILD_NUMBER}",
                "group": "${options.tests}",
                "tool": "${toolName}",
                "branch": "${branchTag}",
                "machine_info": ${machine_info},
                "test_results": ${report}
            }
        """.replaceAll("\n", "")

        this.context.writeFile encoding: 'UTF-8', file: 'temp_group_report.json', text: requestData

        for (i in this.instances) {
            def curl = """
                curl -H "Authorization: token ${i.token}" -X POST -F file=@temp_group_report.json ${i.url}/report/group
            """

            if (this.context.isUnix()) {
                this.context.sh curl
            } else {
                this.context.bat curl
            }
        }

        this.context.bat "del temp_group_report.json"
    }


    def finishBuild(status, options, env) {
        def branchTag = "manual"
        def toolName = "Maya"
        def date = new Date()
        def dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")

        String requestData = """
            {
                "name" : "${env.BUILD_NUMBER}",
                "branch": "${branchTag}",
                "tool": "${toolName}",
                "status": "${status}",
                "end_time": "${dateFormatter.format(date)}"
            }
        """
        for (i in this.instances) {
            def response = this.context.httpRequest acceptType: 'APPLICATION_JSON', consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', customHeaders: [[name: 'Authorization', value: "Token ${i.token}"]], httpMode: 'POST', ignoreSslErrors: true, url: "${i.url}/report/end?data=${java.net.URLEncoder.encode(requestData, 'UTF-8')}", validResponseCodes: '200'    
            this.context.echo "Status: ${response.status}\nContent: ${response.content}"
        }
    }
}
