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
            echo "Status: ${response.status}\nContent: ${response.content}"
        }
    }

    def setTester() {

    }

    def sendGroupResult() {

    }

    def finishBuild() {
        
    }
}
