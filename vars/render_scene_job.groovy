def executeRender(Map options)
{
  
  //receiveFiles("/rpr-plugins/RenderJob", '.')
  //bat """
   //  "C:\\JN\\cis_tools\\receiveFiles.bat" /rpr-plugins/RenderJob .
 // """
  bat """
   If Exist "Output" (
   rmdir /s /Q "Output"
   ) 
  """
  switch(options['Tool']) 
  {
    case 'Blender 2.79':
            bat """
            "C:\\JN\\cis_tools\\receiveFiles.bat" /rpr-plugins/RenderJob/${options.Scene_folder} .
            """
            bat """
            cd "${options.Scene_folder}"
            "C:\\Program Files\\Blender Foundation\\Blender\\blender.exe" -b "IES.blend" -P "blender_render.py"
            """
            break;
    case 'Autodesk 3Ds Max 2017':
            bat """
            cd "RenderJob\\Autodesk 3Ds Max"
            "C:\\Program Files\\Autodesk\\3ds Max 2017\\3dsmax.exe" -U MAXScript "max_render.ms" -silent
            """
            break;
    case 'Autodesk Maya 2017':
            bat """
            cd "RenderJob\\Autodesk Maya"
            set MAYA_SCRIPT_PATH=%cd%;%MAYA_SCRIPT_PATH%
            "C:\\Program Files\\Autodesk\\Maya2017\\bin\\maya.exe" -command "source maya_render.mel; evalDeferred -lp (rpr_render());"
            """
            break;
  }    
  archiveArtifacts "Output/*"
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
                                    executeRender(newOptions)
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
                      artifactNumToKeepStr: '10', daysToKeepStr: '', numToKeepStr: '']]]);
        
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
         String LinkScene = '',
         String LinkMSI = '',
         String platforms = 'Windows:AMD_RXVEGA'
         ) {
  
    String PRJ_ROOT='Render_Scene'
    String PRJ_NAME='Render_Scene'
      
    main(platforms,
                   [
                    enableNotifications:false,
                    PRJ_NAME:PRJ_NAME,
                    PRJ_ROOT:PRJ_ROOT,
                    Scene_folder:Scene_folder,
                    Plugin_version:Plugin_version,
                    Tool:Tool])
}
