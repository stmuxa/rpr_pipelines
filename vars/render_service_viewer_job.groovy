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

			//python3("${CIS_TOOLS}\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --status \"Downloading viewer\" --id ${id}")

			python3("${CIS_TOOLS}\\${options.cis_tools}\\download_viewer.py --version ${options.viewer_version} ")
			bat """
				"${CIS_TOOLS}\\7-Zip\\7z.exe" x "RPRViewer.zip" 
			"""

			//python3("${CIS_TOOLS}\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --status \"Downloading scene\" --id ${id}")
			bat """ 
				wget --no-check-certificate "${options.scene_link}"
			"""
			bat """
				"${CIS_TOOLS}\\7-Zip\\7z.exe" x "${scene_name}"
			"""

			bat '''
				del /q *.zip
				del /q *.7z
			''' 

			//python3("${CIS_TOOLS}\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Building RPRViewer Package\" --id ${id}")
			String zip_name=python3("${CIS_TOOLS}\\${options.cis_tools}\\configure_viewer.py --version ${options.viewer_version} --width ${options.width} --height ${options.height} --engine ${options.engine} ").split('\r\n')[-1].trim()
			echo "Build zip: ${zip_name}"
			echo "Preparing results"
			//python3("${CIS_TOOLS}\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}")
			archiveArtifacts "${zip_name}"

			}   
	     catch(e) {
			currentBuild.result = 'FAILURE'
			print e
			echo "Error while render"
	    } finally {
		     	String post = python3("${CIS_TOOLS}\\${options.cis_tools}\\send_post.py --django_ip \"${options.django_url}/\" --build_number ${currentBuild.number} --jenkins_job \"${options.jenkins_job}\" --tool ${tool} --status ${currentBuild.result} --id ${id}")
			print post
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
		options['cis_tools'] = "RenderServiceScriptsDebug"
		options['jenkins_job'] = "RenderServiceViewerJobDebug"
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
    String engine = ''
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
	    ])
    }

