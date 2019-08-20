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

    // context from perent pipeline
    RBS(instances, context) {
        this.context = context
        for (instanceConfig in instances) {
            this.instances += [new RBSInstance(instanceConfig, context)]
        }
    }

    def startBuild(options) {
        // get tokens for all instances
        for (i in this.instances) {
            n.tokenSetup()
        }
    }

    def setTester() {

    }

    def sendGroupResult() {

    }

    def finishBuild() {
        
    }
}
