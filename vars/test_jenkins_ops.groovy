

def executeTests() {
    
    timeStart = new Date()
    stash includes: "1.zip", name: "1.zip", allowEmpty: true
	timeStop = new Date()
	print (timeStart - timeStop)

	timeStart = new Date()
    stash includes: "2.zip", name: "2.zip", allowEmpty: true
	timeStop = new Date()
	print (timeStart - timeStop)

	timeStart = new Date()
    stash includes: "3.zip", name: "3.zip", allowEmpty: true
	timeStop = new Date()
	print (timeStart - timeStop)

	timeStart = new Date()
    unstash "1.zip"
	timeStop = new Date()
	print (timeStart - timeStop)

	timeStart = new Date()
    unstash "2.zip"
	timeStop = new Date()
	print (timeStart - timeStop)

	timeStart = new Date()
    unstash "3.zip"
	timeStop = new Date()
	print (timeStart - timeStop)

	timeStart = new Date()
    archiveArtifacts "1.zip"
	timeStop = new Date()
	print (timeStart - timeStop)

	timeStart = new Date()
    archiveArtifacts "2.zip"
	timeStop = new Date()
	print (timeStart - timeStop)

	timeStart = new Date()
    archiveArtifacts "3.zip"
	timeStop = new Date()
	print (timeStart - timeStop)
				
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

