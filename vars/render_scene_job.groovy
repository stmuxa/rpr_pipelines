def executeRender(osName, Map options) {
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
            if (options['Plugin'] != 'Skip') {
                  String plugin = options['Plugin'].split('/')[-1].trim()
                  String status = python3("..\\..\\cis_tools\\RenderSceneJob\\check_installer.py --plugin_md5 \"${options.md5}\" --folder . ").split('\r\n')[2].trim()
                  print("STATUS: ${status}")
                  if (status == "DOWNLOAD_COPY") {
                          print("Plugin will be downloaded and copied to Render Service Storage on this PC")
                          bat """ 
                               "C:\\JN\\cis_tools\\RenderSceneJob\\download.bat" "${options.Plugin}"
                          """
                          bat """
                            copy "${plugin}" "..\\..\\RenderServiceStorage"
                          """
                   } else if (status == "ONLY_DOWNLOAD") {
                          print("Plugin will be only downloaded, because there are no free space on PC")
                          bat """ 
                               "C:\\JN\\cis_tools\\RenderSceneJob\\download.bat" "${options.Plugin}"
                          """
                   } else {
                          print("Plugin is copying from Render Service Storage on this PC")
                          bat """
                            copy "${status}" "." 
                          """
                    }
               } else {
                    print("Plugin installation skipped!")
               }
        
            switch(tool) {
              case 'Blender':  
              
                      if (options['Plugin'] != 'Skip') {
                          String plugin = "${status}"
                          plugin = plugin.split('/')[-1].trim()
                          try
                            {
                              powershell"""
                              \$uninstall = Get-WmiObject -Class Win32_Product -Filter "Name = 'Radeon ProRender for Blender'"
                              if (\$uninstall) {
                              Write "Uninstalling..."
                              \$uninstall = \$uninstall.IdentifyingNumber
                              start-process "msiexec.exe" -arg "/X \$uninstall /qn /quiet /L+ie uninstall.log /norestart" -Wait
                              }else{
                              Write "Plugin not found"}
                              """
                            }
                        catch(e) {
                                echo "Error while deinstall plugin"
                             }
                        
              
                          bat """
                              msiexec /i ${plugin} /quiet /qn PIDKEY=${env.RPR_PLUGIN_KEY} /L+ie install.log /norestart
                              """
                
                          try {
                                bat"""
                                echo "Try adding addon from blender" >>install.log
                                """

                                bat """
                                echo import bpy >> registerRPRinBlender.py
                                echo import os >> registerRPRinBlender.py
                                echo addon_path = "C:\\Program Files\\AMD\\RadeonProRenderPlugins\\Blender\\\\addon.zip" >> registerRPRinBlender.py
                                echo bpy.ops.wm.addon_install(filepath=addon_path) >> registerRPRinBlender.py
                                echo bpy.ops.wm.addon_enable(module="rprblender") >> registerRPRinBlender.py
                                echo bpy.ops.wm.save_userpref() >> registerRPRinBlender.py
                                "C:\\Program Files\\Blender Foundation\\Blender\\blender.exe" -b -P registerRPRinBlender.py >>.nstall.log 2>&1
                                """
                          } catch(e) {
                              echo "Error during rpr register"
                              println(e.toString());
                              println(e.getMessage());
                          }
                      }
                      bat """ 
                      "C:\\JN\\cis_tools\\RenderSceneJob\\download.bat" "${options.Scene}"
                      """
                      bat """
                      "C:\\JN\\cis_tools\\7-Zip\\7z.exe" x "${scene_zip}"
                      """
                      bat """
                      copy "..\\..\\cis_tools\\RenderSceneJob\\find_scene_blender.py" "."
                      copy "..\\..\\cis_tools\\RenderSceneJob\\blender_render.py" "."
                      copy "..\\..\\cis_tools\\RenderSceneJob\\launch_blender.py" "."
                      """
                      String scene=python3("find_scene_blender.py --folder .").split('\r\n')[2].trim()
                      echo "Find scene: ${scene}"
                      echo "Launching render"
                          python3("launch_blender.py --tool ${version} --render_device ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene \"${scene}\"")
                      echo "Done"
                      break;
              case 'Max':
                      bat """ 
                      "C:\\JN\\cis_tools\\RenderSceneJob\\download.bat" "${options.Scene}"
                      """
                      bat """
                      "C:\\JN\\cis_tools\\7-Zip\\7z.exe" x "${scene_zip}"
                      """
                      bat """
                      copy "..\\..\\cis_tools\\RenderSceneJob\\find_scene_max.py" "."
                      copy "..\\..\\cis_tools\\RenderSceneJob\\launch_max.py" "."
                      copy "..\\..\\cis_tools\\RenderSceneJob\\max_render.ms" "."
                      """
                      String scene=python3("find_scene_max.py --folder . ").split('\r\n')[2].trim()
                      echo "Find scene: ${scene}"
                      echo "Launching render"
                      python3("launch_max.py --tool ${version} --scene ${scene} --render_device ${options.RenderDevice} --pass_limit ${options.PassLimit}")
                      echo "Done."
                      break;
              case 'Maya':
                      bat """ 
                      "C:\\JN\\cis_tools\\RenderSceneJob\\download.bat" "${options.Scene}"
                      """
                      bat """
                      "C:\\JN\\cis_tools\\7-Zip\\7z.exe" x "${scene_zip}"
                      """
                      bat """
                      copy "..\\..\\cis_tools\\RenderSceneJob\\find_scene_maya.py" "."
                      copy "..\\..\\cis_tools\\RenderSceneJob\\launch_maya.py" "."
                      copy "..\\..\\cis_tools\\RenderSceneJob\\maya_render.mel" "."
                      """
                      String scene=python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
                      echo "Find scene: ${scene}"
                      echo "Launching render"
                      python3("launch_maya.py --tool ${version} --scene \"${scene}\" --render_device ${options.RenderDevice} --pass_limit ${options.PassLimit}")
                      echo "Done."
                      break;
                      } 
            
            }
            catch (hudson.AbortException e) {
                print e
             }
            catch(e) {
                currentBuild.result = 'FAILURE'
                print e
                echo "Error while render"
            }
            finally {
              archiveArtifacts "Output/*"
              String post = python3("..\\..\\cis_tools\\RenderSceneJob\\send_post.py --django_ip \"https://render.cis.luxoft.com/jenkins_post_form/\" --build_number ${currentBuild.number} --status ${currentBuild.result} --id ${id}")
              print post
            }
     break;
    case 'OSX':
      try {
 
            print("Deleting all files in work path...")
            sh '''
            rm -rf *
            '''
            
            print("Detecting plugin for render ...")
            if (options['Plugin'] != 'Skip') {
                  String plugin = options['Plugin'].split('/')[-1].trim()
                  String status = sh (returnStdout: true, script:
                    "python3 ../../cis_tools/RenderSceneJob/check_installer.py --plugin_md5 ${options.md5} --folder ."
                   ).split('\r\n')[0].trim()
                  print("STATUS: ${status}")
                  if (status == "DOWNLOAD_COPY") {
                          print("Plugin will be downloaded and copied to Render Service Storage on this PC")
                          sh """ 
                              chmod +x "../../cis_tools/RenderSceneJob/download.sh" 
                              "../../cis_tools/RenderSceneJob/download.sh" "${options.Plugin}"
                          """
                          sh """
                            cp "${plugin}" "../../RenderServiceStorage"
                          """
                   } else if (status == "ONLY_DOWNLOAD") {
                          print("Plugin will be only downloaded, because there are no free space on PC")
                          sh """ 
                              chmod +x "../../cis_tools/RenderSceneJob/download.sh" 
                              "../../cis_tools/RenderSceneJob/download.sh" "${options.Plugin}"
                          """
                   } else {
                          print("Plugin is copying from Render Service Storage on this PC")
                          sh """
                            cp "${status}" "." 
                          """
                    }
               } else {
                    print("Plugin installation skipped!")
               }
        
            switch(tool) {
              case 'Blender':      
              
                      if (options['Plugin'] != 'Skip') {
                            String plugin = options['Plugin'].split('/')[-1].trim()
                            sh'''
                            $CIS_TOOLS/installBlenderPlugin.sh ./${plugin} >> install.log 2>&1
                            '''
                      }
              
                      sh """ 
                      chmod +x "../../cis_tools/RenderSceneJob/download.sh"
                      "../../cis_tools/RenderSceneJob/download.sh" "${options.Scene}"
                      """
                      sh """
                      unzip "${scene_zip}" -d .
                      """
                      sh """
                      cp "../../cis_tools/RenderSceneJob/find_scene_blender.py" "."
                      cp "../../cis_tools/RenderSceneJob/blender_render.py" "."
                      cp "../../cis_tools/RenderSceneJob/launch_blender.py" "."
                      """
                      String scene = sh (returnStdout: true, script: 'python3 find_scene_blender.py --folder .')
                      scene = scene.trim()
                      echo "Find scene: ${scene}"
                      echo "Launching render"
                      sh """
                        python3 launch_blender.py --tool ${version} --render_device ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene \"${scene}\"
                        """
                      echo "Done"
                      break;
              case 'Max':
                      break;
              case 'Maya':
                      sh """ 
                      "../../cis_tools/RenderSceneJob/download.sh" "${options.Scene}"
                      """
                      sh """
                      unzip "${scene_zip}" -d .
                      """
                      sh """
                      cp "../../cis_tools/RenderSceneJob/find_scene_maya.py" "."
                      cp "../../cis_tools/RenderSceneJob/maya_render.mel" "."
                      cp "../../cis_tools/RenderSceneJob/launch_maya.py" "."
                      """
                      String scene = sh (returnStdout: true, script: 'python3 find_scene_maya.py --folder .')
                      echo "Find scene: ${scene}"
                      echo "Launching render"
                      sh """
                        python3 launch_maya.py --tool ${version} --render_device ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene "${scene}"
                        """
                      echo "Done"
                      break;
                      }  
                
            }
            catch (hudson.AbortException e) {
                print e
             }
            catch(e) {
                currentBuild.result = 'FAILURE'
                print e
                echo "Error while render"
            }
            finally {
              archiveArtifacts "Output/*"
              sh """
               python3 "../../cis_tools/RenderSceneJob/send_post.py" --django_ip \"https://render.cis.luxoft.com//jenkins_post_form/\" --build_number ${currentBuild.number} --status ${currentBuild.result} --id ${id}
              """
            }
      break;
    default:
      try {
            
            print("Deleting all files in work path...")
            sh '''
            rm -rf *
            '''
       
            print("Detecting plugin for render ...")
            if (options['Plugin'] != 'Skip') {
                  String plugin = options['Plugin'].split('/')[-1].trim()
                  String status = sh (returnStdout: true, script:
                    "python3 ../../cis_tools/RenderSceneJob/check_installer.py --plugin_md5 ${options.md5} --folder ."
                   ).split('\r\n')[0].trim()
                  print("STATUS: ${status}")
                  if (status == "DOWNLOAD_COPY") {
                          print("Plugin will be downloaded and copied to Render Service Storage on this PC")
                          sh """ 
                              chmod +x "../../cis_tools/RenderSceneJob/download.sh" 
                              "../../cis_tools/RenderSceneJob/download.sh" "${options.Plugin}"
                          """
                          sh """
                            cp "${plugin}" "../../RenderServiceStorage"
                          """
                   } else if (status == "ONLY_DOWNLOAD") {
                          print("Plugin will be only downloaded, because there are no free space on PC")
                          sh """ 
                              chmod +x "../../cis_tools/RenderSceneJob/download.sh" 
                              "../../cis_tools/RenderSceneJob/download.sh" "${options.Plugin}"
                          """
                   } else {
                          print("Plugin is copying from Render Service Storage on this PC")
                          sh """
                            cp "${status}" "." 
                          """
                    }
               } else {
                    print("Plugin installation skipped!")
               }
        
            switch(tool) {
              case 'Blender':                    
              
                      if (options['Plugin'] != 'Skip') {
                          String plugin = options['Plugin'].split('/')[-1].trim()

                         try
                            {
                                sh"""
                                  /home/user/.local/share/rprblender/uninstall.py /home/user/Desktop/blender-2.79-linux-glibc219-x86_64/ >> uninstall.log 2>&1
                                  """
                         }catch(e)
                              {}             
                          sh """
                          chmod +x ${plugin}
                          printf "${env.RPR_PLUGIN_KEY}\nq\n\ny\ny\n" > input.txt
                          """

                          sh """
                          #!/bin/bash
                          exec 0<input.txt
                          exec &>install.log
                          ./${plugin} --nox11 --noprogress ~/Desktop/blender-2.79-linux-glibc219-x86_64 >> install.log
                          """
                      }
              
              
              
                      sh """ 
                      chmod +x "../../cis_tools/RenderSceneJob/download.sh"
                      "../../cis_tools/RenderSceneJob/download.sh" "${options.Scene}"
                      """
                      sh """
                      unzip "${scene_zip}" -d .
                      """
                      sh """
                      cp "../../cis_tools/RenderSceneJob/find_scene_blender.py" "."
                      cp "../../cis_tools/RenderSceneJob/blender_render.py" "."
                      cp "../../cis_tools/RenderSceneJob/launch_blender.py" "."
                      """
                      String scene = sh (returnStdout: true, script: 'python3 find_scene_blender.py --folder .')
                      scene = scene.trim()
                      echo "Find scene: ${scene}"
                      echo "Launching render"
                      sh """
                        python3 launch_blender.py --tool ${version} --render_device ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene \"${scene}\"
                      """
                      echo "Done"
                      break;
              case 'Max':
                      break;
              case 'Maya':
                      break;
                      }    
            }
            catch (hudson.AbortException e) {
                print e
             }
            catch(e) {
                currentBuild.result = 'FAILURE'
                print e
                echo "Error while render"
            }
            finally {
              archiveArtifacts "Output/*"
              sh """
               python3 "../../cis_tools/RenderSceneJob/send_post.py" --django_ip \"https://render.cis.luxoft.com//jenkins_post_form/\" --build_number ${currentBuild.number} --status ${currentBuild.result} --id ${id}
              """
            }
      break;
  }
  }
}

