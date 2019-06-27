

def executeTests() {
    stash includes: "1.zip", name: "1.zip", allowEmpty: true
    stash includes: "2.zip", name: "2.zip", allowEmpty: true

    unstash "1.zip"
    unstash "2.zip"

    archiveArtifacts "1.zip"
    archiveArtifacts "2.zip"			
}



def main() {
	 
	try {
		timestamps {
		    echo "Scheduling Test Ops"
			node("TestOps"){
				stage("Test"){
					timeout(time: 360, unit: 'MINUTES'){
					    ws("WS/TestOps") {
							executeTests()
						}
					}
				}
			}
		}			  
    } catch (e) {
		println(e.toString());
		println(e.getMessage());
		println(e.getStackTrace());
		currentBuild.result = "FAILED"
		throw e
   	}
}
    
def call() {
	  main()
    }

