class Node {
    def context
    String link
    String credentialId
    String token

    Node(settings, context) {
        this.link = settings["link"]
        this.credentialId = settings["credentialId"]
        this.context = context
    }

    def getToken() {
        def response = this.context.httpRequest consoleLogResponseBody: true, httpMode: 'POST', authentication: "${credentialId}",  url: "${url}", validResponseCodes: '200'
        def token = this.context.readJSON text: "${response.content}"
        this.token = "${token}"
    }
}

class RBS {

    def nodes = []
    def context

    RBS(nodeList, context) {
        for (nodeConfig in nodeList) {
            this.nodes += [new Node(nodeConfig, context)]
        }
        this.context = context
    }

    def startBuild() {
        for (node in this.nodes) {
            println(node)           
        }
    }

    def setTester() {

    }

    def sendGroupResult() {

    }

    def finishBuild() {
        
    }
}
