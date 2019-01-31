def executeGenTestRefCommand(String osName, Map options)
{

}

def executeTestCommand(String osName, Map options)
{

}

def executeTests(String osName, String asicName, Map options)
{
    //TODO: execute tests
    cleanWs()
    //String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
    
    try {
        //checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])
        outputEnvironmentInfo(osName)
        unstash "app${osName}"
        
        if(options['updateRefs']) {
            echo "Updating Reference Images"
            executeGenTestRefCommand(osName, options)
            // send files
        } else {
            echo "Execute Tests"
            // receive files
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
        cleanWs()
    }
}

def executeBuildWindows(Map options)
{
    bat """
    %CIS_TOOLS%\\receiveFilesSync.bat ${options.PRJ_ROOT}/${options.PRJ_NAME}/UnrealAssets/ /mnt/c/TestResources/UnrealAssets
    """
    
    //TODO: check symlink
    /*bat"""
    mklink /d DerivedDataCache C:\\TestResources\\UnrealAssets\\ShooterGame\\DerivedDataCache
    mklink /d DerivedDataCache C:\\TestResources\\UnrealAssets\\ShooterGame\\DerivedDataCache
    """*/
    bat """
    xcopy C:\\TestResources\\UnrealAssets\\ShooterGame ShooterGame /s/y/i
    """
    
    bat """
    Setup.bat >> ${STAGE_NAME}.log 2>&1
    rem .\\Engine\\Source\\ThirdParty\\RTEffects\\build.bat >> ${STAGE_NAME}.log 2>&1
    .\\GenerateProjectFiles.bat -cmakefile >> ${STAGE_NAME}.log 2>&1
    .\\Engine\\Build\\BatchFiles\\Build.bat ShooterGameEditor Win64 Development -WaitMutex -FromMsBuild >> ${STAGE_NAME}.log 2>&1
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
    dir('RadeonProGameRTFX')
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
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "${STAGE_NAME}*.log"
    }                        
}

def executeDeploy(Map options, List platformList, List testResultList)
{

}

def call(String projectBranch = "",
         String platforms = 'Windows',
         Boolean updateRefs = false,
         Boolean enableNotifications = true) {

    String PRJ_ROOT='rpr-core'
    String PRJ_NAME='RadeonProGameRTFX'
    String projectRepo='https://github.com/Radeon-Pro/RadeonGameRTFX.git'

    multiplatform_pipeline(platforms, null, this.&executeBuild, this.&executeTests, null,
                           [projectBranch:projectBranch,
                            updateRefs:updateRefs, 
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            projectRepo:projectRepo,
                            BUILDER_TAG:'BuilderU',
                            executeBuild:true,
                            executeTests:false,
                            BUILD_TIMEOUT:300])
}
