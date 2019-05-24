def executeBuildViewer(osName, gpuName, Map options, uniqueID) {
    currentBuild.result = 'SUCCESS'
   
    String scene_name = options['Scene'].split('/')[-1].trim()
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
    
		
		
		
				bat """
					copy "..\\..\\cis_tools\\${options.cis_tools}\\find_scene_blender.py" "."
					copy "..\\..\\cis_tools\\${options.cis_tools}\\blender_render.py" "."
					copy "..\\..\\cis_tools\\${options.cis_tools}\\launch_blender.py" "."
				"""
			    
				python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")
				bat """ 
				"..\\..\\cis_tools\\${options.cis_tools}\\download.bat" "${options.Scene}"
				"""
				bat """
					copy ${scene_name} "..\\..\\RenderServiceStorage\\scenes" 
				"""

				if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
				    bat """
				    "..\\..\\cis_tools\\7-Zip\\7z.exe" x "${scene_name}"
				    """
				    options['sceneName'] = python3("find_scene_blender.py --folder .").split('\r\n')[2].trim()
				}
				
				String scene=python3("find_scene_blender.py --folder .").split('\r\n')[2].trim()
				echo "Find scene: ${scene}"
				echo "Launching render"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}")
				python3("launch_blender.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --render_device_type ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName \"${options.sceneName}\" ")
				echo "Preparing results"
				python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}")
				break;

		    

			}   
	     catch(e) {
			currentBuild.result = 'FAILURE'
			print e
			echo "Error while render"
	    } finally {
			archiveArtifacts 'Output/*'
			String post = python3("..\\..\\cis_tools\\${options.cis_tools}\\send_post.py --django_ip \"${options.django_url}/\" --build_number ${currentBuild.number} --jenkins_job \"${options.jenkins_job}\" --tool ${tool} --status ${currentBuild.result} --id ${id}")
			print post
	    }
	  	break;

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

