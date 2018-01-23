def executeTestCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat "mkdir testSave"
        bat "..\\Bin\\Release\\x64\\UnitTest64.exe  --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log  2>&1"
        break;
    case 'OSX':
        sh "mkdir testSave"
        sh "../Bin/Release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1"
        break;
    default:
        sh "mkdir testSave"
        sh "../Bin/Release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1"
    }
}

def executeTests(String osName, String asicName, Map options)
{
    //String PRJ_PATH="builds/rpr-core/RadeonProImageProcessor"
    //String REF_PATH="${PRJ_PATH}/ReferenceImages/${asicName}-${osName}"
    //String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}/${asicName}-${osName}".replace('%2F', '_')

    try {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')

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
        
        //send if needed
        //sendFiles(osName, './OutputImages/*.*', PRJ_PATH)

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
    set msbuild=\"C:\\Program Files (x86)\\MSBuild\\14.0\\Bin\\MSBuild.exe\"
    if not exist %msbuild% (
        set msbuild=\"C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe\"
    )
    set target=build
    set maxcpucount=/maxcpucount 
    set PATH=C:\\Python27\\;%PATH%
    .\\Tools\\premake\\win\\premake5 vs2015                             >> ${STAGE_NAME}.log 2>&1
    set solution=.\\RadeonImageFilters.sln
    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Release;Platform=x64 %parameters% %solution% >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuildOSX()
{
    sh """
        Tools/premake/osx/premake5 --use_opencl --embed_kernels gmake   >> ${STAGE_NAME}.log 2>&1
        make config=release_x64                                         >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuildLinux()
{
    sh """
    chmod +x Tools/premake/linux64/premake5
    Tools/premake/linux64/premake5 --use_opencl --embed_kernels gmake   >> ${STAGE_NAME}.log 2>&1
    make config=release_x64                                             >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')
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
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100;Ubuntu;OSX', 
         Boolean updateRefs = false, Boolean enableNotifications = true) {
    
    String PRJ_NAME="RadeonProRenderBlenderPlugin"
    String PRJ_ROOT="rpr-plugins"
    String PRJ_PATH="builds/${PRJ_ROOT}/${PRJ_NAME}"
    
    String REF_PATH="${PRJ_PATH}/ReferenceImages"
    String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
    
    multiplatform_pipeline(platforms, this.&executeBuild, this.&executeTests, null, 
                           [projectBranch:projectBranch, 
                           enableNotifications:enableNotifications])
}
