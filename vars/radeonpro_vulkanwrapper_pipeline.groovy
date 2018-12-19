def executeGenTestRefCommand(String osName, Map options)
{
    dir('BaikalNext/RprTest')
    {
        switch(osName)
        {
            case 'Windows':
                bat """
                RprTest_genref.bat --gtest_output=xml:../../${STAGE_NAME}.gtest.xml >> ..\\..\\${STAGE_NAME}.log 2>&1
                """
                break;
            case 'OSX':
                sh """
                    export LD_LIBRARY_PATH=`pwd`/../Build/bin/:\$LD_LIBRARY_PATH
                    ./RprTest.sh --gtest_output=xml:../../${STAGE_NAME}.gtest.xml >> ../../${STAGE_NAME}.log 2>&1
                """
                break;
            default:
                sh """
                    export LD_LIBRARY_PATH=`pwd`/../Build/bin/:\${LD_LIBRARY_PATH}
                    ./RprTest.sh --gtest_output=xml:../../${STAGE_NAME}.gtest.xml >> ../../${STAGE_NAME}.log 2>&1
                """
        }
    }
}

def executeTestCommand(String osName, Map options)
{
    dir('BaikalNext/RprTest')
    {
        switch(osName)
        {
            case 'Windows':
                bat """
                RprTest.bat --gtest_output=xml:../../${STAGE_NAME}.gtest.xml >> ..\\..\\${STAGE_NAME}.log 2>&1
                """
                break;
            case 'OSX':
                sh """
                export LD_LIBRARY_PATH=`pwd`/../Build/bin/:\$LD_LIBRARY_PATH
                RprTest.sh --gtest_output=xml:../../${STAGE_NAME}.gtest.xml >> ../../${STAGE_NAME}.log 2>&1
                """
                break;
            default:
                sh """
                export LD_LIBRARY_PATH=`pwd`/../Build/bin/:\${LD_LIBRARY_PATH}
                RprTest.sh --gtest_output=xml:../../${STAGE_NAME}.gtest.xml >> ../../${STAGE_NAME}.log 2>&1
                """
        }
    }
}

def executeTests(String osName, String asicName, Map options)
{
    cleanWs()
    //String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
    String REF_PATH_PROFILE="rpr-core/RadeonProRender-Hybrid/ReferenceImages"
    String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"
    
    try {
        //checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])
        
        outputEnvironmentInfo(osName)
        unstash "app${osName}"
        switch(osName)
        {
            case 'Windows':
                unzip dir: '.', glob: '', zipFile: 'BaikalNext_Build-Windows.zip'
                break
            default:
                sh "tar -xJf BaikalNext_Build*"
        }
            
        
        if(options['updateRefs']) {
            echo "Updating Reference Images"
            executeGenTestRefCommand(osName, options)
            sendFiles('./BaikalNext/RprTest/ReferenceImages/*.*', "${REF_PATH_PROFILE}/${asicName}-${osName}")
        } else {
            echo "Execute Tests"
            receiveFiles("${REF_PATH_PROFILE}/${asicName}-${osName}/*", './BaikalNext/RprTest/ReferenceImages/')
            executeTestCommand(osName, options)
        }
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        
        dir('BaikalNext/RprTest')
        {
            sendFiles('./ReferenceImages/*.*', "${options.JOB_PATH}/${asicName}-${osName}/ReferenceImages")
            sendFiles('./OutputImages/*.*', "${options.JOB_PATH}/${asicName}-${osName}/OutputImages")
        }
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        junit "*.gtest.xml"
        cleanWs()
    }
}

def executeBuildWindows(Map options)
{
    bat """
    mkdir Build
    cd Build
    cmake ${options['cmakeKeys']} -DVW_ENABLE_DXR=ON -G "Visual Studio 15 2017 Win64" .. >> ..\\${STAGE_NAME}.log 2>&1
    cmake --build . --config Release >> ..\\${STAGE_NAME}.log 2>&1
    """
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
        
        // dir('Build')
        // {
            // TODO: check file name
            // stash includes: "BaikalNext_${STAGE_NAME}*", name: "app${osName}"
        // }
    }
    catch (e) {
        currentBuild.result = "FAILED"
        archiveArtifacts "Build/CMakeFiles/CMakeOutput.log" 
        throw e
    }
    finally {
        archiveArtifacts "${STAGE_NAME}.log"
        // archiveArtifacts "Build/BaikalNext_${STAGE_NAME}*"
    }                        

}

def executeDeploy(Map options, List platformList, List testResultList)
{

}

def call(String projectBranch = "", 
         String platforms = 'Windows;Ubuntu18;CentOS7', 
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
