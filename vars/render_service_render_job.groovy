def executeRender(osName, gpuName, Map options) {
    currentBuild.result = 'SUCCESS'
    
    String tool = options['Tool'].split(':')[0].trim()
    String version = options['Tool'].split(':')[1].trim()
    String scene_name = options['Scene'].split('/')[-1].trim()
    
	timeout(time: 65, unit: 'MINUTES') {
		switch(osName) {
			case 'Windows':
				try {
					// Clean up work folder
					bat '''
						@echo off
						del /q *
						for /d %%x in (*) do @rd /s /q "%%x"
					''' 
					try {
						// download scene, check if it is already downloaded
						print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}"))
						def exists = fileExists "..\\..\\RenderServiceStorage\\${scene_name}"
						if (exists) {
							print("Scene is copying from Render Service Storage on this PC")
							bat """
								copy "..\\..\\RenderServiceStorage\\${scene_name}" "${scene_name}"
							"""
						} else {
							bat """ 
								wget --no-check-certificate "${options.Scene}"
							"""
							bat """
								copy "${scene_name}" "..\\..\\RenderServiceStorage"
							"""
						}
					} catch(e) {
						print("Error while downloading")
					}
					
					switch(tool) {
						case 'Blender':  
							// copy necessary scripts for render
							bat """
								copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_blender.py" "."
								copy "${CIS_TOOLS}\\${options.cis_tools}\\blender_render.py" "."
								copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_blender.py" "."
							"""
						
							// unzip
							if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
								bat """
									7z x "${scene_name}"
								"""
								options['sceneName'] = python3("find_scene_blender.py --folder .").split('\r\n')[2].trim()
							}
							// Launch render
							String scene=python3("find_scene_blender.py --folder .").split('\r\n')[2].trim()
							echo "Find scene: ${scene}"
							print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}"))
							python3("launch_blender.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --min_samples ${options.Min_Samples} --max_samples ${options.Max_Samples} --noise_threshold ${options.Noise_threshold} --width ${options.Width} --height ${options.Height} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName \"${options.sceneName}\" ")
							print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}"))
							break;

						case 'Max':
							// copy necessary scripts for render	
							bat """
								copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_max.py" "."
								copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_max.py" "."
								copy "${CIS_TOOLS}\\${options.cis_tools}\\max_render.ms" "."
							"""
							// unzip
							if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
								bat """
									7z x "${scene_name}"
								"""
								options['sceneName'] = python3("find_scene_max.py --folder . ").split('\r\n')[2].trim()
							}
							// Launch render
							String scene=python3("find_scene_max.py --folder . ").split('\r\n')[2].trim()
							echo "Find scene: ${scene}"
							print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}"))
							python3("launch_max.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --min_samples ${options.Min_Samples} --max_samples ${options.Max_Samples} --noise_threshold ${options.Noise_threshold} --width ${options.Width} --height ${options.Height} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName \"${options.sceneName}\" ")
							print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}"))
							break;

						case 'Maya':
							// copy necessary scripts for render	
							bat """
								copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_maya.py" "."
								copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_maya.py" "."
								copy "${CIS_TOOLS}\\${options.cis_tools}\\maya_render.py" "."
							"""
							// unzip
							if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
								bat """
									7z x "${scene_name}"
								"""
								options['sceneName'] = python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
							}
						    // Launch render
							String scene=python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
							echo "Find scene: ${scene}"
							print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}"))
							python3("launch_maya.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --min_samples ${options.Min_Samples} --max_samples ${options.Max_Samples} --noise_threshold ${options.Noise_threshold} --width ${options.Width} --height ${options.Height} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName \"${options.sceneName}\" ")
							print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}"))
							break;

						case 'Maya (Redshift)':
							// copy necessary scripts for render	
							bat """
								copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_maya.py" "."
								copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_maya_redshift.py" "."
								copy "${CIS_TOOLS}\\${options.cis_tools}\\render_func.mel" "."
							"""
							// unzip
							if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
								bat """
									7z x "${scene_name}"
								"""
								options['sceneName'] = python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
							}
							// Launch render
							String scene=python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
							echo "Find scene: ${scene}"
							print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool \"${tool}\" --status \"Rendering scene\" --id ${id}"))
							python3("launch_maya_redshift.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --min_samples ${options.Min_Samples} --max_samples ${options.Max_Samples} --noise_threshold ${options.Noise_threshold} --width ${options.Width} --height ${options.Height} --scene \"${scene}\" --sceneName \"${options.sceneName}\" ")
							print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool \"${tool}\" --status \"Completed\" --id ${id}"))
							break;
		    
						case 'Core':
							// copy necessary scripts for render	
							bat """
								copy "${CIS_TOOLS}\\${options.cis_tools}\\find_scene_core.py" "."
								copy "${CIS_TOOLS}\\${options.cis_tools}\\launch_core_render.py" "."					
							"""
							// unzip
							if ("${scene_name}".endsWith('.zip') || "${scene_name}".endsWith('.7z')) {
								bat """
									7z x "${scene_name}"
								"""
								options['sceneName'] = python3("find_scene_core.py --folder . ").split('\r\n')[2].trim()
							}
							// Launch render
							String scene=python3("find_scene_core.py --folder . ").split('\r\n')[2].trim()
							echo "Find scene: ${scene}"
							print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}"))
							python3("launch_core_render.py --tool ${version} --django_ip \"${options.django_url}/\" --id ${id} --pass_limit ${options.PassLimit} --scene \"${scene}\" --width ${options.width} --height ${options.height} --startFrame ${options.startFrame} --endFrame ${options.endFrame} --gpu \"${options.gpu}\" --sceneName \"${options.sceneName}\" ")
							print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Completed\" --id ${id}"))
							break;

						}   
				} catch(e) {
					currentBuild.result = 'FAILURE'
					print e
				} finally {
					archiveArtifacts 'Output/*'
					print(python3("${CIS_TOOLS}\\${options.cis_tools}\\send_render_results.py --django_ip \"${options.django_url}/\" --build_number ${currentBuild.number} --jenkins_job \"${options.jenkins_job}\" --tool \"${tool}\" --status ${currentBuild.result} --id ${id}"))
				}
			  	break;
		}
    }
}

