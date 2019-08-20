class Node {
    String link
    String credentialId
    String token

    Node(settings) {
        this.link = settings["link"]
        this.credentialId = settings["credentialId"]
        try {
            def res = httpRequest consoleLogResponseBody: true, httpMode: 'POST', authentication: "${settings['credentialId']}",  url: "${settings['link']}", validResponseCodes: '200'
            def content = readJSON text: "${response.content}"
            this.token = "${response.content}"
        }
        catch (e) {
            println(e)
        }
    }
}

class RBS {

    def nodes = []

    RBS(nodeList) {
        for (nodeConfig in nodeList) {
            this.nodes += [new Node(nodeConfig)]
        }
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
