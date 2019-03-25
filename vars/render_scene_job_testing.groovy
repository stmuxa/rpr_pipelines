def executeRender(osName, gpuName, Map options, uniqueID) {
	currentBuild.result = 'SUCCESS'
	
	String tool = options['Tool'].split(':')[0].trim()
	String version = options['Tool'].split(':')[1].trim()
	String scene_zip = options['Scene'].split('/')[-1].trim()
	echo "${options}"
	
	timeout(time: 1, unit: 'HOURS') {
	switch(osName) {
		case 'Windows':
			try {
				print("Deleting all files in work path...")
				bat '''
				@echo off
				del /q *
				for /d %%x in (*) do @rd /s /q "%%x"
				'''	
	
				print("Detecting plugin for render ...")
				if (options['Plugin_Link'] == 'default') {
					python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Installing plugin\" --id ${id}")
					plugin_name = "RadeonProRender.msi"
					plugin_tool = tool
					switch(tool) {
						case 'Blender':  
							def exists = fileExists '..\\..\\RenderServiceStorage\\radeonprorenderforblender.msi'
							if (exists) {
								print("Plugin is copying from Render Service Storage on this PC")
								bat """
									copy "..\\..\\RenderServiceStorage\\radeonprorenderforblender.msi" "RadeonProRender.msi"
								"""
							} else {
								print("Plugin will be donwloaded and copied to Render Service Storage on this PC")
								bat """ 
							 		"C:\\JN\\cis_tools\\${options.cis_tools}\\download.bat" "${options.plugin_storage}/radeonprorenderforblender.msi"
								"""
								bat """
									copy "radeonprorenderforblender.msi" "..\\..\\RenderServiceStorage"
								"""
								plugin_name = "radeonprorenderforblender.msi"
							}
							break;
						case 'Maya':  
							def exists = fileExists '..\\..\\RenderServiceStorage\\radeonprorenderformaya.msi'
							if (exists) {
								print("Plugin is copying from Render Service Storage on this PC")
								bat """
									copy "..\\..\\RenderServiceStorage\\radeonprorenderformaya.msi" "RadeonProRender.msi"
								"""
							} else {
								print("Plugin will be donwloaded and copied to Render Service Storage on this PC")
								bat """ 
							 		"C:\\JN\\cis_tools\\${options.cis_tools}\\download.bat" "${options.plugin_storage}/radeonprorenderformaya.msi"
								"""
								bat """
									copy "radeonprorenderformaya.msi" "..\\..\\RenderServiceStorage"
								"""
								plugin_name = "radeonprorenderformaya.msi"
							}
							break;
						case 'Max':  
							def exists = fileExists '..\\..\\RenderServiceStorage\\radeonprorenderformax.msi'
							if (exists) {
								print("Plugin is copying from Render Service Storage on this PC")
								bat """
									copy "..\\..\\RenderServiceStorage\\radeonprorenderformax.msi" "RadeonProRender.msi"
								"""
							} else {
								print("Plugin will be donwloaded and copied to Render Service Storage on this PC")
								bat """ 
							 		"C:\\JN\\cis_tools\\${options.cis_tools}\\download.bat" "${options.plugin_storage}/radeonprorenderformax.msi"
								"""
								bat """
									copy "radeonprorenderformax.msi" "..\\..\\RenderServiceStorage"
								"""
								plugin_name = "radeonprorenderformax.msi"
							}
							break;
						case 'Redshift':  
							def exists = fileExists '..\\..\\RenderServiceStorage\\radeonprorenderformaya.msi'
							if (exists) {
								print("Plugin is copying from Render Service Storage on this PC")
								bat """
									copy "..\\..\\RenderServiceStorage\\radeonprorenderformaya.msi" "RadeonProRender.msi"
								"""
							} else {
								print("Plugin will be donwloaded and copied to Render Service Storage on this PC")
								bat """ 
							 		"C:\\JN\\cis_tools\\${options.cis_tools}\\download.bat" "${options.plugin_storage}/radeonprorenderformaya.msi"
								"""
								bat """
									copy "radeonprorenderformaya.msi" "..\\..\\RenderServiceStorage"
								"""
								plugin_name = "radeonprorenderformaya.msi"
							}
							plugin_tool = "Maya"
							break;
					}
					install_plugin(osName, plugin_tool, plugin_name)
				} else if (options['Plugin_Link'] == 'skip') {
					print("Skip plugin install")
				} else {
					String plugin = options['Plugin_Link'].split("/")[-1]
					print("Downloading ...")
					bat """ 
							 "C:\\JN\\cis_tools\\${options.cis_tools}\\download.bat" "${options.Plugin_Link}"
					"""
					print("Installing ...")
        			install_plugin(osName, tool, plugin)
				}
				
				switch(tool) {
					case 'Blender':  
						
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")

						bat """
						copy "..\\..\\cis_tools\\${options.cis_tools}\\find_scene_blender.py" "."
						copy "..\\..\\cis_tools\\${options.cis_tools}\\blender_render.py" "."
						copy "..\\..\\cis_tools\\${options.cis_tools}\\launch_blender.py" "."
						"""
					
						String scene_exists = python3("..\\..\\cis_tools\\${options.cis_tools}\\check_scene_exists.py --file_name ${options.sceneName} ")
						if (scene_exists == "file_exists") {
							bat """
								copy "..\\..\\RenderServiceStorage\\scenes\\${options.sceneName}" "."
							"""
						} else {
							bat """ 
							"..\\..\\cis_tools\\${options.cis_tools}\\download.bat" "${options.Scene}"
							"""
							bat """
								copy ${options.sceneName} "..\\..\\RenderServiceStorage\\scenes" 
							"""
						}

						if ("${scene_zip}".endsWith('.zip') || "${scene_zip}".endsWith('.7z')) {
							bat """
							"..\\..\\cis_tools\\7-Zip\\7z.exe" x "${scene_zip}"
							"""
							options['sceneName'] = python3("find_scene_blender.py --folder .").split('\\\\')[-1]
						}
						
						String scene=python3("find_scene_blender.py --folder .").split('\r\n')[2].trim()
						echo "Find scene: ${scene}"
						echo "Launching render"
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}")
						python3("launch_blender.py --tool ${version} --render_device_type ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName ${options.sceneName}")
						echo "Done"
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Preparing results\" --id ${id}")
						break;

					case 'Max':
					
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")

						bat """
						copy "..\\..\\cis_tools\\${options.cis_tools}\\find_scene_max.py" "."
						copy "..\\..\\cis_tools\\${options.cis_tools}\\launch_max.py" "."
						copy "..\\..\\cis_tools\\${options.cis_tools}\\max_render.ms" "."
						"""

						bat """ 
						"..\\..\\cis_tools\\${options.cis_tools}\\download.bat" "${options.Scene}"
						"""

						if ("${scene_zip}".endsWith('.zip') || "${scene_zip}".endsWith('.7z')) {
							bat """
							"..\\..\\cis_tools\\7-Zip\\7z.exe" x "${scene_zip}"
							"""
							options['sceneName'] = python3("find_scene_max.py --folder . ").split('\r\n')[2].trim()
						}
						
						String scene=python3("find_scene_max.py --folder . ").split('\r\n')[2].trim()
						echo "Find scene: ${scene}"
						echo "Launching render"
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}")
						python3("launch_max.py --tool ${version} --render_device_type ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName ${options.sceneName}")
						echo "Done."
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Preparing results\" --id ${id}")
						break;

					case 'Maya':
					
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")

						bat """
						copy "..\\..\\cis_tools\\${options.cis_tools}\\find_scene_maya.py" "."
						copy "..\\..\\cis_tools\\${options.cis_tools}\\launch_maya.py" "."
						copy "..\\..\\cis_tools\\${options.cis_tools}\\maya_render.py" "."
						"""

						bat """ 
						"..\\..\\cis_tools\\${options.cis_tools}\\download.bat" "${options.Scene}"
						"""

						if ("${scene_zip}".endsWith('.zip') || "${scene_zip}".endsWith('.7z')) {
							bat """
							"..\\..\\cis_tools\\7-Zip\\7z.exe" x "${scene_zip}"
							"""
							options['sceneName'] = python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
						}
						
						String scene=python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
						echo "Find scene: ${scene}"
						echo "Launching render"
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}")
						python3("launch_maya.py --tool ${version} --render_device_type ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName ${options.sceneName}")
						echo "Done."
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Preparing results\" --id ${id}")
						break;
					
					case 'Redshift':
							
						
						checkOutBranchOrScm('master', 'git@github.com:luxteam/RS2RPRConvertTool.git')
						bat """
						copy "..\\..\\cis_tools\\${options.cis_tools}\\find_scene_maya.py" "."
						copy "..\\..\\cis_tools\\${options.cis_tools}\\launch_redshift_render.py" "."
						copy "..\\..\\cis_tools\\${options.cis_tools}\\launch_converted_render.py" "."
						copy "..\\..\\cis_tools\\${options.cis_tools}\\maya_convert_render.py" "."
						"""
						
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")
						bat """ 
						"..\\..\\cis_tools\\${options.cis_tools}\\download.bat" "${options.Scene}"
						"""

						if ("${scene_zip}".endsWith('.zip') || "${scene_zip}".endsWith('.7z')) {
							bat """
							"..\\..\\cis_tools\\7-Zip\\7z.exe" x "${scene_zip}"
							"""
							options['sceneName'] = python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
						}
						
						String scene=python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
						echo "Find scene: ${scene}"
						echo "Launching conversion and render"
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering Redshift scene\" --id ${id}")
						python3("launch_redshift_render.py --tool ${version} --scene \"${scene}\" --sceneName ${options.sceneName}")
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering converted scene\" --id ${id}")
						python3("launch_converted_render.py --tool ${version} --scene \"${scene}\" --sceneName ${options.sceneName}")
						echo "Done."
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Preparing results\" --id ${id}")
						break;
					
					case 'Core':
							
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Downloading scene\" --id ${id}")

						bat """
						copy "..\\..\\cis_tools\\${options.cis_tools}\\find_scene_core.py" "."
						copy "..\\..\\cis_tools\\${options.cis_tools}\\launch_core_render.py" "."
						"""

						bat """ 
						"..\\..\\cis_tools\\${options.cis_tools}\\download.bat" "${options.Scene}"
						"""

						if ("${scene_zip}".endsWith('.zip') || "${scene_zip}".endsWith('.7z')) {
							bat """
							"..\\..\\cis_tools\\7-Zip\\7z.exe" x "${scene_zip}"
							"""
							options['sceneName'] = python3("find_scene_core.py --folder . ").split('\\\\')[-1]
						}
						
						String scene=python3("find_scene_core.py --folder . ").split('\r\n')[2].trim()
						echo "Find scene: ${scene}"
						echo "Launching render"
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Rendering scene\" --id ${id}")
						python3("launch_core_render.py --tool ${version} --pass_limit ${options.PassLimit} --scene \"${scene}\" --width ${options.width} --height ${options.height} --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName ${options.sceneName}")
						echo "Done."
						python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --tool ${tool} --status \"Preparing results\" --id ${id}")
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

		case 'OSX':

			try {
 
				print("Deleting all files in work path...")
				sh '''
				rm -rf *
				'''
				
				print("Detecting plugin for render ...")
				if (options['Plugin_Link'] == 'default') {
					sh """
						python3 ../../cis_tools/${options.cis_tools}/send_status.py --django_ip "${options.django_url}/" --tool ${tool} --status "Installing plugin" --id ${id}
					"""
					def exists = fileExists '../../RenderServiceStorage/radeonprorenderforblender.dmg'
					if (exists) {
						print("Plugin is copying from Render Service Storage on this PC")
						sh """
							cp "../../RenderServiceStorage/radeonprorenderforblender.dmg" "RadeonProRender.dmg"
						"""
						plugin_name = "RadeonProRender.dmg"
					} else {
						print("Plugin will be donwloaded and copied to Render Service Storage on this PC")
						sh """ 
							 chmod +x "../../cis_tools/${options.cis_tools}/download.sh" 
							 "../../cis_tools/${options.cis_tools}/download.sh" "${options.plugin_storage}/radeonprorenderforblender.dmg"
						"""
						sh """
							cp "radeonprorenderforblender.dmg" "../../RenderServiceStorage"
						"""
						plugin_name = "radeonprorenderforblender.dmg"
					}
					install_plugin(osName, tool, plugin_name)
				} else if (options['Plugin_Link'] == 'skip'){
					print("Skip plugin install")
			    } else {
					String plugin = options['Plugin_Link'].split('/')[-1].trim()
					print("Downloading plugin")
					sh """ 
						chmod +x "../../cis_tools/${options.cis_tools}/download.sh" 
						"../../cis_tools/${options.cis_tools}/download.sh" "${options.Plugin_Link}"
					"""
					plugin = "./" + plugin
					install_plugin(osName, tool, plugin)	
				}
				
				switch(tool) {
					case 'Blender':      

						sh """
						cp "../../cis_tools/RenderSceneJob/find_scene_blender.py" "."
						cp "../../cis_tools/RenderSceneJob/blender_render.py" "."
						cp "../../cis_tools/RenderSceneJob/launch_blender.py" "."
						"""

						sh """
							python3 ../../cis_tools/${options.cis_tools}/send_status.py --django_ip "${options.django_url}/" --tool ${tool} --status "Downloading scene" --id ${id}
						"""
					
						sh """ 
							chmod +x "../../cis_tools/RenderSceneJob/download.sh"
							"../../cis_tools/RenderSceneJob/download.sh" "${options.Scene}"
						"""

						if ("${scene_zip}".endsWith('.zip') || "${scene_zip}".endsWith('.7z')) {
							sh """
								7z x "${scene_zip}"
							"""
							options['sceneName'] = sh (returnStdout: true, script: 'python3 find_scene_blender.py --folder .')
							options['sceneName'] = options['sceneName'].trim()
						}
						
						String scene = sh (returnStdout: true, script: 'python3 find_scene_blender.py --folder .')
						scene = scene.trim()
						echo "Find scene: ${scene}"
						sh """
							python3 ../../cis_tools/${options.cis_tools}/send_status.py --django_ip "${options.django_url}/" --tool ${tool} --status "Rendering scene" --id ${id}
						"""
						echo "Launching render"
						sh """
							python3 launch_blender.py --tool ${version} --render_device ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName ${options.sceneName}
						"""
						echo "Done"
						sh """
							python3 ../../cis_tools/${options.cis_tools}/send_status.py --django_ip "${options.django_url}/" --tool ${tool} --status "Preparing results" --id ${id}
						"""
						break;

					case 'Max':
						break;
					case 'Maya':
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

		default:

			try {
						
				print("Deleting all files in work path...")
				sh '''
				rm -rf *
				'''
			 	
				if (options['Plugin_Link'] == 'default') {
					sh """
						python3 ../../cis_tools/${options.cis_tools}/send_status.py --django_ip "${options.django_url}/" --tool ${tool} --status "Downloading scene" --id ${id}
					"""
					def exists = fileExists '../../RenderServiceStorage/radeonprorenderforblender.run'
					if (exists) {
						print("Plugin is copying from Render Service Storage on this PC")
						sh """
							cp "../../RenderServiceStorage/radeonprorenderforblender.run" "radeonprorenderforblender.run"
						"""
					} else {
						print("Plugin will be donwloaded and copied to Render Service Storage on this PC")
						sh """ 
							 chmod +x "../../cis_tools/${options.cis_tools}/download.sh" 
							 "../../cis_tools/${options.cis_tools}/download.sh" "${options.plugin_storage}/radeonprorenderforblender.run"
						"""
						sh """
							cp "radeonprorenderforblender.run" "../../RenderServiceStorage"
						"""
					}
					install_plugin(osName, tool, "./radeonprorenderforblender.run")
				} else if (options['Plugin_Link'] == 'skip'){
					print("Skip plugin install")
				} else {
					String plugin = options['Plugin_Link'].split('/')[-1].trim()
					print("Downloading plugin")
					sh """ 
						chmod +x "../../cis_tools/${options.cis_tools}/download.sh" 
						"../../cis_tools/${options.cis_tools}/download.sh" "${options.Plugin_Link}"
					"""
					plugin = "./" + plugin
					install_plugin(osName, tool, plugin)
			    } 

				switch(tool) {
					case 'Blender':                    
							
						sh """
						cp "../../cis_tools/RenderSceneJob/find_scene_blender.py" "."
						cp "../../cis_tools/RenderSceneJob/blender_render.py" "."
						cp "../../cis_tools/RenderSceneJob/launch_blender.py" "."
						"""
					
						sh """
							python3 ../../cis_tools/${options.cis_tools}/send_status.py --django_ip "${options.django_url}/" --tool ${tool} --status "Downloading scene" --id ${id}
						"""

						sh """ 
							chmod +x "../../cis_tools/RenderSceneJob/download.sh"
							"../../cis_tools/RenderSceneJob/download.sh" "${options.Scene}"
						"""
						
						if ("${scene_zip}".endsWith('.zip') || "${scene_zip}".endsWith('.7z')) {
							sh """
								7z x "${scene_zip}"
							"""
							options['sceneName'] = sh (returnStdout: true, script: 'python3 find_scene_blender.py --folder .')
							options['sceneName'] = options['sceneName'].trim()
						}
						
						String scene = sh (returnStdout: true, script: 'python3 find_scene_blender.py --folder .')
						scene = scene.trim()
						echo "Find scene: ${scene}"
						sh """
							python3 ../../cis_tools/${options.cis_tools}/send_status.py --django_ip "${options.django_url}/" --tool ${tool} --status "Rendering scene" --id ${id}
						"""
						echo "Launching render"
						sh """
							python3 launch_blender.py --tool ${version} --render_device ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene \"${scene}\" --startFrame ${options.startFrame} --endFrame ${options.endFrame} --sceneName ${options.sceneName}
						"""
						echo "Done"
						sh """
							python3 ../../cis_tools/${options.cis_tools}/send_status.py --django_ip "${options.django_url}/" --tool ${tool} --status "Preparing results" --id ${id}
						"""
						break;

					case 'Max':
						break;
					case 'Maya':
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

def install_plugin(osName, tool, plugin) {
	switch(osName) {
		case 'Windows':
			switch(tool) {
				case 'Blender': 
					try {
							powershell"""
							\$uninstall = Get-WmiObject -Class Win32_Product -Filter "Name = 'Radeon ProRender for Blender'"
							if (\$uninstall) {
							Write "Uninstalling..."
							\$uninstall = \$uninstall.IdentifyingNumber
							start-process "msiexec.exe" -arg "/X \$uninstall /qn /quiet /L+ie uninstall.log /norestart" -Wait
							}else{
							Write "Plugin not found"}
							"""
					} catch(e) {
						echo "Error while deinstall plugin"
					}
												
					bat """
							msiexec /i \"${plugin}\" /quiet /qn PIDKEY=${env.RPR_PLUGIN_KEY} /L+ie install.log /norestart
					"""
								
					try {
						bat"""
						echo "Try adding addon from blender" >> install.log
						"""

						bat """
						echo import bpy >> registerRPRinBlender.py
						echo import os >> registerRPRinBlender.py
						echo addon_path = "C:\\Program Files\\AMD\\RadeonProRenderPlugins\\Blender\\\\addon.zip" >> registerRPRinBlender.py
						echo bpy.ops.wm.addon_install(filepath=addon_path) >> registerRPRinBlender.py
						echo bpy.ops.wm.addon_enable(module="rprblender") >> registerRPRinBlender.py
						echo bpy.ops.wm.save_userpref() >> registerRPRinBlender.py
						"C:\\Program Files\\Blender Foundation\\Blender\\blender.exe" -b -P registerRPRinBlender.py >> install.log 2>&1
						"""
					} catch(e) {
						echo "Error during rpr register"
						println(e.toString());
						println(e.getMessage());
					}
					break;
				case 'Maya':
					try
					    {
						powershell"""
						\$uninstall = Get-WmiObject -Class Win32_Product -Filter "Name = 'Radeon ProRender for Autodesk Maya®'"
						if (\$uninstall) {
						Write "Uninstalling..."
						\$uninstall = \$uninstall.IdentifyingNumber
						start-process "msiexec.exe" -arg "/X \$uninstall /qn /quiet /L+ie uninstall.log /norestart" -Wait
						}else{
						Write "Plugin not found"}
						"""
					    }
					    catch(e)
					    {
						echo "Error while deinstall plugin"
						echo e.toString()
						echo e.getMessage()
					    }
					bat """
					msiexec /i ${plugin} /quiet /qn PIDKEY=${env.RPR_PLUGIN_KEY} /L+ie install.log /norestart
					"""
					break;
				case 'Max':
					try
					    {
						powershell"""
						\$uninstall = Get-WmiObject -Class Win32_Product -Filter "Name = 'Radeon ProRender for Autodesk 3ds Max®'"
						if (\$uninstall) {
						Write "Uninstalling..."
						\$uninstall = \$uninstall.IdentifyingNumber
						start-process "msiexec.exe" -arg "/X \$uninstall /qn /quiet /L+ie uninstall.log /norestart" -Wait
						}else{
						Write "Plugin not found"}
						"""
					    }
					    catch(e)
					    {
						echo "Error while deinstall plugin"
						echo e.toString()
					    }
					bat """
					msiexec /i ${plugin} /quiet /qn PIDKEY=${env.RPR_PLUGIN_KEY} /L+ie install.log /norestart
					"""
					break;
			}
			break;

		case 'OSX':
			switch(tool) {
				case 'Blender': 
					sh """
					$CIS_TOOLS/installBlenderPlugin.sh ${plugin} >> install.log 2>&1
					"""			
					break;
				case 'Maya':
					break;
			}
			break;

		default:
			switch(tool) {
				case 'Blender': 
			 		try {
						sh """
							/home/user/.local/share/rprblender/uninstall.py /home/user/Desktop/blender-2.79-linux-glibc219-x86_64/ >> uninstall.log 2>&1
						"""
					} catch(e) {}	             
					sh """
					chmod +x ${plugin}
					printf "${env.RPR_PLUGIN_KEY}\nq\n\ny\ny\n" > input.txt
					"""

					sh """
					#!/bin/bash
					exec 0<input.txt
					exec &>install.log
					${plugin} --nox11 --noprogress ~/Desktop/blender-2.79-linux-glibc219-x86_64 >> install.log
					"""
					break;
			}
	}
}

def executeDeploy(nodes, options) {
	
	try {
		print("Deleting all files in work path...")
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
		String post = python3("..\\..\\cis_tools\\${options.cis_tools}\\send_post.py --django_ip \"${options.django_url}/\" --build_number ${currentBuild.number} --jenkins_job \"${options.jenkins_job}\" --tool ${tool} --status ${currentBuild.result} --id ${id}")
		print post
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
				options['django_url'] = "https://render.cis.luxoft.com/jenkins_post_form/"
				options['plugin_storage'] = "https://render.cis.luxoft.com/media/plugins/"
				options['cis_tools'] = "RenderSceneJob"
				options['jenkins_job'] = "RenderSceneJob"
			} else {
				options['django_url'] = "https://testrender.cis.luxoft.com/jenkins_post_form/"
				options['plugin_storage'] = "https://testrender.cis.luxoft.com/media/plugins/"
				options['cis_tools'] = "RenderSceneJob_Test"
				options['jenkins_job'] = "RenderSceneJob_Testing"
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
				node("Windows && ReportBuilder")
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
	String PassLimit = '',
	String RenderDevice = '',
	String id = '',
	String Plugin_Link = '',
	String startFrame = '',
	String endFrame = '',
	String sceneName = '',
	String width = '',
	String height = ''
	) {
		String PRJ_ROOT='Render_Scene'
		String PRJ_NAME='Render_Scene'	
		main(platforms,[
			enableNotifications:false,
			PRJ_NAME:PRJ_NAME,
			PRJ_ROOT:PRJ_ROOT,
			Tool:Tool,
			Scene:Scene,
			PassLimit:PassLimit,
			RenderDevice:RenderDevice,
			id:id,
			Plugin_Link:Plugin_Link,
			startFrame:startFrame,
			endFrame:endFrame,
			sceneName:sceneName,
			width:width,
			height:height])
	}
