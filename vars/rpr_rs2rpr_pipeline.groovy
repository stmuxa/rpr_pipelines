def executeGenTestRefCommand(String osName, Map options)
{
    executeTestCommand(osName, options)
    
    dir('scripts')
    {
        switch(osName)
        {
            case 'Windows':
                bat """
                make_results_baseline.bat
                """
                break;
            case 'OSX':
                sh """
                ./make_results_baseline.sh
                """
                break;
            default:
                sh """
                ./make_results_baseline.sh
                """
        }
    }
}

def executeTestCommand(String osName, Map options)
{
    switch(osName)
    {
    case 'Windows':        
        dir('scripts')
        {
            bat """
            run.bat ${options.testsPackage} \"${options.tests}\">> ../${STAGE_NAME}.log  2>&1
            """
        }
        break;
    case 'OSX':
        sh """
        echo 'sample image' > ./OutputImages/sample_image.txt
        """
        break;
    default:
        sh """
        echo 'sample image' > ./OutputImages/sample_image.txt
        """
    }
}

def executeTests(String osName, String asicName, Map options)
{
    try {
        checkoutGit(options['testsBranch'], 'git@github.com:luxteam/jobs_test_rs2rpr.git')
        
        // update assets
        if(isUnix())
        {
            sh """
            ${CIS_TOOLS}/receiveFilesSync.sh ${options.PRJ_ROOT}/${options.PRJ_NAME}/RedshiftAssets/ ${CIS_TOOLS}/../TestResources/RedshiftAssets
            """
        }
        else
        {
            bat """
            %CIS_TOOLS%\\receiveFilesSync.bat ${options.PRJ_ROOT}/${options.PRJ_NAME}/RedshiftAssets/ /mnt/c/TestResources/RedshiftAssets
            """
        }
        
        String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
        String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"
        
        options.REF_PATH_PROFILE = REF_PATH_PROFILE
        
        outputEnvironmentInfo(osName)
        
        if(options['updateRefs'])
        {
            executeGenTestRefCommand(osName, options)
            sendFiles('./Work/Baseline/', REF_PATH_PROFILE)
        }
        else
        {
            try{
                receiveFiles("${REF_PATH_PROFILE}/*", './Work/Baseline/')
            }
            catch (e) {
            }
            executeTestCommand(osName, options)   
        }
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        echo "Stashing test results to : ${options.testResultsName}"
        dir('Work')
        {
            stash includes: '**/*', name: "${options.testResultsName}", allowEmpty: true
            
            def sessionReport = readJSON file: 'Results/Maya/session_report.json'
            sessionReport.results.each{ testName, testConfigs ->
                testConfigs.each{ key, value ->
                    if ( value.render_duration == 0)
                    {
                        error "Crashed tests detected"
                    }
                }
            }
        }
    }
}

def executeBuildWindows(Map options)
{

}

def executeBuildOSX(Map options)
{

}

def executeBuildLinux(Map options)
{
    
}

def executeBuild(String osName, Map options)
{
    try {        
        dir('jobs_test_rs2rpr')
        {
            checkoutGit(options['projectBranch'], 'https://github.com/luxteam/jobs_test_rs2rpr.git')
        }
        dir('RS2RPRConvertionScript')
        {
            checkoutGit(options['projectBranch'], 'https://github.com/luxteam/RS2RPRConvertionScript.git')
        }
        
        outputEnvironmentInfo(osName)

        switch(osName)
        {
        case 'Windows': 
            executeBuildWindows(options); 
            break;
        case 'OSX':
            executeBuildOSX(options);
            break;
        default: 
            executeBuildLinux(options);
        }
        
        //stash includes: 'Bin/**/*', name: "app${osName}"
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
    }                        
}

