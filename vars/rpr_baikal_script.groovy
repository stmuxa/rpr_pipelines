def executeTestWindows(String asicName, String projectBranch, String osName = "Windows")
{
    def retNode = {
        node("${osName} && Tester && OpenCL && gpu${asicName}")
        {
            stage("Test-${asicName}-${osName}")
            {
                try {
                    checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')
                    
                    bat "set > ${STAGE_NAME}.log"
                    unstash "app${osName}"

                    dir('BaikalTest')
                    {
                        bat """
                        ..\\Bin\\Release\\x64\\BaikalTest64.exe -genref 1 --gtest_output=xml:../${STAGE_NAME}_genref.xml >> ..\\${STAGE_NAME}_genref.log 2>&1
                        ..\\Bin\\Release\\x64\\BaikalTest64.exe --gtest_output=xml:../${STAGE_NAME}.xml >> ..\\${STAGE_NAME}.log 2>&1
                        """
                    }                    
                }
                catch (e) {
                    // If there was an exception thrown, the build failed
                    currentBuild.result = "FAILED"
                    throw e
                }
                finally {
                    archiveArtifacts "${STAGE_NAME}_genref.log"
                    archiveArtifacts "${STAGE_NAME}.log"
                    junit "${STAGE_NAME}_genref.xml"
                    junit "${STAGE_NAME}.xml"
                }
            }
        }
    }
    return retNode
}

def executeTestOSX(String asicName, String projectBranch, String osName = "OSX")
{
    def retNode = {
        node("${osName} && Tester && OpenCL && gpu${asicName}")
        {
            stage("Test-${asicName}-${osName}")
            {
                try {
                    checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')

                    sh "env > ${STAGE_NAME}.log"
                    unstash "app${osName}"

                    dir('BaikalTest')
                    {
                        sh """
                        ../Bin/Release/x64/BaikalTest64 -genref 1 --gtest_output=xml:../${STAGE_NAME}_genref.xml >> ../${STAGE_NAME}_genref.log 2>&1
                        ../Bin/Release/x64/BaikalTest64 --gtest_output=xml:../${STAGE_NAME}.xml >> ../${STAGE_NAME}.log 2>&1
                        """
                    }
                }
                catch (Exception e) {
                    println(e.toString());
                    println(e.getMessage());
                    println(e.getStackTrace());
                    
                    currentBuild.result = "FAILED"
                    throw e
                }
                finally {
                    archiveArtifacts "${STAGE_NAME}_genref.log"
                    archiveArtifacts "${STAGE_NAME}.log"
                    junit "${STAGE_NAME}_genref.xml"
                    junit "${STAGE_NAME}.xml"
                }
            }
        }
    }
    return retNode
}

def executeTestLinux(String asicName, String projectBranch, String osName)
{
    def retNode = {
        node("${osName} && Tester && OpenCL && gpu${asicName}")
        {
            stage("Test-${asicName}-${osName}")
            {
                try {
                    checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')

                    sh "env > ${STAGE_NAME}.log"
                    unstash "app${osName}"

                    dir('BaikalTest')
                    {
                        sh """
                        export LD_LIBRARY_PATH=`pwd`/../Bin/Release/x64/:\${LD_LIBRARY_PATH}

                        ../Bin/Release/x64/BaikalTest64 -genref 1 --gtest_output=xml:../${STAGE_NAME}_genref.xml >> ../${STAGE_NAME}_genref.log 2>&1
                        ../Bin/Release/x64/BaikalTest64 --gtest_output=xml:../${STAGE_NAME}.xml >> ../${STAGE_NAME}.log 2>&1
                        """
                    }
                }
                catch (Exception e) {
                    println(e.toString());
                    println(e.getMessage());
                    println(e.getStackTrace());
                    
                    currentBuild.result = "FAILED"
                    throw e
                }
                finally {
                    archiveArtifacts "${STAGE_NAME}_genref.log"
                    archiveArtifacts "${STAGE_NAME}.log"
                    junit "${STAGE_NAME}_genref.xml"
                    junit "${STAGE_NAME}.xml"
                }
            }
        }
    }
    return retNode
}
def executeBuildWindows(String projectBranch, String osName = "Windows")
{
    def retNode = {
        node("${osName} && Builder")
        {
            stage("Build-${osName}")
            {
                String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                
                ws("WS/${JOB_NAME_FMT}") {
                    try {
                        checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')

                        bat "set > ${STAGE_NAME}.log"

                        bat """
                        HOSTNAME > ${STAGE_NAME}.log
                        set msbuild="C:\\Program Files (x86)\\MSBuild\\14.0\\Bin\\MSBuild.exe"
                        if not exist %msbuild% (
                            set msbuild="C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe"
                        )
                        set target=build
                        set maxcpucount=/maxcpucount 
                        set PATH=C:\\Python27\\;%PATH%
                        .\\Tools\\premake\\win\\premake5 vs2015 >> ${STAGE_NAME}.log 2>&1
                        set solution=.\\Baikal.sln
                        %msbuild% /target:%target% %maxcpucount% /property:Configuration=Release;Platform=x64 %parameters% %solution% >> ${STAGE_NAME}.log 2>&1
                        """
                        stash includes: 'Bin/**/*', name: "app${osName}"

                    }
                    catch (e) {
                        // If there was an exception thrown, the build failed
                        currentBuild.result = "FAILED"
                        throw e
                    }
                    finally {
                        archiveArtifacts "${STAGE_NAME}.log"
                    }
                }
            }
        }
    }
    return retNode
}

