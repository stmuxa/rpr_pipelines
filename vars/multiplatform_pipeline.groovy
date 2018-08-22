import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;

def executeTestsNode(String osName, String gpuNames, def executeTests, Map options)
{
    if(gpuNames && options['executeTests'])
    {
        def testTasks = [:]
        gpuNames.split(',').each()
        {
            String asicName = it
            echo "Scheduling Test ${osName}:${asicName}"

            testTasks["Test-${it}-${osName}"] = {
                for ( int i = 0; i < options.tests.size; i++ ) {
                    String testName = options.tests[i]
                    stage("Test-${asicName}-${osName}")
                    {
                        node("${osName} && Tester && OpenCL && gpu${asicName}")
                        {
                            timeout(time: 8, unit: 'HOURS')
                            {
                                ws("WS/${options.PRJ_NAME}_Test")
                                {
                                    Map newOptions = options.clone()
                                    newOptions['testResultsName'] = "testResult-${asicName}-${osName}-${testName}"
                                    newOptions['stageName'] = "${asicName}-${osName}"
                                    newOptions['tests'] = testName
                                    executeTests(osName, asicName, newOptions)
                                }
                            }
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

def executePlatform(String osName, String gpuNames, def executeBuild, def executeTests, Map options)
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
                        timeout(time: 90, unit: 'MINUTES')
                        {
                            String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                            ws("WS/${options.PRJ_NAME}_Build") {
                                executeBuild(osName, options)
                            }
                        }
                    }
                }
            }

            executeTestsNode(osName, gpuNames, executeTests, options)
        }
        catch (e) {
            println(e.toString());
            println(e.getMessage());     
            currentBuild.result = "FAILED"
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
                      artifactNumToKeepStr: '10', daysToKeepStr: '', numToKeepStr: '']]]);
        
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
                    node("Windows && ${options['BUILDER_TAG']}")
                    {
                        ws("WS/${options.PRJ_NAME}_PreBuild") {
                            stage("PreBuild")
                            {
                                timeout(time: 30, unit: 'MINUTES')
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
                }
                
                def tasks = [:]

                platforms.split(';').each()
                {
                    //def (osName, gpuNames) = it.tokenize(':')
                    
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
                            options.tests.each()
                            { testName ->
                                String asicName = it
                                testResultList << "testResult-${asicName}-${osName}-${testName}"
                            }
                        }
                    }

                    tasks[osName]=executePlatform(osName, gpuNames, executeBuild, executeTests, options)
                }
                parallel tasks
            }
            finally
            {
                node("Windows && Builder")
                {
                    stage("Deploy")
                    {
                        timeout(time: 45, unit: 'MINUTES')
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
                                    currentBuild.result = "FAILED"
                                    throw e
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    catch (FlowInterruptedException e)
    {
        println(e.toString());
        println(e.getMessage());
        options.CBR = "ABORTED"
        echo "Job was ABORTED by user: ${currentBuild.result}"
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
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
                                        options)
        }
    }
}
