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
    def tool
    def branchTag
    def buildName
    def rbsLogin
    def rbsPassword
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
    RBS(context, tool, branchTag, env) {
        this.context = context
        this.tool = tool
        this.buildName = env.BUILD_NUMBER
        this.rbsLogin = env.RBS_LOGIN
        this.rbsPassword = env.RBS_PASSWORD
        this.branchTag = branchTag
        for (iConfig in this.instancesConfig) {
            this.instances += [new RBSInstance(iConfig, context)]
        }
    }


    def startBuild(options) {
        // get tokens for all instances
        try {
            for (i in this.instances) {
                i.tokenSetup()

                String requestData = """
                    {"name": "${this.buildName}",
                    "primary_time": "${options.JOB_STARTED_TIME}",
                    "branch": "${this.branchTag}",
                    "tool": "${this.tool}",
                    "groups": ["${options.testsList.join('","')}"],
                    "count_test_machine" : ${options.gpusCount}}
                """.replaceAll("\n", "")

                def response = this.context.httpRequest acceptType: 'APPLICATION_JSON', consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', customHeaders: [[name: 'Authorization', value: "Token ${i.token}"]], httpMode: 'POST', ignoreSslErrors: true, url: "${i.url}/report/job?data=${java.net.URLEncoder.encode(requestData, 'UTF-8')}", validResponseCodes: '200'
                this.context.echo "Status: ${response.status}\nContent: ${response.content}"
            }
        } catch (e) {
            this.context.echo e
            this.context.echo "RBS could not create a build! Next requests not available."
        }
    }


    def setTester(options) {
        try {
            String tests = (options.tests != "") ? """--tests ${options.tests}""" : ""
            String testsPackage = (options.testsPackage != "none") ? """--tests_package ${options.testsPackage}""" : ""
            
            this.context.python3("""jobs_launcher/rbs.py --tool ${this.tool} --branch ${this.branchTag} --build ${this.buildName} ${tests} ${testsPackage} --login ${this.rbsLogin} --password ${this.rbsPassword}""")
        } catch (e) {
            this.context.echo e
            this.context.echo "RBS Set Tester is crash!"
        }
    }


    def setFailureStatus() {
        for (i in this.instances) {
            def response = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[name: 'Authorization', value: "Token ${i.token}"]], httpMode: 'POST', ignoreSslErrors: true, url: "${i.url}?name=${this.buildName}&tool=${this.tool}&branch=${this.branchTag}&status=FAILURE", validResponseCodes: '200'
            this.context.echo "Status: ${response.status}\nContent: ${response.content}"
        }
    }

    def sendSuiteResult(sessionReport, options) {
        try {
            String report = this.context.readFile("Results/${this.tool}/${options.tests}/report_compare.json")
            this.context.writeJSON file: 'temp_machine_info.json', json: sessionReport.machine_info
            String machine_info = this.context.readFile("temp_machine_info.json")

            String requestData = """
                {
                    "job": "${this.buildName}",
                    "group": "${options.tests}",
                    "tool": "${this.tool}",
                    "branch": "${this.branchTag}",
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
        } catch (e) {
            this.context.echo "RBS Send Group Results is crash!"
        }
    }


    def finishBuild(options, status) {
        try {

            String requestData = """
                {
                    "name" : "${this.buildName}",
                    "branch": "${this.branchTag}",
                    "tool": "${this.tool}",
                    "status": "${status}",
                    "end_time": "${getTime()}"
                }
            """
            for (i in this.instances) {
                def response = this.context.httpRequest acceptType: 'APPLICATION_JSON', consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', customHeaders: [[name: 'Authorization', value: "Token ${i.token}"]], httpMode: 'POST', ignoreSslErrors: true, url: "${i.url}/report/end?data=${java.net.URLEncoder.encode(requestData, 'UTF-8')}", validResponseCodes: '200'
                this.context.echo "Status: ${response.status}\nContent: ${response.content}"
            }
        } catch (e) {
            this.context.echo "RBS Finish Build is crash!"
        }
    }


    def getTime() {
        def date = new Date()
        def dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
        return dateFormatter.format(date)
    }


    def getBranchTag(String name) {
        if (name.contains("Weekly")) {
            return "weekly"
        } else if (name.contains("Auto")) {
            return "master"
        } else {
            return "manual"
        }
    }
}