def executePreBuild(Map options)
{
    currentBuild.description = ""
    ['projectBranch', 'thirdpartyBranch', 'packageBranch'].each
    {
        if(options[it] != 'master' && options[it] != "")
        {
            currentBuild.description += "<b>${it}:</b> ${options[it]}<br/>"
        }
    }

    //properties([])

    if (env.BRANCH_NAME && env.BRANCH_NAME == "master") {
        properties([[$class: 'BuildDiscarderProperty', strategy: 	
                         [$class: 'LogRotator', artifactDaysToKeepStr: '', 	
                          artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
    } else if (env.BRANCH_NAME && BRANCH_NAME != "master") {
        properties([[$class: 'BuildDiscarderProperty', strategy: 	
                         [$class: 'LogRotator', artifactDaysToKeepStr: '', 	
                          artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '3']],
                  [$class: 'JobPropertyImpl', throttle: [count: 2, durationName: 'hour', userBoost: true]]]);
    } else if (env.JOB_NAME == "RadeonProRenderMayaPlugin-WeeklyFull") {
        properties([[$class: 'BuildDiscarderProperty', strategy: 	
                         [$class: 'LogRotator', artifactDaysToKeepStr: '', 	
                          artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '50']]]);
    } else {
        properties([[$class: 'BuildDiscarderProperty', strategy: 	
                         [$class: 'LogRotator', artifactDaysToKeepStr: '', 	
                          artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
    }
}

def executeDeploy(Map options, List platformList, List testResultList)
{
    try { 
        if(options['executeTests'] && testResultList)
        {
            checkoutGit(options['testsBranch'], 'git@github.com:luxteam/jobs_test_maya.git')

            dir("summaryTestResults")
            {
                testResultList.each()
                {
                    dir("$it".replace("testResult-", ""))
                    {
                        try
                        {
                            unstash "$it"
                        }catch(e)
                        {
                            echo "Can't unstash ${it}"
                            println(e.toString());
                            println(e.getMessage());
                        }
                    
                    }
                }
            }

            dir("jobs_launcher")
            {
                if(options.projectBranch != "") {
                    options.branchName = options.projectBranch
                } else {
                    options.branchName = env.BRANCH_NAME
                }
                if(options.incrementVersion) {
                    options.branchName = "master"
                }
                
                options.commitMessage = options.commitMessage.replace("'", "")
                options.commitMessage = options.commitMessage.replace('"', '')
                bat """
                build_reports.bat ..\\summaryTestResults Maya2017 ${options.commitSHA} ${options.branchName} \\"${options.commitMessage}\\"
                """
                bat "get_status.bat ..\\summaryTestResults"
            }
            
            try
            {
                def summaryReport = readJSON file: 'summaryTestResults/summary_status.json'
                if (summaryReport.failed > 0 || summaryReport.error > 0)
                {
                    println("Some tests failed")
                    currentBuild.result="UNSTABLE"
                }
            }
            catch(e)
            {
                println("CAN'T GET TESTS STATUS")
            }
            
            try
            {
                options.testsStatus = readFile("summaryTestResults/slack_status.json")
            }
            catch(e)
            {
                println(e.toString())
                println(e.getMessage())
                options.testsStatus = ""
            }   

            publishHTML([allowMissing: false, 
                         alwaysLinkToLastBuild: false, 
                         keepAll: true, 
                         reportDir: 'summaryTestResults', 
                         reportFiles: 'summary_report.html, performance_report.html, compare_report.html',
                         reportName: 'Test Report',
                         reportTitles: 'Summary Report, Performance Report, Compare Report'])
        }
    }
    catch (e) {
        currentBuild.result = "FAILED"
        println(e.toString());
        println(e.getMessage());
        throw e
    }
    finally
    {}   
}



def call(String projectBranch = "",
         String testsBranch = "master",
         String platforms = 'Windows:AMD_RXVEGA,NVIDIA_GF1080TI', 
         Boolean updateRefs = false,
         Boolean enableNotifications = true,
         String testsPackage = "",
         String tests = "") {
    try
    {
        String PRJ_NAME="RS2RPRConvertionScript"
        String PRJ_ROOT="rpr-plugins"

        multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, this.&executeDeploy, 
                               [projectBranch:projectBranch, 
                                testsBranch:testsBranch, 
                                updateRefs:updateRefs, 
                                enableNotifications:enableNotifications,
                                PRJ_NAME:PRJ_NAME,
                                PRJ_ROOT:PRJ_ROOT,
                                testsPackage:testsPackage,
                                tests:tests,
                                reportName:'Test_20Report'])
    }
    catch(e) {
        currentBuild.result = "FAILED"
        println(e.toString());
        println(e.getMessage());
        throw e
    }
}
