def executeTestCommand(String osName)
{
    
}

def executeTests(String osName, String asicName, Map options)
{
    
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
    .\\Tools\\premake\\win\\premake5 vs2015    >> ${STAGE_NAME}.log 2>&1 
    set solution=Build\\ProRenderGLTF.sln
    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Release;Platform=x64 %parameters% %solution% >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuildOSX()
{
     sh """
        chmod +x Tools/premake/linux64/premake5
        Tools/premake/osx/premake5 gmake   >> ${STAGE_NAME}.log 2>&1
        cd Build
        make config=release_x64                                         >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuildLinux()
{
    sh """
    chmod +x Tools/premake/linux64/premake5
    Tools/premake/linux64/premake5 gmake   >> ${STAGE_NAME}.log 2>&1
    cd Build
    make config=release_x64                                             >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProRender-GLTF.git')
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
            break;
        }

       // stash includes: 'Bin/**/*', name: "app${osName}"
        
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
    try {
        platformList.each()
        {
            String osName = it;
            dir(osName)
            {
                //unstash "app${osName}"
            }
        }
       
        
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        //archiveArtifacts "${STAGE_NAME}.log"
    }   
}

def call(String projectBranch = "", 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100;Ubuntu;OSX', 
         Boolean updateRefs = false, Boolean enableNotifications = true) {
    
    String PRJ_NAME="RadeonProRender-GLTF"
    String PRJ_ROOT="rpr-core"
    properties([[$class: 'BuildDiscarderProperty', strategy: 
                 [$class: 'LogRotator', artifactDaysToKeepStr: '', 
                  artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);

    multiplatform_pipeline(platforms, null, this.&executeBuild, null, this.&executeDeploy, 
                           [projectBranch:projectBranch, 
                            enableNotifications:enableNotifications,
                            BUILDER_TAG:'BuilderS',
                            executeBuild:true,
                            executeTests:false,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT])
}