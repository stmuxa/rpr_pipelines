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
        checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_rprviewer.git')
        outputEnvironmentInfo(osName)

        unstash "app${osName}"

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
        dir('Work')
        {
            stash includes: '**/*', name: "${options.testResultsName}", allowEmpty: true
        }
    }
}

def executeBuildWindows(Map options)
{
    bat"""
    cmake . -B build -G "Visual Studio 15 2017" -A x64 >> ${STAGE_NAME}.log 2>&1
    cmake --build build --target RadeonProViewer --config Release >> ${STAGE_NAME}.log 2>&1

    mkdir ${options.DEPLOY_FOLDER}
    xcopy config.json ${options.DEPLOY_FOLDER}
    xcopy README.md ${options.DEPLOY_FOLDER}
    xcopy UIConfig.json ${options.DEPLOY_FOLDER}
    xcopy UIConfigFerrari.json ${options.DEPLOY_FOLDER}
    xcopy sky.hdr ${options.DEPLOY_FOLDER}
    move build\\Release\\RadeonProViewer.exe ${options.DEPLOY_FOLDER}

    xcopy shaders ${options.DEPLOY_FOLDER}\\shaders /y/i/s
    xcopy rpr ${options.DEPLOY_FOLDER}\\rpr /y/i/s
    xcopy hybrid ${options.DEPLOY_FOLDER}\\hybrid /y/i/s
    xcopy support ${options.DEPLOY_FOLDER}\\support /y/i/s

    mkdir ${options.DEPLOY_FOLDER}\\rpml\\lib
    xcopy rpml\\lib\\RadeonML-DirectML.dll ${options.DEPLOY_FOLDER}\\rpml\\lib\\RadeonML-DirectML.dll*
    xcopy rif\\models ${options.DEPLOY_FOLDER}\\rif\\models /s/i/y
    xcopy rif\\lib ${options.DEPLOY_FOLDER}\\rif\\lib /s/i/y
    del /q ${options.DEPLOY_FOLDER}\\rif\\lib\\*.lib
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

        stash includes: "${options.DEPLOY_FOLDER}/**/*", name: "app${osName}"
        zip archive: true, dir: "${options.DEPLOY_FOLDER}", glob: '', zipFile: 'RprViewer.zip'
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
            checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_rprviewer.git')

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
            String branchName = env.BRANCH_NAME ?: options.projectBranch

            try {
                withEnv(["JOB_STARTED_TIME=${options.JOB_STARTED_TIME}"])
                {
                    dir("jobs_launcher") {
                        bat """
                        build_reports.bat ..\\summaryTestResults "${escapeCharsByUnicode('RprViewer')}" ${options.commitSHA} ${branchName} \"${escapeCharsByUnicode(options.commitMessage)}\"
                        """
                    }
                }
            } catch(e) {
                println("ERROR during report building")
                println(e.toString())
                println(e.getMessage())
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

    multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, this.&executeDeploy,
                           [projectBranch:projectBranch,
                            testsBranch:testsBranch,
                            updateRefs:updateRefs,
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            projectRepo:projectRepo,
                            BUILDER_TAG:'BuilderS',
                            executeBuild:true,
                            executeTests:true,
                            DEPLOY_FOLDER:"RprViewer"])
}
