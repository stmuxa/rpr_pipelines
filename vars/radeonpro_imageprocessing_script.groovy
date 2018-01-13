                        


def executeTestCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat "mkdir testSave"
        bat "..\\Bin\\Release\\x64\\UnitTest64.exe >> ..\\Test${current_profile}.log  2>&1"
        break;
    case 'OSX':
        sh "mkdir testSave"
        sh "../Bin/Release/x64/UnitTest64 >> ../Test${current_profile}.log  2>&1"
        break;
    default:
        sh "mkdir testSave"
        sh "../Bin/Release/x64/UnitTest64 >> ../Test${current_profile}.log  2>&1"
    }
}

def sendFiles(String osName, String local, String remote)
{
    if(osName == 'Windows')
    {
        bat """
            %CIS_TOOLS%\\sendFiles.bat ${local} ${remote}
        """
    }
    else
    {
        sh """
            ${CIS_TOOLS}/sendFiles.sh \"${local}\" ${remote}
        """
    }
}

def receiveFiles(String osName, String remote, String local)
{
    if(osName == 'Windows')
    {
        bat """
            %CIS_TOOLS%\\receiveFiles.bat ${remote} ${local}
        """
    }
    else
    {
        sh """
            ${CIS_TOOLS}/receiveFiles.sh \"${remote}\" ${local}
        """
    }
}

def printEnv(String osName)
{
    if(osName == 'Windows')
    {
         bat "set > ${STAGE_NAME}.log"
    }
    else
    {
         sh "env > ${STAGE_NAME}.log"
    }
}

def executeTests(String asicName, String projectBranch, Boolean updateRefs, String osName)
{
    //String PRJ_PATH="builds/rpr-core/RadeonProImageProcessor"
    //String REF_PATH="${PRJ_PATH}/ReferenceImages/${asicName}-${osName}"
    //String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}/${asicName}-${osName}".replace('%2F', '_')

    try {
        checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')

        printEnv(osName)
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
    bat '''
    HOSTNAME > Build_Windows_VS2015.log
    set msbuild="C:\\Program Files (x86)\\MSBuild\\14.0\\Bin\\MSBuild.exe"
    if not exist %msbuild% (
        set msbuild="C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe"
    )
    set target=build
    set maxcpucount=/maxcpucount 
    set PATH=C:\\Python27\\;%PATH%
    .\\Tools\\premake\\win\\premake5 vs2015 >> Build_Windows_VS2015.log 2>&1
    set solution=.\\RadeonImageFilters.sln
    rem %msbuild% /target:%target% %maxcpucount% /property:Configuration=Debug;Platform=x64 %parameters% %solution%
    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Release;Platform=x64 %parameters% %solution% >> Build_Windows_VS2015.log 2>&1
    '''
}

def executeBuildOSX()
{
    sh """
        uname -a > Build_${osName}.log
        Tools/premake/osx/premake5 --use_opencl --embed_kernels gmake >> Build_${osName}.log 2>&1
        make config=release_x64          >> Build_${osName}.log 2>&1
    """
}

def executeBuildLinux()
{
    sh """
    uname -a > Build_${linuxName}.log
    chmod +x Tools/premake/linux64/premake5
    Tools/premake/linux64/premake5 --use_opencl --embed_kernels gmake   >> Build_${linuxName}.log 2>&1
    make config=release_x64                                             >> Build_${linuxName}.log 2>&1
    """

}
def executeBuild(String projectBranch, String osName)
{
    try {
        checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')
        printEnv(osName)

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
        if(osName == 'Windows')
        {
            executeBuildWindows()
        }else
        if(osName == 'OSX')
        {
            executeBuildOSX()
        }else
        {
            executeBuildLinux()
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
def executePlatform(String osName, String gpuNames, Boolean updateRefs, String projectBranch)
{
    def retNode =  
    {
        try {
            node("${osName} && Builder")
            {
                stage("Build-${osName}")
                {
                    String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                    ws("WS/${JOB_NAME_FMT}") {
                        executeBuild(projectBranch, osName)
                    }
                }
            }

            if(gpuNames)
            {
                def testTasks = [:]
                gpuNames.split(',').each()
                {
                    String asicName = it
                    echo "Scheduling Test ${osName}:${asicName}"
                    testTasks["Test-${it}-${osName}"] = {
                        node("${osName} && Tester && OpenCL && gpu${asicName}")
                        {
                            stage("Test-${asicName}-${osName}")
                            {
                                executeTests(asicName, projectBranch, updateRefs, osName)
                            }
                        }
                    }
                }
                parallel testTasks
            }
            else
            {
                echo "No tests found for ${osName}"
            }
        }
        catch (e) {
            println(e.toString());
            println(e.getMessage());
            println(e.getStackTrace());        
            currentBuild.result = "FAILED"
            throw e
        }
    }
    return retNode
}

def call(String projectBranch = "", 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100;Ubuntu;OSX:Intel_Iris', 
         Boolean updateRefs = false, Boolean enableNotifications = true) {
      
    try {
        timestamps {
            def tasks = [:]
            
            platforms.split(';').each()
            {
                def (osName, gpuNames) = it.tokenize(':')
                                
                tasks[osName]=executePlatform(osName, gpuNames, updateRefs, projectBranch)
            }
            parallel tasks
        }
    }
    catch (e) {
        currentBuild.result = "FAILED"
    }
    finally {
        if(enableNotifications)
        {
            sendBuildStatusNotification(currentBuild.result)
        }
    }
}



