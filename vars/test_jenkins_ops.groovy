

def executeTests() {
    
    timeStart = new Date()
    stash includes: "C:\\TestOps\\1.zip", name: "1.zip", allowEmpty: true
	timeStop = new Date()
	print (timeStart - timeStop)

	timeStart = new Date()
    stash includes: "C:\\TestOps\\2.zip", name: "2.zip", allowEmpty: true
	timeStop = new Date()
	print (timeStart - timeStop)

	timeStart = new Date()
    stash includes: "C:\\TestOps\\3.zip", name: "3.zip", allowEmpty: true
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
		    String PRJ_PATH="${options.PRJ_ROOT}/${options.PRJ_NAME}"
		    String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
		    options['PRJ_PATH']="${PRJ_PATH}"
		    options['JOB_PATH']="${JOB_PATH}"


		    echo "Scheduling Test Ops"
			node("TestOps"){
				stage("Test"){
					timeout(time: 360, unit: 'MINUTES'){
					    ws("WS/${options.PRJ_NAME}") {
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
	String PRJ_ROOT='TestJenkinsOps'
	String PRJ_NAME='TestJenkinsOps'  
	  main()
    }