def main(String PCs, Map options) {

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
		List tokens = PCs.tokenize(':')
		String osName = tokens.get(0)
		String deviceName = tokens.get(1)
		String renderDevice = "gpu${deviceName}"
		
		try {
			echo "Scheduling Render ${osName}:${deviceName}"
			testTasks["Render-${osName}-${deviceName}"] = {
				node("${osName} && RenderService && ${renderDevice}") {
					stage("Render") {
						timeout(time: 65, unit: 'MINUTES') {
							ws("WS/${options.PRJ_NAME}_Render") {
								executeRender(osName, deviceName, options)
							}
						}
					}
				}
			}

			parallel testTasks
		    
	    } catch(e) {
			println(e.toString());
			println(e.getMessage());
			println(e.getStackTrace());
			currentBuild.result = "FAILED"
			print e
	    } 
	}    
    
}
    
def call(String PCs = '',
    String id = '',
    String Tool = '',
    String Scene = '',  
    String sceneName = '',
    String Min_samples = '',
    String Max_samples = '',
    String Noise_threshold = '',
    String startFrame = '',
    String endFrame = '',
    String Width = '',
    String Height = ''
    ) {
	String PRJ_ROOT='RenderServiceRenderJob'
	String PRJ_NAME='RenderServiceRenderJob'  
	main(PCs,[
	    enableNotifications:false,
	    PRJ_NAME:PRJ_NAME,
	    PRJ_ROOT:PRJ_ROOT,
	    id:id,
	    Tool:Tool,
	    Scene:Scene,
	    sceneName:sceneName,
	    Min_Samples:Min_Samples,
	    Max_Samples:Max_Samples,
	    Noise_threshold:Noise_threshold,
	    startFrame:startFrame,
	    endFrame:endFrame,
	    Width:Width,
	    Height:Height
	    ])
    }