def executePlatform(String osName, String gpuNames, Map options)
{
    def retNode =  
    {   
        try {
            
            if(gpuNames)
            {
                def testTasks = [:]
                gpuNames.split(',').each()
                {
                    String asicName = it
                    echo "Scheduling Test ${osName}:${asicName}"

                    testTasks["Test-${it}-${osName}"] = {
                        node("${osName} && Tester && OpenCL && gpu${asicName}")
                        {
                            stage("Test-${asicName}-${osName}")
                            {
                                ws("WS/${options.PRJ_NAME}_Test") {
                                    Map newOptions = options.clone()
                                    newOptions['testResultsName'] = "testResult-${asicName}-${osName}"
                                    executeRender(osName, newOptions)
                                }
                            }
                        }
                    }
                }
                parallel testTasks
            }
        }
        catch (e) {
            println(e.toString());
            println(e.getMessage());
            println(e.getStackTrace());        
            currentBuild.result = "FAILED"
            echo "FAILED by executePlatform"
            throw e
        }
    }
    return retNode
}

def main(String platforms, Map options) {
    
    try {
        properties([[$class: 'BuildDiscarderProperty', strategy: 
                     [$class: 'LogRotator', artifactDaysToKeepStr: '', 
                      artifactNumToKeepStr: '50', daysToKeepStr: '', numToKeepStr: '']]]);
        
        timestamps {
            String PRJ_PATH="${options.PRJ_ROOT}/${options.PRJ_NAME}"
            String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
            options['PRJ_PATH']="${PRJ_PATH}"
            options['JOB_PATH']="${JOB_PATH}"

            def platformList = [];
            def testResultList = [];

            def tasks = [:]

            platforms.split(';').each()
            {

                List tokens = it.tokenize(':')
                String osName = tokens.get(0)
                String gpuNames = ""
                if (tokens.size() > 1)
                {
                    gpuNames = tokens.get(1)
                }

                platformList << osName
                if(gpuNames)
                {
                    gpuNames.split(',').each()
                    {
                        String asicName = it
                        testResultList << "testResult-${asicName}-${osName}"
                    }
                }

                tasks[osName]=executePlatform(osName, gpuNames, options)
            }
            parallel tasks
        }     
      }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        println(e.getStackTrace());
        currentBuild.result = "FAILED"
        throw e
    }
    finally {

        echo "enableNotifications = ${options.enableNotifications}"
        if("${options.enableNotifications}" == "true")
        {
            sendBuildStatusNotification(currentBuild.result, 
                                        options.get('slackChannel', ''), 
                                        options.get('slackBaseUrl', ''),
                                        options.get('slackTocken', ''),
                                        options.CBR)
        }
    }
}
  
def call(String Tool = '',
         String Scene = '',	
         String platforms = '',
         String PassLimit = '',
         String RenderDevice = 'gpu',
         String id = '',
         String Plugin = '',
         String md5 = ''
         ) {
  
    String PRJ_ROOT='Render_Scene'
    String PRJ_NAME='Render_Scene'
      
    main(platforms,
                   [
                    enableNotifications:false,
                    PRJ_NAME:PRJ_NAME,
                    PRJ_ROOT:PRJ_ROOT,
                    Tool:Tool,
                    Scene:Scene,
                    PassLimit:PassLimit,
                    RenderDevice:RenderDevice,
                    id:id,
                    Plugin:Plugin,
                    md5:md5])
}
