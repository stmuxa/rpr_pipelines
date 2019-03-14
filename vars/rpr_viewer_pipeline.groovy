def executeGenTestRefCommand(String osName, Map options)
{
    //TODO: execute genref command
}

def executeTestCommand(String osName, Map options)
{
    switch(osName)
    {
    case 'Windows':
        dir('scripts')
        {          
            bat """
            run.bat >> ../${options.stageName}.log  2>&1
            """
        }
        break;
    default:
        echo "empty"
    }
}

def executeTests(String osName, String asicName, Map options)
{
    cleanWs()
    
    bat """
    %CIS_TOOLS%\\receiveFilesSync.bat ${options.PRJ_ROOT}/${options.PRJ_NAME}/Assets/ /mnt/c/TestResources/RprViewer
    """
    
    String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
    String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"
    
    try {
        checkOutBranchOrScm(options['testsBranch'], 'git@github.com:luxteam/jobs_test_rprviewer.git')
        outputEnvironmentInfo(osName)
        
        unstash "app${osName}"
        bat "rename rpviewer Viewer"
        
        if(options['updateRefs']) {
            echo "Updating Reference Images"
            executeGenTestRefCommand(osName, options)
            //TODO: sendFiles()
        } else {
            echo "Execute Tests"
            //TODO: receiveFiles("${options.REF_PATH}", "./jobs_test_rprviewer/Work/Baseline/")
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
        dir('jobs_test_rprviewer/Work')
        {
            stash includes: '**/*', name: "${options.testResultsName}", allowEmpty: true
        }
    }
}

def executeBuildWindows(Map options)
{
    bat"""
    "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe" /target:build /property:Configuration=Release RadeonProViewer.sln >> ${STAGE_NAME}.log 2>&1
    mkdir rpviewer\\RprViewer
    xcopy config.json rpviewer\\RprViewer
    xcopy UIConfig.json rpviewer\\RprViewer
    xcopy UIConfigFerrari.json rpviewer\\RprViewer
    xcopy sky.hdr rpviewer\\RprViewer
    move x64\\Release\\RadeonProViewer.exe rpviewer\\RprViewer
    
    xcopy shaders rpviewer\\RprViewer\\shaders /y/i/s
    xcopy rpr rpviewer\\RprViewer\\rpr /y/i/s
    xcopy hybrid rpviewer\\RprViewer\\hybrid /y/i/s
    
    xcopy hybrid\\win\\3rdparty rpviewer\\3rdparty /y/i/s
    xcopy hybrid\\win\\BaikalNext rpviewer\\BaikalNext /y/i/s
    """
}

def executeBuildOSX(Map options)
{
}

def executeBuildLinux(Map options)
{
}

def executePreBuild(Map options)
{
    checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])

    AUTHOR_NAME = bat (
            script: "git show -s --format=%%an HEAD ",
            returnStdout: true
            ).split('\r\n')[2].trim()

    echo "The last commit was written by ${AUTHOR_NAME}."
    options.AUTHOR_NAME = AUTHOR_NAME

    commitMessage = bat ( script: "git log --format=%%B -n 1", returnStdout: true ).split('\r\n')[2].trim()
    echo "Commit message: ${commitMessage}"
    options.commitMessage = commitMessage
}

def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])
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
        
        stash includes: 'rpviewer/**/*', name: 'appWindows'
        zip archive: true, dir: 'rpviewer', glob: '', zipFile: 'RprViewer.zip'
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "${STAGE_NAME}.log"
    }
}

def executeDeploy(Map options, List platformList, List testResultList)
{
    try
    {
        if(options['executeTests'] && testResultList)
        {
            checkOutBranchOrScm(options['testsBranch'], 'git@github.com:luxteam/jobs_test_rprviewer.git')
            
            dir("summaryTestResults")
            {
                testResultList.each()
                {
                    dir("$it".replace("testResult-", ""))
                    {
                        try
                        {
                            unstash "$it"
                        }
                        catch(e)
                        {
                            echo "Can't unstash ${it}"
                            println(e.toString())
                            println(e.getMessage())
                        }
                    }
                }
            }

            dir("jobs_launcher") {
                String branchName = env.BRANCH_NAME ?: options.projectBranch

                try {
                    withEnv(["JOB_STARTED_TIME=${options.JOB_STARTED_TIME}"])
                    {
                        bat """
                        build_reports.bat ..\\summaryTestResults "${escapeCharsByUnicode('RprViewer')}" ${options.commitSHA} ${branchName} \"${escapeCharsByUnicode(options.commitMessage)}\"
                        """
                    }
                } catch(e) {
                    println("ERROR during report building")
                    println(e.toString())
                    println(e.getMessage())
                }
            }
        }
        publishHTML([allowMissing: false,
                         alwaysLinkToLastBuild: false,
                         keepAll: true,
                         reportDir: 'summaryTestResults',
                         reportFiles: 'summary_report.html, performance_report.html, compare_report.html',
                         reportName: 'Test Report',
                         reportTitles: 'Summary Report, Performance Report, Compare Report'])
    }
    catch(e)
    {
        println(e.toString())
    }
}

def call(String projectBranch = "", 
         String testsBranch = "master",
         String platforms = 'Windows',
         Boolean updateRefs = false,
         Boolean enableNotifications = true) {

    String PRJ_ROOT='rpr-core'
    String PRJ_NAME='RadeonProViewer'
    String projectRepo='https://github.com/Radeon-Pro/RadeonProViewer.git'

    multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, null,
                           [projectBranch:projectBranch,
                            testsBranch:testsBranch,
                            updateRefs:updateRefs, 
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            projectRepo:projectRepo,
                            BUILDER_TAG:'BuilderS',
                            executeBuild:true,
                            executeTests:true])
}
