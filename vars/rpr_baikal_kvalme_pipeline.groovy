def executeGenTestRefCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat """
        ..\\Build\\bin\\Release\\BaikalTest.exe -genref 1 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log 2>&1
        """
        break;
    case 'OSX':
        sh """
            ../Build/bin/BaikalTest -genref 1 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
        """
        break;
    default:
        sh """
            export LD_LIBRARY_PATH=`pwd`/../Build/bin/:\${LD_LIBRARY_PATH}
            ../Build/bin/BaikalTest -genref 1 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
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
    String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
    String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"

    try {
        checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])

        outputEnvironmentInfo(osName)
        unstash "app${osName}"
        
        dir('BaikalTest')
        {
            if(options['updateRefs'])
            {
                executeGenTestRefCommand(osName)
                sendFiles('./ReferenceImages/*.*', "${REF_PATH_PROFILE}")
            }
            else
            {
                receiveFiles("${REF_PATH_PROFILE}/*", './ReferenceImages/')
                executeTestCommand(osName)
            }
        }                    
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        //println(e.getStackTrace());    
        
        dir('BaikalTest')
        {
            sendFiles('./ReferenceImages/*.*', "${JOB_PATH_PROFILE}/ReferenceImages")
            sendFiles('./OutputImages/*.*', "${JOB_PATH_PROFILE}/OutputImages")
        }
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        junit "*.gtest.xml"
    }
}

def executeBuildWindows()
{
    bat """
    mkdir Build
    cd Build
    cmake -DCMAKE_BUILD_TYPE=Release -G "Visual Studio 14 2015 Win64" .. >> ..\\${STAGE_NAME}.log 2>&1
    cmake --build . --config Release >> ..\\${STAGE_NAME}.log 2>&1
    """
}

def executeBuildOSX()
{
    sh """
    mkdir Build
    cd Build
    cmake -DCMAKE_BUILD_TYPE=Release .. >> ../${STAGE_NAME}.log 2>&1
    make >> ../${STAGE_NAME}.log 2>&1
    """
}

def executeBuildLinux()
{
    sh """
    mkdir Build
    cd Build
    cmake -DCMAKE_BUILD_TYPE=Release .. >> ../${STAGE_NAME}.log 2>&1
    make >> ../${STAGE_NAME}.log 2>&1
    """
}
def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])
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
        
        stash includes: 'Build/bin/**/*', name: "app${osName}"
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
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;OSX:Intel_Iris;Ubuntu:AMD_WX7100;CentOS7', 
         String PRJ_ROOT='rpr-core',
         String PRJ_NAME='RadeonProRender-Baikal-Kvalme',
         String projectRepo='https://github.com/Kvalme/RadeonProRender-Baikal.git',
         Boolean updateRefs = false, 
         Boolean enableNotifications = true) {

    multiplatform_pipeline(platforms, null, this.&executeBuild, this.&executeTests, null,
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
                            slackTocken:"${SLACK_BAIKAL_TOCKEN}"])
}
