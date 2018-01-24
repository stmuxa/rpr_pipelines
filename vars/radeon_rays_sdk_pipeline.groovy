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
        sh "export LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:../Bin/Release/x64"
        sh "../Bin/Release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1"
    }  
}

def executeTests(String osName, String asicName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')

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
    HOSTNAME > ${STAGE_NAME}.log
    set msbuild="C:\\Program Files (x86)\\MSBuild\\14.0\\Bin\\MSBuild.exe"
    if not exist %msbuild% (
        set msbuild="C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe"
    )
    set target=build
    set maxcpucount=/maxcpucount 
    set PATH=C:\\Python27\\;%PATH%
    .\\Tools\\premake\\win\\premake5 --safe_math vs2015 >> ${STAGE_NAME}.log 2>&1
    set solution=.\\RadeonRays.sln
    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Release;Platform=x64 %parameters% %solution% >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuildOSX()
{
    sh """
    uname -a > ${STAGE_NAME}.log
    Tools/premake/osx/premake5 --safe_math gmake >> ${STAGE_NAME}.log 2>&1
    make config=release_x64          >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuildLinux()
{
    sh """
    uname -a > ${STAGE_NAME}.log
    chmod +x Tools/premake/linux64/premake5
    Tools/premake/linux64/premake5 --safe_math gmake    >> ${STAGE_NAME}.log 2>&1
    make config=release_x64                 >> ${STAGE_NAME}.log 2>&1
    """
}
def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonRays_SDK.git')
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

def executeDeploy(Map options)
{
}

def call(String projectBranch = "", 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;OSX:Intel_Iris;Ubuntu:AMD_WX7100', 
         Boolean enableNotifications = true) {

    String PRJ_NAME="RadeonRays_SDK"
    String PRJ_ROOT="rpr-core"

    multiplatform_pipeline(platforms, this.&executeBuild, this.&executeTests, null, 
                           [projectBranch:projectBranch,
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            slackChannel:"${SLACK_BAIKAL_CHANNEL}",
                            slackBaseUrl:"${SLACK_BAIKAL_BASE_URL}",
                            slackTocken:"${SLACK_BAIKAL_TOCKEN}"])
}
