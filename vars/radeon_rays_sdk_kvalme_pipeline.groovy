def executeTestCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat "..\\Bin\\Release\\x64\\UnitTest64.exe  --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log  2>&1"
        break;
    case 'OSX':
        sh "../Bin/Release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1"
        break;
    default:
        sh """
        export LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:../Bin/Release/x64
        ../Bin/Release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1
        """
    }  
}

def executeTests(String osName, String asicName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], options['projectURL'])

        outputEnvironmentInfo(osName)
        unstash "app${osName}"

        dir('UnitTest')
        {
            executeTestCommand(osName)
        }                
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        println(e.getStackTrace());    
        
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
    cmake -DCMAKE_BUILD_TYPE=Release ..
    make
    """
}

def executeBuildOSX()
{
    sh """
    mkdir Build
    cd Build
    cmake -DCMAKE_BUILD_TYPE=Release ..
    make
    """
}

def executeBuildLinux()
{
    sh """
    mkdir Build
    cd Build
    cmake -DCMAKE_BUILD_TYPE=Release ..
    make
    """
}

def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], options['projectURL'])
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
    }                        

}

def executeDeploy(Map options, List platformList, List testResultList)
{
}

def call(String projectBranch = "", 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;OSX:Intel_Iris;Ubuntu:AMD_WX7100', 
         Boolean enableNotifications = true) {

    String PRJ_NAME="RadeonRays_SDK"
    String PRJ_ROOT="rpr-core"
    String projectURL = 'https://github.com/Kvalme/RadeonRays_SDK.git'
    
    properties([[$class: 'BuildDiscarderProperty', strategy: 
                 [$class: 'LogRotator', artifactDaysToKeepStr: '', 
                  artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
    
    multiplatform_pipeline(platforms, null, this.&executeBuild, this.&executeTests, null, 
                           [projectBranch:projectBranch,
                            enableNotifications:enableNotifications,
                            executeBuild:true,
                            executeTests:true,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            BUILDER_TAG:'BuilderS',
                            projectURL:projectURL,
                            slackChannel:"${SLACK_BAIKAL_CHANNEL}",
                            slackBaseUrl:"${SLACK_BAIKAL_BASE_URL}",
                            slackTocken:"${SLACK_BAIKAL_TOCKEN}"])
}
