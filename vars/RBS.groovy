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
        try {
            def response = this.context.httpRequest consoleLogResponseBody: true, httpMode: 'POST', authentication: "${this.credentialId}",  url: "${this.url}/api/login", validResponseCodes: '200'
            def token = this.context.readJSON text: "${response.content}"
            this.token = "${token.token}"
        } catch (e) {
            println(e)
        }
    }
}

class RBS {

    def instances = []
    def context
    Map instancesConfig = [
        "master": [
            "url": "https://rbsdb.cis.luxoft.com",
            "credentialId": "ddd49290-412d-45c3-9ae4-65dba573b4c0"
        ],
        "develop": [
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

    def startBuild() {
        println("Send login request to Report Builder System")
        // get tokens for all instances
        for (i in this.instances) {
            i.tokenSetup()
        }
    }

    def setTester() {

    }

    def sendGroupResult() {

    }

    def finishBuild() {
        
    }
}
