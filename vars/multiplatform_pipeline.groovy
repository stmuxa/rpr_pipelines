def executePlatform(String osName, String gpuNames, def executeBuild, def executeTests, def executeDeploy, Map options)
{
    def retNode =  
    {   
        try {
            if(!options['skipBuild'] && options['executeBuild'])
            {
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
            }

            if(gpuNames && options['executeTests'])
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
            echo "FAILED by executePlatform"
            throw e
        }
    }
    return retNode
}

def call(String platforms, 
         def executePreBuild, def executeBuild, def executeTests, def executeDeploy, Map options) {
    
    //currentBuild.result = "SUCCESSFUL"
    try {
        properties([[$class: 'BuildDiscarderProperty', strategy: 
                     [$class: 'LogRotator', artifactDaysToKeepStr: '', 
                      artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
        
        timestamps {
            String PRJ_PATH="${options.PRJ_ROOT}/${options.PRJ_NAME}"
            String REF_PATH="${PRJ_PATH}/ReferenceImages"
            String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
            options['PRJ_PATH']="${PRJ_PATH}"
            options['REF_PATH']="${REF_PATH}"
            options['JOB_PATH']="${JOB_PATH}"
            if(options.get('BUILDER_TAG', '') == '')
                options['BUILDER_TAG'] = 'Builder'

            def platformList = [];
            def testResultList = [];

            try {
                if(executePreBuild)
                {
                    node("Windows && Builder")
                    {
                        ws("WS/${options.PRJ_NAME}_PreBuild") {
                            stage("PreBuild")
                            {
                                executePreBuild(options)
                                
                                if(!options['executeBuild']) {
                                    options.CBR = 'SKIPPED'
                                    echo "Build SKIPPED"
                                }
                            }
                        }
                    }
                }
                
                def tasks = [:]

                platforms.split(';').each()
                {
                    def (osName, gpuNames) = it.tokenize(':')
                    
                    List tokens_2 = it.tokenize(':')
                    String osName_2 = tokens_2.get(0)
                    String gpuNames_2 = tokens_2.get(1)
                    
                    echo "tokens"
                    echo "${tokens_2}"
                    
                    echo "os name"
                    echo "${osName}"
                    echo "${osName_2}"
                    
                    echo "gpu"
                    echo "${gpuNames}"
                    echo "${gpuNames_2}"
                    
                    platformList << osName_2
                    if(gpuNames_2)
                    {
                        gpuNames_2.split(',').each()
                        {
                            String asicName = it
                            testResultList << "testResult-${asicName}-${osName_2}"
                        }
                    }

                    tasks[osName_2]=executePlatform(osName_2, gpuNames_2, executeBuild, executeTests, executeDeploy, options)
                }
                parallel tasks
            }
            finally
            {
                node("Windows && Builder")
                {
                    stage("Deploy")
                    {
                        ws("WS/${options.PRJ_NAME}_Deploy") {

                            try {
                                if(executeDeploy && options['executeTests'])
                                {
                                    executeDeploy(options, platformList, testResultList)
                                }
                                dir('_publish_artifacts_html_')
                                {
                                    deleteDir()
                                    appendHtmlLinkToFile("artifacts.html", "${options.PRJ_PATH}", 
                                                         "https://builds.rpr.cis.luxoft.com/${options.PRJ_PATH}")
                                    appendHtmlLinkToFile("artifacts.html", "${options.REF_PATH}", 
                                                         "https://builds.rpr.cis.luxoft.com/${options.REF_PATH}")
                                    appendHtmlLinkToFile("artifacts.html", "${options.JOB_PATH}", 
                                                         "https://builds.rpr.cis.luxoft.com/${options.JOB_PATH}")

                                    archiveArtifacts "artifacts.html"
                                }
                                publishHTML([allowMissing: false, 
                                             alwaysLinkToLastBuild: false, 
                                             keepAll: true, 
                                             reportDir: '_publish_artifacts_html_', 
                                             reportFiles: 'artifacts.html', reportName: 'Project\'s Artifacts', reportTitles: 'Artifacts'])
                            }
                            catch (e) {
                                println(e.toString());
                                println(e.getMessage());
                                println(e.getStackTrace());
                                currentBuild.result = "FAILED"
                                echo "FAILED by deploy"
                                throw e
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
                                        options.get('slackTocken', ''),
                                        options.CBR)
        }
    }
}
