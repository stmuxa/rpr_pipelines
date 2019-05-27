def executeRender(osName, gpuName, Map options, uniqueID) {
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
		    case 'RedshiftConvert':
			    
				bat """
					cd "..\\..\\RenderServiceStorage\\RS2RPRConvertTool" 
					git pull
					cd "..\\..\\WS\\Render_Scene_Render"
					copy "..\\..\\RenderServiceStorage\\RS2RPRConvertTool\\convertRS2RPR.py" "."
				"""

				bat """
					copy "..\\..\\cis_tools\\${options.cis_tools}\\find_scene_maya.py" "."
					copy "..\\..\\cis_tools\\${options.cis_tools}\\launch_redshift_render_conv.py" "."
					copy "..\\..\\cis_tools\\${options.cis_tools}\\launch_converted_render.py" "."
					copy "..\\..\\cis_tools\\${options.cis_tools}\\maya_convert_render.py" "."
				"""
				
				String scene_exists = python3("..\\..\\cis_tools\\${options.cis_tools}\\check_scene_exists.py --file_name ${scene_name} ").split('\r\n')[2].trim()
				if (scene_exists == "file_exists") {
				    bat """
						copy "..\\..\\RenderServiceStorage\\scenes\\${scene_name}" "."
				    """
				} else {
				    python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")
				    bat """ 
				    	"..\\..\\cis_tools\\${options.cis_tools}\\download.bat" "${options.Scene}"
				    """
				    bat """
						copy ${scene_name} "..\\..\\RenderServiceStorage\\scenes" 
				    """
				}

				if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
				    bat """
				    	"..\\..\\cis_tools\\7-Zip\\7z.exe" x "${scene_name}"
				    """
				    options['sceneName'] = python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
				}
				
				String scene=python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
				echo "Find scene: ${scene}"
				echo "Launching conversion and render"
				python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering Redshift scene\" --id ${id}")
				python3("launch_redshift_render_conv.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --scene \"${scene}\" --sceneName ${options.sceneName}")
				python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering converted scene\" --id ${id}")
				python3("launch_converted_render.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --scene \"${scene}\" --sceneName ${options.sceneName}")
				echo "Preparing results"
				python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}")
				break;
			}   
	    } catch(e) {
			currentBuild.result = 'FAILURE'
			print e
			echo "Error while render"
	    } finally {
			String stashName = osName + "_" + gpuName + "_" + uniqueID
			stash includes: 'Output/*', name: stashName, allowEmpty: true
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
	    def nodes = platforms.split(';')
	    int platformCount = nodes.size()
	    int frameStep = 0
	    int frameCount = 0
	    
	    try {

		if (platformCount > 1 && options['startFrame'] != options['endFrame']) {
		    int startFrame = options['startFrame'] as Integer
		    int endFrame = options['endFrame'] as Integer
		    frameCount = endFrame - startFrame + 1
		    if (frameCount % platformCount == 0) {
			frameStep = frameCount / platformCount
		    } else {
			int absFrame = frameCount + (platformCount - frameCount % platformCount)
			frameStep = absFrame / platformCount
		    }
		}
	
		for (i = 0; i < platformCount; i++) {

		    String uniqueID = Integer.toString(i)

		    String item = nodes[i]
		    Map newOptions = options.clone()

		    if (platformCount > 1 && newOptions['startFrame'] != newOptions['endFrame']) {
			if (i != (platformCount - 1)) {
			    newOptions['startFrame'] = Integer.toString(i * frameStep + 1)
			    newOptions['endFrame'] = Integer.toString((i + 1) * frameStep)
			} else {
			    newOptions['startFrame'] = Integer.toString(i * frameStep + 1)
			    newOptions['endFrame'] = Integer.toString(frameCount)
			}
		    }

		    List tokens = item.tokenize(':')
		    String osName = tokens.get(0)
		    String deviceName = tokens.get(1)
		    
		    String renderDevice = ""
		    if (deviceName == "ANY") {
			String tool = options['Tool'].split(':')[0].trim()
			renderDevice = tool
		    } else {
			if (options['RenderDevice'] == "gpu") {
			    renderDevice = "gpu${deviceName}"
			} else {
			    renderDevice = "cpu${deviceName}"
			}
		    }
		    
		    echo "Scheduling Render ${osName}:${deviceName}"
		    testTasks["Test-${osName}-${deviceName}"] = {
			node("${osName} && RenderService && ${renderDevice}")
			{
			    stage("Render-${osName}-${deviceName}")
			    {
				timeout(time: 60, unit: 'MINUTES')
				{
				    ws("WS/${newOptions.PRJ_NAME}_Render") {
					executeRender(osName, deviceName, newOptions, uniqueID)
				    }
				}
			    }
			}
		    }

		}

		parallel testTasks

	    } finally {
		node("Windows && RSReportBuilder")
		{
		    stage("Deploy")
		    {
			timeout(time: 15, unit: 'MINUTES')
			{
			    ws("WS/${options.PRJ_NAME}_Deploy") {
				executeDeploy(nodes, options)
			    }
			}
		    }
		}
	    }
	}    
    }   
    catch (e) {
	println(e.toString());
	println(e.getMessage());
	println(e.getStackTrace());
	currentBuild.result = "FAILED"
	throw e
    }
}
    
def call(String Tool = '',
    String Scene = '',  
    String platforms = '',
    String RenderDevice = '',
    String id = '',
    String Plugin_Link = '',
    String sceneName = '',
    ) {
	String PRJ_ROOT='RenderServiceRenderJob'
	String PRJ_NAME='RenderServiceRenderJob'  
	main(platforms,[
	    enableNotifications:false,
	    PRJ_NAME:PRJ_NAME,
	    PRJ_ROOT:PRJ_ROOT,
	    Tool:Tool,
	    Scene:Scene,
	    RenderDevice:RenderDevice,
	    id:id,
	    Plugin_Link:Plugin_Link,
	    sceneName:sceneName,
	    ])
    }
