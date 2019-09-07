def executeConvert(osName, gpuName, Map options, uniqueID) {
    currentBuild.result = 'SUCCESS'
    
    String tool = options['Tool'].split(':')[0].trim()
    String version = options['Tool'].split(':')[1].trim()
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

		switch(tool) {
		    case 'Maya Redshift':
			    
				bat """
					cd "${CIS_TOOLS}\\..\\RenderServiceStorage\\RS2RPRConvertTool" 
					git pull
					cd "..\\..\\WS\\Render_Scene_Render"
					copy "${CIS_TOOLS}\\..\\RenderServiceStorage\\RS2RPRConvertTool\\convertRS2RPR.py" "."
				"""

				bat """
					copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_maya.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_maya_redshift_conversion.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_maya_rpr_conversion.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\maya_rpr_conversion.py" "."
				"""
			
				print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_convert_status.py --django_ip \"${options.django_url}/\" --tool \"${tool}\" --status \"Downloading scene\" --id ${id}"))
				bat """ 
					wget --no-check-certificate "${options.Scene}"
				"""

				if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
				    bat """
				    	7z x "${scene_name}"
				    """
				    options['sceneName'] = python3("find_scene_maya.py --folder .").split('\r\n')[2].trim()
				}
				
				String scene=python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
				echo "Find scene: ${scene}"
				echo "Launching conversion and render"
				print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_convert_status.py --django_ip \"${options.django_url}/\" --tool \"${tool}\" --status \"Rendering Redshift scene\" --id ${id}"))
				python3("launch_maya_redshift_conversion.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --scene \"${scene}\" --sceneName ${options.sceneName}")
				print(python3(".${CIS_TOOLS}\\${options.cis_tools}\\send_convert_status.py --django_ip \"${options.django_url}/\" --tool \"${tool}\" --status \"Rendering converted scene\" --id ${id}"))
				python3("launch_maya_rpr_conversion.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --scene \"${scene}\" --sceneName ${options.sceneName}")
				echo "Preparing results"
				print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_convert_status.py --django_ip \"${options.django_url}/\" --tool \"${tool}\" --status \"Completed\" --id ${id}"))
				
				archiveArtifacts 'Output/*'
				break;
			}   
	    } catch(e) {
			currentBuild.result = 'FAILURE'
			print e
			echo "Error while render"
	    } finally {
			print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_convert_results.py --django_ip \"${options.django_url}\" --build_number ${currentBuild.number} --jenkins_job \"${options.jenkins_job}\" --status ${currentBuild.result} --id ${id}"))
	    }
	  break;
	}
    }
}


def main(String PCs, Map options) {
	
    try {

	timestamps {
	    String PRJ_PATH="${options.PRJ_ROOT}/${options.PRJ_NAME}"
	    String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
	    options['PRJ_PATH']="${PRJ_PATH}"
	    options['JOB_PATH']="${JOB_PATH}"

	    boolean PRODUCTION = false

	    if (PRODUCTION) {
		options['django_url'] = "https://render.cis.luxoft.com/convert/jenkins/"
		options['plugin_storage'] = "https://render.cis.luxoft.com/media/plugins/"
		options['cis_tools'] = "RenderServiceScripts"
		options['jenkins_job'] = "RenderServiceConvertJob"
	    } else {
		options['django_url'] = "https://testrender.cis.luxoft.com/convert/jenkins/"
		options['plugin_storage'] = "https://testrender.cis.luxoft.com/media/plugins/"
		options['cis_tools'] = "RenderServiceScripts"
		options['jenkins_job'] = "RenderServiceConvertJob"
	    }

	    def testTasks = [:]
	    def nodes = PCs.split(';')
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
				renderDevice = "Redshift"
		    } else {
			    renderDevice = "gpu${deviceName}"
		    }
		    
		    echo "Scheduling converter task ${osName}:${deviceName}"
		    testTasks["Test-${osName}-${deviceName}"] = {
				node("${osName} && RenderService && ${renderDevice}"){
				    stage("BuildViewer-${osName}-${deviceName}"){
						timeout(time: 60, unit: 'MINUTES'){
						    ws("WS/${newOptions.PRJ_NAME}") {
								executeConvert(osName, deviceName, newOptions, uniqueID)
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
    
def call(String Tool = '',
    String Scene = '',  
    String PCs = '',
    String id = '',
    String sceneName = '',
    ) {
	String PRJ_ROOT='RenderServiceConvertJob'
	String PRJ_NAME='RenderServiceConvertJob'  
	main(PCs,[
	    enableNotifications:false,
	    PRJ_NAME:PRJ_NAME,
	    PRJ_ROOT:PRJ_ROOT,
	    Tool:Tool,
	    Scene:Scene,
	    id:id,
	    sceneName:sceneName
	    ])
    }