def executeBuildOSX(String projectBranch, String osName = "OSX")
{
    def retNode = {
        node("${osName} && Builder")
        {
            stage("Build-${osName}")
            {
                String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                ws("WS/${JOB_NAME_FMT}") {
                    try {                        
                        checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')
                        
                        sh "env > Build_${osName}.log"
                        
                        sh """
                        uname -a > ${STAGE_NAME}.log
                        Tools/premake/osx/premake5 gmake >> ${STAGE_NAME}.log 2>&1
                        make config=release_x64          >> ${STAGE_NAME}.log 2>&1
                        """
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
            }
        }
    }
    return retNode
}

def executeBuildLinux(String projectBranch, String osName)
{
    def retNode = {
        node("${osName} && Builder")
        {
            stage("Build-${osName}")
            {
                String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                ws("WS/${JOB_NAME_FMT}") {
                    try {
                        checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')

                        sh "env > ${STAGE_NAME}.log"
               
                        sh """
                        uname -a > ${STAGE_NAME}.log
                        chmod +x Tools/premake/linux64/premake5
                        Tools/premake/linux64/premake5 gmake    >> ${STAGE_NAME}.log 2>&1
                        make config=release_x64                 >> ${STAGE_NAME}.log 2>&1
                        """
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
            }
        }
    }
    return retNode
}

def executeTests(String testPlatforms, String projectBranch)
{
    def tasks = [:]
    
    testPlatforms.split(';').each()
    {
        def (osName, gpuName) = "${it}".tokenize(':')
        if(osName == 'Windows')
        {
            tasks["${it}"] = executeTestWindows("${gpuName}", projectBranch)
        }
        else
        if(osName == 'OSX')
        {
            tasks["${it}"] = executeTestOSX("${gpuName}", projectBranch)
        }
        else
        {
            tasks["${it}"] = executeTestLinux("${gpuName}", projectBranch, osName)
        }
    }
    
    parallel tasks
}

def executeBuilds(String projectBranch)
{
    def tasks = [:]

    tasks["Build-Windows"] = executeBuildWindows(projectBranch)
    tasks["Build-Ubuntu"] = executeBuildLinux(projectBranch, "Ubuntu")
    tasks["Build-OSX"] = executeBuildOSX(projectBranch)

    parallel tasks
}

def call(String projectBranch = "", 
         String testPlatforms = 'Windows:AMD_RXVEGA;Windows:AMD_WX9100;Windows:AMD_WX7100;OSX:Intel_Iris', Boolean enableNotifications = true) {
      
    try {
        timestamps {
            executeBuilds(projectBranch)
            executeTests(testPlatforms, projectBranch)
        }
    }
    catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
    }
    finally {
        if(enableNotifications)
        {
            sendBuildStatusNotification(currentBuild.result)
        }
    }
}
