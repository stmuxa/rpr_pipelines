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
		    case 'Blender':  
			
				bat """
					copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_blender.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\blender_render.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_blender.py" "."
				"""
			    
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")
				bat """ 
					wget --no-check-certificate "${options.Scene}"
				"""

				if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
				    bat """
				    	7z x "${scene_name}"
				    """
				    options['sceneName'] = python3("find_scene_blender.py --folder .").split('\r\n')[2].trim()
				}
				
				String scene=python3("find_scene_blender.py --folder .").split('\r\n')[2].trim()
				echo "Find scene: ${scene}"
				echo "Launching render"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}")
				python3("launch_blender.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --min_samples ${options.Min_Samples} --max_samples ${options.Max_Samples} --noise_threshold ${options.Noise_threshold} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName \"${options.sceneName}\" ")
				echo "Preparing results"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}")
				break;

		    case 'Max':
		    
				bat """
					copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_max.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_max.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\max_render.ms" "."
				"""
			
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")
				bat """ 
					wget --no-check-certificate "${options.Scene}"
				"""
				
				if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
				    bat """
				    	7z x "${scene_name}"
				    """
				    options['sceneName'] = python3("find_scene_max.py --folder . ").split('\r\n')[2].trim()
				}
				
				String scene=python3("find_scene_max.py --folder . ").split('\r\n')[2].trim()
				echo "Find scene: ${scene}"
				echo "Launching render"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}")
				python3("launch_max.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --render_device_type ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName \"${options.sceneName}\" ")
				echo "Preparing results"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}")
				break;

		    case 'Maya':
		    
				bat """
					copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_maya.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_maya.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\maya_render.py" "."
				"""

				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")
				bat """ 
					wget --no-check-certificate "${options.Scene}"
				"""

				if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
				    bat """
				    	7z x "${scene_name}"
				    """
				    options['sceneName'] = python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
				}
				
				String scene=python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
				echo "Find scene: ${scene}"
				echo "Launching render"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}")
				python3("launch_maya.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --min_samples ${options.Min_Samples} --max_samples ${options.Max_Samples} --noise_threshold ${options.Noise_threshold} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName \"${options.sceneName}\" ")
				echo "Preparing results"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}")
				break;
			
		    case 'Maya (Redshift)':
		    
				bat """
					copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_maya.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_maya_redshift.py" "."
				"""

				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool \"${tool}\" --status \"Downloading scene\" --id ${id}")
				bat """ 
					wget --no-check-certificate "${options.Scene}"
				"""

				if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
				    bat """
				    	7z x "${scene_name}"
				    """
				    options['sceneName'] = python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
				}
				
				String scene=python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
				echo "Find scene: ${scene}"
				echo "Launching render"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool \"${tool}\" --status \"Rendering scene\" --id ${id}")
				python3("launch_maya.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --min_samples ${options.Min_Samples} --max_samples ${options.Max_Samples} --noise_threshold ${options.Noise_threshold} --scene \"${scene}\" --sceneName \"${options.sceneName}\" ")
				echo "Preparing results"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool \"${tool}\" --status \"Completed\" --id ${id}")
				break;
		    
		    case 'Core':
				    
				bat """
					copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_core.py" "."
					copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_core_render.py" "."					
				"""

				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")
				bat """ 
					wget --no-check-certificate "${options.Scene}"
				"""

				if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
				    bat """
				    	7z x "${scene_name}"
				    """
				    options['sceneName'] = python3("find_scene_core.py --folder . ").split('\r\n')[2].trim()
				}
				
				String scene=python3("find_scene_core.py --folder . ").split('\r\n')[2].trim()
				echo "Find scene: ${scene}"
				echo "Launching render"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}")
				python3("launch_core_render.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --pass_limit ${options.PassLimit} --scene \"${scene}\" --width ${options.width} --height ${options.height} --startFrame ${options.startFrame} --endFrame ${options.endFrame} --gpu \"${options.gpu}\" --sceneName \"${options.sceneName}\" ")
				echo "Preparing results"
				python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}")
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

def executeDeploy(nodes, options) {
    
    try {
		print("Clean up work folder")
		bat '''
			@echo off
			del /q *
			for /d %%x in (*) do @rd /s /q "%%x"
		''' 
		bat '''
		    mkdir Output
		'''
		    
		int platformCount = nodes.size()
		for (i = 0; i < platformCount; i++) {
		    String uniqueID = Integer.toString(i)
		    String item = nodes[i]
		    List tokens = item.tokenize(':')
		    String osName = tokens.get(0)
		    String gpuName = tokens.get(1)
		    String stashName = osName + "_" + gpuName + "_" + uniqueID
		    dir(stashName) {
			unstash stashName
		    }
		    bat """
			echo "${stashName}"
			move ${stashName}\\Output\\*.* "Output\\"
		    """
		}
    } catch(e) {
		currentBuild.result = 'FAILURE'
		print e
		echo "No results."
    } finally {
		String tool = options['Tool'].split(':')[0].trim()
		archiveArtifacts 'Output/*'
		String post = python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_results.py --django_ip \"${options.django_url}/\" --build_number ${currentBuild.number} --jenkins_job \"${options.jenkins_job}\" --tool \"${tool}\" --status ${currentBuild.result} --id ${id}")
		print post
    }
}


def main(String PCs, Map options) {

    try {

	timestamps {
	    String PRJ_PATH="${options.PRJ_ROOT}/${options.PRJ_NAME}"
	    String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
	    options['PRJ_PATH']="${PRJ_PATH}"
	    options['JOB_PATH']="${JOB_PATH}"

	    boolean PRODUCTION = true

	    if (PRODUCTION) {
		options['django_url'] = "https://render.cis.luxoft.com/render/jenkins/"
		options['plugin_storage'] = "https://render.cis.luxoft.com/media/plugins/"
		options['cis_tools'] = "RenderServiceScripts"
		options['jenkins_job'] = "RenderServiceRenderJob"
	    } else {
		options['django_url'] = "https://testrender.cis.luxoft.com/render/jenkins/"
		options['plugin_storage'] = "https://testrender.cis.luxoft.com/media/plugins/"
		options['cis_tools'] = "RenderServiceScripts"
		options['jenkins_job'] = "RenderServiceRenderJob"
	    }


	    def testTasks = [:]
	    def nodes = PCs.split(';')
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
		    echo "2.7"
		    String renderDevice = ""
		    if (deviceName == "ANY") {
			String tool = options['Tool'].split(':')[0].trim()
			renderDevice = tool
		    } else {
			renderDevice = "gpu${deviceName}"
		    }
		    echo "3"
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
		    
	    } catch(e) {
		currentBuild.result = 'FAILURE'
		print e
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
    String PCs = '',
    String Min_samples = '',
    String Max_samples = '',
    String Noise_threshold = '',
    String id = '',
    String startFrame = '',
    String endFrame = '',
    String sceneName = '',
    String width = '',
    String height = ''
    ) {
	String PRJ_ROOT='RenderServiceRenderJob'
	String PRJ_NAME='RenderServiceRenderJob'  
	main(PCs,[
	    enableNotifications:false,
	    PRJ_NAME:PRJ_NAME,
	    PRJ_ROOT:PRJ_ROOT,
	    Tool:Tool,
	    Scene:Scene,
	    Min_Samples:Min_Samples,
	    Max_Samples:Max_Samples,
	    Noise_threshold:Noise_threshold,
	    id:id,
	    startFrame:startFrame,
	    endFrame:endFrame,
	    sceneName:sceneName,
	    width:width,
	    height:height
	    ])
    }
