def executeGenTestRefCommand(String osName, Map options)
{
    //TODO: execute genref command
}

def executeTestCommand(String osName, Map options)
{
    //TODO: execute test command
}

def executeTests(String osName, String asicName, Map options)
{
    String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
    
    //TODO: update test resources
    
    try {        
        outputEnvironmentInfo(osName)
        
        unstash "app${osName}"
        
        bat "tree"
        
        /*if(options['updateRefs']) {
            echo "Updating Reference Images"
            executeGenTestRefCommand(osName, options)
            //TODO: sendFiles()
        } else {
            echo "Execute Tests"
            //TODO: receiveFiles()
            executeTestCommand(osName, options)
        }*/
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
    }
}

def executeBuildWindows(Map options)
{
    bat"""
    "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe" /target:build /property:Configuration=Release RadeonProViewer.sln >> ${STAGE_NAME}.log 2>&1
    mkdir rpviewer
    xcopy config.json rpviewer
    xcopy sky.hdr rpviewer
    move x64\\Release\\RadeonProViewer.exe rpviewer
    xcopy shaders rpviewer\\shaders /y/i/s
    xcopy rpr rpviewer\\rpr /y/i/s
    xcopy hybrid rpviewer\\hybrid /y/i/s
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
    cleanWs()
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
}

def call(String projectBranch = "", 
         String testsBranch = "",
         String platforms = 'Windows',
         Boolean updateRefs = false,
         Boolean enableNotifications = true) {

    String PRJ_ROOT='rpr-core'
    String PRJ_NAME='RadeonProViewer'
    String projectRepo='https://github.com/Radeon-Pro/RadeonProViewer.git'

    multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, null,
                           [projectBranch:projectBranch,
                            updateRefs:updateRefs, 
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            projectRepo:projectRepo,
                            BUILDER_TAG:'BuilderS',
                            executeBuild:true,
                            executeTests:true])
}
