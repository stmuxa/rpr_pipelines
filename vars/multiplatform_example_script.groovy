def executeGenTestRefCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat """
        ..\\Bin\\Release\\x64\\BaikalTest64.exe -genref 1 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log 2>&1
        """
        break;
    case 'OSX':
        sh """
            ../Bin/Release/x64/BaikalTest64 -genref 1 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
        """
        break;
    default:
        sh """
            export LD_LIBRARY_PATH=`pwd`/../Bin/Release/x64/:\${LD_LIBRARY_PATH}
            ../Bin/Release/x64/BaikalTest64 -genref 1 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
        """
    }
}

def executeTestCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat """
            ..\\Bin\\Release\\x64\\BaikalTest64.exe --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log 2>&1
        """
        break;
    case 'OSX':
        sh """
            ../Bin/Release/x64/BaikalTest64 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
        """
        break;
    default:
        sh """
            export LD_LIBRARY_PATH=`pwd`/../Bin/Release/x64/:\${LD_LIBRARY_PATH}
            ../Bin/Release/x64/BaikalTest64 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
        """
    }
}

def executeTests(String osName, String asicName, Map options)
{
    String PRJ_PATH="builds/rpr-core/RadeonProRender-Baikal"
    String REF_PATH="${PRJ_PATH}/ReferenceImages/${asicName}-${osName}"
    String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}/${asicName}-${osName}".replace('%2F', '_')

    try {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/luxteam/MultiplatformSampleProject.git')

        outputEnvironmentInfo(osName)
        
        unstash "app${osName}"

        dir('Tests')
        {
            if(options['updateRefs'])
            {
                executeGenTestRefCommand(osName)
                sendFiles(osName, './ReferenceImages/*.*', REF_PATH)

            }
            else
            {
                receiveFiles(osName, "${REF_PATH}/*", './ReferenceImages/')
                executeTestCommand(osName)
            }
        }                    
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        println(e.getStackTrace());

        dir('Tests')
        {
            if(options['updateRefs'])
            {
                executeGenTestRefCommand(osName)
                sendFiles(osName, './ReferenceImages/*.*', REF_PATH)

            }
            else
            {
                receiveFiles(osName, "${REF_PATH}/*", './ReferenceImages/')
                executeTestCommand(osName)
            }
        }        
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        sendFiles(osName, '*.log', "${PRJ_PATH}")
    }
}

def executeBuildWindows()
{
    bat """
    HOSTNAME > ${STAGE_NAME}.log

    """
}

def executeBuildOSX()
{
    sh """
    uname -a > ${STAGE_NAME}.log

    """
}

def executeBuildLinux()
{
    sh """
    uname -a > ${STAGE_NAME}.log

    """
}
def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')
        outputEnvironmentInfo(osName)

        switch(osName)
        {
        case 'Windows': 
            executeBuildWindows(); 
            break;
        case 'OSX':
            executeBuildOSX();
            break;
        default: 
            executeBuildLinux();
        }
        
        stash includes: 'Bin/**/*', name: "app${osName}"
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "${STAGE_NAME}.log"
        sendFiles(osName, '*.log', "${PRJ_PATH}")
        sendFiles(osName, '*.gtest.xml', "${PRJ_PATH}")
    }                        

}

def executeDeploy(Map options)
{
}

def call(String projectBranch = "", 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;OSX:Intel_Iris;Ubuntu;AMD_RX460', 
         Boolean updateRefs = false, Boolean enableNotifications = true) {
    
    multiplatform_pipeline(platforms, this.&executeBuild, this.&executeTests, null, 
                           [projectBranch:projectBranch,
                           updateRefs:updateRefs, 
                           enableNotifications:enableNotifications])
}
