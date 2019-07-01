

def executeTests(operations) {
	if ("stash" in operations) {
		stash includes: "1.zip", name: "1.zip", allowEmpty: true
		stash includes: "2.zip", name: "2.zip", allowEmpty: true
	}

	if ("unstash" in operations) {
		unstash "1.zip"
		unstash "2.zip"
	}
	
	if ("archiveArtifacts" in operations) {
		archiveArtifacts "1.zip"
		archiveArtifacts "2.zip"		
	}
}

def main(label, operations, type) {
	try {
		timestamps {
			
			def testers = [:]

			testers["${label}"] = {
				node("${label}") {
					stage("Test"){
						timeout(time: 360, unit: 'MINUTES'){
							ws("WS/TestOps") {
								executeTests(operations)
							}
						}
				    }
				}
			}

			parallel testers
		}			  
	} catch (e) {
		println(e.toString());
		println(e.getMessage());
		println(e.getStackTrace());
		currentBuild.result = "FAILED"
		throw e
	}
}
	
def call(
		String label = "",
		String operations = "",
		String type = ""
		) {
	  	main(label, operations, type)
	}


