def executeBuildViewer(osName, gpuName, Map options, uniqueID) {
    currentBuild.result = 'SUCCESS'
   
    String scene_name = options['scene_link'].split('/')[-1].trim()
    echo "${options}"
    
    timeout(time: 1, unit: 'HOURS') {
	    try {
			print("Clean up work folder")
			bat '''
				@echo off
				del /q *
				for /d %%x in (*) do @rd /s /q "%%x"
			''' 

			print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_viewer_status.py --django_ip \"${options.django_url}/\" --status \"Downloading viewer\" --id ${id}"))
		    
			print(python3("${CIS_TOOLS}\\${options.cis_tools}\\download_viewer.py --version ${options.viewer_version} "))
		    	bat """
				7z x "RPRViewer.zip" 
			"""

			print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_viewer_status.py --django_ip \"${options.django_url}/\" --status \"Downloading scene\" --id ${id}"))
			bat """ 
				wget --no-check-certificate "${options.scene_link}"
			"""
			bat """
				7z x "${scene_name}"
			"""
			bat '''
				del /q *.zip
				del /q *.7z
			''' 

			print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_viewer_status.py --django_ip \"${options.django_url}/\" --status \"Building RPRViewer Package\" --id ${id}"))
			python3("${CIS_TOOLS}\\${options.cis_tools}\\configure_viewer.py --version ${options.viewer_version} --width ${options.width} --height ${options.height} --engine ${options.engine} --iterations ${options.iterations} --scene_name \"${options.scene_name}\" --scene_version ${options.scene_version} ").split('\r\n')[-1].trim()
			echo "Preparing results"
			print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_viewer_status.py --django_ip \"${options.django_url}/\" --status \"Completed\" --id ${id}"))
			
		    	try {
				archiveArtifacts "*.zip"
				archiveArtifacts "*.txt"
				if (options.engine != "ogl") {archiveArtifacts "img0001.png"}
				
			 } catch(e) {
				currentBuild.result = 'FAILURE'
				print e
			}
		    
	    		}   
	     catch(e) {
		     	print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_viewer_status.py --django_ip \"${options.django_url}/\" --status \"Completed\" --id ${id}"))
			currentBuild.result = 'FAILURE'
			print e
		     	archiveArtifacts "*.zip"
		     	archiveArtifacts "*.txt"
			echo "Error while configurating viewer"
	    } finally {
		     	print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_viewer_results.py --django_ip \"${options.django_url}\" --build_number ${currentBuild.number} --jenkins_job \"${options.jenkins_job}\" --status ${currentBuild.result} --id ${id}"))
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
		options['django_url'] = "https://render.cis.luxoft.com/viewer/jenkins/"
		options['cis_tools'] = "RenderServiceScripts"
		options['jenkins_job'] = "RenderServiceViewerJob"
	    } else {
		options['django_url'] = "https://testrender.cis.luxoft.com/viewer/jenkins/"
		options['cis_tools'] = "RenderServiceScripts"
		options['jenkins_job'] = "RenderServiceViewerJob"
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
		    
		    String renderDevice = ""
		    if (deviceName == "ANY") {
				renderDevice = "Viewer"
		    } else {
			    renderDevice = "gpu${deviceName}"
		    }
		    
		    echo "Scheduling Build Viewer ${osName}:${deviceName}"
		    testTasks["Test-${osName}-${deviceName}"] = {
				node("${osName} && RenderService && ${renderDevice}"){
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
    String scene_link = '',  
    String platforms = '',
    String viewer_version = '',
    String id = '',
    String width = '',
    String height = '',
    String engine = '',
    String iterations = '',
    String scene_name = '',
    String scene_version = ''
    ) {
	String PRJ_ROOT='RenderServiceViewerJob'
	String PRJ_NAME='RenderServiceViewerJob'  
	main(platforms,[
	    enableNotifications:false,
	    PRJ_NAME:PRJ_NAME,
	    PRJ_ROOT:PRJ_ROOT,
	    scene_link:scene_link,
	    viewer_version:viewer_version,
	    id:id,
	    width:width,
	    height:height,
	    engine:engine,
	    iterations:iterations,
	    scene_name:scene_name,
	    scene_version:scene_version
	    ])
    }

