def executeBuildViewer(osName, gpuName, Map options, uniqueID) {
    currentBuild.result = 'SUCCESS'
   
    String filename = options['data_link'].split('/')[-1].trim()
    echo "${options}"
    
    timeout(time: 1, unit: 'HOURS') {
	switch(osName) {
		case 'Windows':
		    try {
				print("Clean up work folder")
				bat '''
					@echo off
					del /q *
					for /d %%x in (*) do @rd /s /q "%%x"
				''' 

				print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_execute_status.py --django_ip \"${options.django_url}/\" --status \"Downloading package\" --id ${id}"))
				bat """ 
					wget --no-check-certificate "${options.data_link}"
				"""
				bat """
					7z x "${filename}"
				"""

				print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_execute_status.py --django_ip \"${options.django_url}/\" --status \"Testing Package\" --id ${id}"))
				python3("${CIS_TOOLS}\\${options.cis_tools}\\launch_executer.py --filename ${options.filename} ").split('\r\n')[-1].trim()
				echo "Preparing results"
				print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_execute_status.py --django_ip \"${options.django_url}/\" --status \"Completed\" --id ${id}"))


		    } catch(e) {
				currentBuild.result = 'FAILURE'
				print e
				echo "Error while render"
		    } finally {
				archiveArtifacts "Output/*"
				print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_execute_results.py --django_ip \"${options.django_url}\" --build_number ${currentBuild.number} --jenkins_job \"${options.jenkins_job}\" --status ${currentBuild.result} --id ${id}"))
		    }
		break
		case 'Ubuntu':
			try{
				print("Clean up work folder")
					sh '''
						rm -rf *
					'''
					print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_execute_status.py --django_ip \"${options.django_url}/\" --status \"Downloading package\" --id ${id}"))
					sh """ 
						wget --no-check-certificate "${options.data_link}"
					"""
					sh """
						7z x "${filename}"
					"""

					print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_execute_status.py --django_ip \"${options.django_url}/\" --status \"Testing Package\" --id ${id}"))
					python3("${CIS_TOOLS}\\${options.cis_tools}\\launch_executer.py --filename ${options.filename} ").split('\r\n')[-1].trim()
					echo "Preparing results"
					print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_execute_status.py --django_ip \"${options.django_url}/\" --status \"Completed\" --id ${id}"))


			    } catch(e) {
					currentBuild.result = 'FAILURE'
					print e
					echo "Error while render"
			    } finally {
					archiveArtifacts "Output/*"
					print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_execute_results.py --django_ip \"${options.django_url}\" --build_number ${currentBuild.number} --jenkins_job \"${options.jenkins_job}\" --status ${currentBuild.result} --id ${id}"))
			    }
		break
		}
	}
}



def main(String platforms, Map options) {
	 
	try {

	timestamps {
	    String PRJ_PATH="${options.PRJ_ROOT}/${options.PRJ_NAME}"
	    String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
	    options['PRJ_PATH']="${PRJ_PATH}"
	    options['JOB_PATH']="${JOB_PATH}"

	    boolean PRODUCTION = true

	    if (PRODUCTION) {
		options['django_url'] = "https://render.cis.luxoft.com/execute/jenkins/"
		options['cis_tools'] = "RenderServiceScripts"
		options['jenkins_job'] = "RenderServiceExecuteJob"
	    } else {
		options['django_url'] = "https://testrender.cis.luxoft.com/execute/jenkins/"
		options['cis_tools'] = "RenderServiceScripts"
		options['jenkins_job'] = "RenderServiceExecuteJob"
	    }

	    def testTasks = [:]
	    def nodes = platforms.split(';')
	    int platformCount = nodes.size()
	    
		for (i = 0; i < platformCount; i++) {

		    String uniqueID = Integer.toString(i)

		    String item = nodes[i]
		    Map newOptions = options.clone()

		    List tokens = item.tokenize(':')
		    String osName = tokens.get(0)
		    String deviceName = tokens.get(1)
		    
		    String executeDevice = ""
		    if (deviceName == "ANY") {
				executeDevice = "ExecuteService"
		    } else {
			    executeDevice = "gpu${deviceName}"
		    }
		    
		    echo "Scheduling Build Viewer ${osName}:${deviceName}"
		    testTasks["Test-${osName}-${deviceName}"] = {
				node("${osName} && ExecuteService && ${executeDevice}"){
				    stage("BuildViewer-${osName}-${deviceName}"){
						timeout(time: 60, unit: 'MINUTES'){
						    ws("WS/${newOptions.PRJ_NAME}") {
								executeBuildViewer(osName, deviceName, newOptions, uniqueID)
						    }
						}
				    }
				}
		    }
		}

		parallel testTasks

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
    String data_link = '',  
    String platforms = '',
    String id = '',
    String filename = ''
    ) {
	String PRJ_ROOT='RenderServiceExecuteJob'
	String PRJ_NAME='RenderServiceExecuteJob'  
	main(platforms,[
	    enableNotifications:false,
	    PRJ_NAME:PRJ_NAME,
	    PRJ_ROOT:PRJ_ROOT,
	    data_link:data_link,
	    id:id,
	    filename:filename
	    ])
    }

