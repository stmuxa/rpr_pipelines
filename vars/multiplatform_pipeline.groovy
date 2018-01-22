def executePlatform(String osName, String gpuNames, def executeBuild, def executeTests, def executeDeploy, Map options)
{
    def retNode =  
    {
        try {
            node("${osName} && Builder")
            {
                stage("Build-${osName}")
                {
                    String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                    ws("WS/${JOB_NAME_FMT}") {
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
                    testResultList << "testResult-${asicName}-${osName}"

                    testTasks["Test-${it}-${osName}"] = {
                        node("${osName} && Tester && OpenCL && gpu${asicName}")
                        {
                            stage("Test-${asicName}-${osName}")
                            {
                                options['testResultsName'] = "testResult-${asicName}-${osName}"
                                executeTests(osName, asicName, options)
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
            def tasks = [:]
            def testResultList = [];
            
            platforms.split(';').each()
            {
                def (osName, gpuNames) = it.tokenize(':')
                gpuNames.split(',').each()
                {
                    String asicName = it
                    testResultList << "testResult-${asicName}-${osName}"
                }
                
                tasks[osName]=executePlatform(osName, gpuNames, executeBuild, executeTests, executeDeploy, options)
            }
            parallel tasks

            if(executeDeploy)
            {
                node("Deploy")
                {
                    stage("Deploy")
                    {
                        String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                        ws("WS/${JOB_NAME_FMT}_Deploy") {
                            executeDeploy(options, testResultList)
                        }
                    }
                }
            }    
        }
    }
    catch (e) {
        currentBuild.result = "FAILED"
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
