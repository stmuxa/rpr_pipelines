def executePlatform(String osName, String gpuNames, def executeBuild, def executeTests, def executeDeploy, Map options)
{
    def retNode =  
    {
        try {
            node("${osName} && ${options.BUILDER_TAG}")
            {
                stage("Build-${osName}")
                {
                    String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                    ws("WS/${options.PRJ_NAME}_Build") {
                        executeBuild(osName, options)
                    }
                }
            }

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
                                    executeTests(osName, asicName, newOptions)
                                }
                            }
                        }
                    }
                }
                parallel testTasks
            }
            else
            {
                echo "No tests found for ${osName}"
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
    return retNode
}

def call(String platforms, 
         def executeBuild, def executeTests, def executeDeploy, Map options) {
      
    try {
        
        timestamps {
            String PRJ_PATH="${options.PRJ_ROOT}/${options.PRJ_NAME}"
            String REF_PATH="${PRJ_PATH}/ReferenceImages"
            String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
            options['PRJ_PATH']="${PRJ_PATH}"
            options['REF_PATH']="${REF_PATH}"
            options['JOB_PATH']="${JOB_PATH}"
            if(options.get('BUILDER_TAG', '') == '')
                options['BUILDER_TAG'] = 'Builder'

            try {
                def tasks = [:]
                def testResultList = [];

                platforms.split(';').each()
                {
                    def (osName, gpuNames) = it.tokenize(':')
                    if(gpuNames)
                    {
                        gpuNames.split(',').each()
                        {
                            String asicName = it
                            testResultList << "testResult-${asicName}-${osName}"
                        }
                    }

                    tasks[osName]=executePlatform(osName, gpuNames, executeBuild, executeTests, executeDeploy, options)
                }
                parallel tasks
            }
            finally
            {
                node("Deploy")
                {
                    stage("Deploy")
                    {
                        String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                        ws("WS/${options.PRJ_NAME}_Deploy") {


                            appendHtmlLinkToFile("artifacts.html", "${options.PRJ_PATH}", 
                                                 "https://builds.rpr.cis.luxoft.com/${options.PRJ_PATH}")
                            appendHtmlLinkToFile("artifacts.html", "${options.REF_PATH}", 
                                                 "https://builds.rpr.cis.luxoft.com/${options.REF_PATH}")
                            appendHtmlLinkToFile("artifacts.html", "${options.JOB_PATH}", 
                                                 "https://builds.rpr.cis.luxoft.com/${options.JOB_PATH}")

                            archiveArtifacts "artifacts.html"
                            if(executeDeploy)
                            {
                                executeDeploy(options, testResultList)
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
    finally {

        echo "enableNotifications = ${options.enableNotifications}"
        if("${options.enableNotifications}" == "true")
        {
            sendBuildStatusNotification(currentBuild.result, 
                                        options.get('slackChannel', ''), 
                                        options.get('slackBaseUrl', ''),
                                        options.get('slackTocken', ''))
        }
    }
}
