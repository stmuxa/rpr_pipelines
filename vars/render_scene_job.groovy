def executeRender(osName, Map options) {
  
  timeout(time: 1, unit: 'HOURS') {
  switch(osName) {
    case 'Windows':
      try {
            bat '''
            @echo off
            DEL /F /S /Q *
            '''
            String tool = options['Tool'].split(':')[0].trim()
            String version = options['Tool'].split(':')[1].trim()
            echo "${options}"
            switch(tool) {
              case 'Blender':                    
                      bat """ 
                      "C:\\JN\\cis_tools\\RenderSceneJob\\download.bat" "${options.Scene}"
                      """
                      bat """
                      "C:\\JN\\cis_tools\\7-Zip\\7z.exe" x "scene.zip"
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
                      "C:\\JN\\cis_tools\\7-Zip\\7z.exe" x "scene.zip"
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
                      "C:\\JN\\cis_tools\\7-Zip\\7z.exe" x "scene.zip"
                      """
                      bat """
                      copy "..\\..\\cis_tools\\RenderSceneJob\\find_scene_maya.py" "."
                      copy "..\\..\\cis_tools\\RenderSceneJob\\launch_maya.py" "."
                      copy "..\\..\\cis_tools\\RenderSceneJob\\maya_render.mel" "."
                      """
                      String scene=python3("find_scene_maya.py --folder . ").split('\r\n')[2].trim()
                      echo "Find scene: ${scene}"
                      echo "Launching render"
                      python3("launch_maya.py --tool ${version} --scene ${scene} --render_device ${options.RenderDevice} --pass_limit ${options.PassLimit}")
                      echo "Done."
                      break;
                      }    
            }
            catch(e) {
                echo "Error while render"
            }
            finally {
              archiveArtifacts "Output/*"
            }
     break;
    case 'OSX':
      try {
            sh '''
            rm -rf *
            '''
            String tool = options['Tool'].split(':')[0].trim()
            String version = options['Tool'].split(':')[1].trim()
            echo "${options}"
            switch(tool) {
              case 'Blender':                    
                      sh """ 
                      chmod +x "../../cis_tools/RenderSceneJob/download.sh"
                      "../../cis_tools/RenderSceneJob/download.sh" "${options.Scene}"
                      """
                      sh """
                      unzip "scene.zip" -d .
                      """
                      sh """
                      cp "../../cis_tools/RenderSceneJob/find_scene_blender.py" "."
                      cp "../../cis_tools/RenderSceneJob/blender_render.py" "."
                      cp "../../cis_tools/RenderSceneJob/launch_blender.py" "."
                      """
                      String scene = sh (returnStdout: true, script: 'python3 find_scene_blender.py --folder .')
                      echo "Find scene: ${scene}"
                      echo "Launching render"
                      sh """
                        python3 launch_blender.py --tool ${version} --render_device ${options.RenderDevice} --pass_limit ${options.PassLimit} --scene '${scene}'
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
                      unzip "scene.zip" -d .
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
            catch(e) {
                echo "Error while render"
            }
            finally {
              archiveArtifacts "Output/*"
            }
      break;
    default:
      try {
            sh '''
            rm -rf *
            '''
            String tool = options['Tool'].split(':')[0].trim()
            String version = options['Tool'].split(':')[1].trim()
            echo "${options}"
            switch(tool) {
              case 'Blender':                    
                      sh """ 
                      chmod +x "../../cis_tools/RenderSceneJob/download.sh"
                      "../../cis_tools/RenderSceneJob/download.sh" "${options.Scene}"
                      """
                      sh """
                      unzip "scene.zip" -d .
                      """
                      sh """
                      cp "../../cis_tools/RenderSceneJob/find_scene_blender.py" "."
                      cp "../../cis_tools/RenderSceneJob/blender_render.py" "."
                      cp "../../cis_tools/RenderSceneJob/launch_blender.py" "."
                      """
                      //String scene=python3("find_scene_blender.py --folder .").split('\r\n')[2].trim()
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
            catch(e) {
                echo "Error while render"
            }
            finally {
              archiveArtifacts "Output/*"
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
         String RenderDevice = 'gpu'
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
                    RenderDevice:RenderDevice])
}
