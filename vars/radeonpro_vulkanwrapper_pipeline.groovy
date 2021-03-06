def executeGenTestRefCommand(String osName, Map options)
{

}

def executeTestCommand(String osName, Map options)
{

}

def executeTests(String osName, String asicName, Map options)
{
    cleanWs()
    String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
    String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"
    
    try {
        //checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])
        outputEnvironmentInfo(osName)
        unstash "app${osName}"
        
        if(options['updateRefs']) {
            echo "Updating Reference Images"
            executeGenTestRefCommand(osName, options)
            
        } else {
            echo "Execute Tests"
            
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
    try
    {
        bat """
        mkdir Build_DXR
        cd Build_DXR
        cmake ${options['cmakeKeys']} -DVW_ENABLE_DXR=ON -DVW_ENABLE_DXR_SUPPORT=ON -G "Visual Studio 15 2017 Win64" .. >> ..\\${STAGE_NAME}.DXR.log 2>&1
        cmake --build . --config Release >> ..\\${STAGE_NAME}.DXR.log 2>&1
        """
    }
    catch(e){
        println(e.toString())
        currentBuild.result = "FAILED"
        throw e
    }
    finally
    {
        bat """
        mkdir Build
        cd Build
        cmake ${options['cmakeKeys']} -DVW_ENABLE_DXR=ON -DVW_ENABLE_DXR_SUPPORT=OFF -G "Visual Studio 15 2017 Win64" .. >> ..\\${STAGE_NAME}.log 2>&1
        cmake --build . --config Release >> ..\\${STAGE_NAME}.log 2>&1
        """
    }
}

def executeBuildOSX(Map options)
{
    sh """
    mkdir Build
    cd Build
    cmake ${options['cmakeKeys']} .. >> ../${STAGE_NAME}.log 2>&1
    make >> ../${STAGE_NAME}.log 2>&1
    """
}

def executeBuildLinux(Map options)
{
    sh """
    mkdir Build
    cd Build
    cmake ${options['cmakeKeys']} .. >> ../${STAGE_NAME}.log 2>&1
    make >> ../${STAGE_NAME}.log 2>&1
    """
}

def executePreBuild(Map options)
{
    dir('RadeonProVulkanWrapper')
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

        if("${env.BRANCH_NAME}" == "master" || "${options.projectBranch}" == "master")
        {
            try
            {
                bat "tools\\doxygen\\doxygen.exe tools\\doxygen\\Doxyfile >> doxygen_build.log 2>&1"
                archiveArtifacts allowEmptyArchive: true, artifacts: 'doxygen_build.log'
                sendFiles('./docs/', "/${options.PRJ_ROOT}/${options.PRJ_NAME}/doxygen-docs")
            }
            catch(e)
            {
                println("Can't build doxygen documentation")
                println(e.toString())
                currentBuild.result = "UNSTABLE"
            }
        }
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
    cleanWs()
}

def call(String projectBranch = "", 
         //TODO: OSX
         String platforms = 'Windows;Ubuntu;Ubuntu18;CentOS7', 
         String PRJ_ROOT='rpr-core',
         String PRJ_NAME='RadeonProVulkanWrapper',
         String projectRepo='https://github.com/Radeon-Pro/RadeonProVulkanWrapper.git',
         Boolean updateRefs = false, 
         Boolean enableNotifications = true,
         String cmakeKeys = "-DCMAKE_BUILD_TYPE=Release") {

    multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, null, null,
                           [projectBranch:projectBranch,
                            updateRefs:updateRefs, 
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            projectRepo:projectRepo,
                            BUILDER_TAG:'BuilderS',
                            executeBuild:true,
                            executeTests:true,
                            slackChannel:"${SLACK_BAIKAL_CHANNEL}",
                            slackBaseUrl:"${SLACK_BAIKAL_BASE_URL}",
                            slackTocken:"${SLACK_BAIKAL_TOCKEN}",
                            cmakeKeys:cmakeKeys])
}
