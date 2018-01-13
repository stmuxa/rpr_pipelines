def executeTestWindows(String asicName, String projectBranch, Boolean updateRefs, String osName = "Windows")
{
    def retNode = {
        node("${osName} && Tester && OpenCL && gpu${asicName}")
        {
            stage("Test-${asicName}-${osName}")
            {
                String PRJ_PATH="builds/rpr-core/RadeonProRender-Baikal"
                String REF_PATH="${PRJ_PATH}/ReferenceImages/${asicName}-${osName}"
                String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}/${asicName}-${osName}".replace('%2F', '_')

                try {
                    checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')

                    bat "set > ${STAGE_NAME}.log"
                    unstash "app${osName}"

                    dir('BaikalTest')
                    {
                        if(updateRefs)
                        {
                            bat """
                            ..\\Bin\\Release\\x64\\BaikalTest64.exe -genref 1 --gtest_output=xml:../${STAGE_NAME}_genref.gtest.xml >> ..\\${STAGE_NAME}_genref.log 2>&1
                            """
                            bat """
                                %CIS_TOOLS%\\sendFiles.bat ./ReferenceImages/*.* ${REF_PATH}
                            """
                        }
                        else
                        {
                            bat """
                                %CIS_TOOLS%\\receiveFiles.bat ${REF_PATH}/* ./ReferenceImages/
                            """
                            bat """
                            ..\\Bin\\Release\\x64\\BaikalTest64.exe --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log 2>&1
                            """
                        }
                    }                    
                }
                catch (e) {
                    if(updateRefs)
                    {
                        sh """
                            ${CIS_TOOLS}/sendFiles.sh ./ReferenceImages/*.* ${REF_PATH}
                        """
                    }
                    else
                    {
                        sh """
                            ${CIS_TOOLS}/sendFiles.sh ./OutputImages/*.* ${PRJ_PATH}
                        """
                    }
                    currentBuild.result = "FAILED"
                    throw e
                }
                finally {
                    archiveArtifacts "*.log"
                    junit "*.gtest.xml"
                }
            }
        }
    }
    return retNode
}

def executeTestOSX(String asicName, String projectBranch, Boolean updateRefs, String osName = "OSX")
{
    def retNode = {
        node("${osName} && Tester && OpenCL && gpu${asicName}")
        {
            stage("Test-${asicName}-${osName}")
            {
                String PRJ_PATH="builds/rpr-core/RadeonProRender-Baikal"
                String REF_PATH="${PRJ_PATH}/ReferenceImages/${asicName}-${osName}"
                String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}/${asicName}-${osName}".replace('%2F', '_')

                try {
                    checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')

                    sh "env > ${STAGE_NAME}.log"
                    unstash "app${osName}"

                    dir('BaikalTest')
                    {
                        if(updateRefs)
                        {
                            sh """
                                ../Bin/Release/x64/BaikalTest64 -genref 1 --gtest_output=xml:../${STAGE_NAME}_genref.gtest.xml >> ../${STAGE_NAME}_genref.log 2>&1
                            """
                            sh """
                                ${CIS_TOOLS}/sendFiles.sh \"./ReferenceImages/*\" ${REF_PATH}
                            """
                        }
                        else
                        {
                            sh """
                                ${CIS_TOOLS}/receiveFiles.sh \"${REF_PATH}/*\" ./ReferenceImages/
                            """
                            sh """
                                ../Bin/Release/x64/BaikalTest64 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
                            """
                        }
                    }
                }
                catch (Exception e) {
                    if(updateRefs)
                    {
                        sh """
                            ${CIS_TOOLS}/sendFiles.sh \"./ReferenceImages/*.*\" ${REF_PATH}
                        """
                    }
                    else
                    {
                        sh """
                            ${CIS_TOOLS}/sendFiles.sh \"./OutputImages/*.*\" ${PRJ_PATH}
                        """
                    }
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
        }
    }
    return retNode
}

def executeTestLinux(String asicName, String projectBranch, Boolean updateRefs, String osName)
{
    def retNode = {
        node("${osName} && Tester && OpenCL && gpu${asicName}")
        {
            stage("Test-${asicName}-${osName}")
            {
                String PRJ_PATH="builds/rpr-core/RadeonProRender-Baikal"
                String REF_PATH="${PRJ_PATH}/ReferenceImages/${asicName}-${osName}"
                String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}/${asicName}-${osName}".replace('%2F', '_')

                try {
                    checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')
                    
                    sh "env > ${STAGE_NAME}.log"
                    unstash "app${osName}"

                    dir('BaikalTest')
                    {
                        if(updateRefs)
                        {
                            sh """
                                export LD_LIBRARY_PATH=`pwd`/../Bin/Release/x64/:\${LD_LIBRARY_PATH}
                                ../Bin/Release/x64/BaikalTest64 -genref 1 --gtest_output=xml:../${STAGE_NAME}_genref.gtest.xml >> ../${STAGE_NAME}_genref.log 2>&1
                            """
                            sh """
                                ${CIS_TOOLS}/sendFiles.sh ./ReferenceImages/* ${REF_PATH}
                            """
                        }
                        else
                        {
                            sh """
                                ${CIS_TOOLS}/receiveFiles.sh ${REF_PATH}/* ./ReferenceImages/
                            """
                            sh """
                                export LD_LIBRARY_PATH=`pwd`/../Bin/Release/x64/:\${LD_LIBRARY_PATH}
                                ../Bin/Release/x64/BaikalTest64 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
                            """
                        }
                    }
                }
                catch (Exception e) {
                    if(updateRefs)
                    {
                        sh """
                            ${CIS_TOOLS}/sendFiles.sh ./ReferenceImages/*.* ${REF_PATH}
                        """
                    }
                    else
                    {
                        sh """
                            ${CIS_TOOLS}/sendFiles.sh ./OutputImages/*.* ${PRJ_PATH}
                        """
                    }

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

def executePlatform(String osName, String gpuNames, bool updateRefs, String projectBranch)
{
    def retNode =  
    {
        try {
            stage("BuildStage-${osName}")
            {
                def buildTasks = [:]
                if(osName == 'Windows')
                {
                    buildTasks["Build-${osName}"]=executeBuildWindowsVS2015(projectBranch)
                }else
                if(osName == 'OSX')
                {
                    buildTasks["Build-${osName}"]=executeBuildOSX(projectBranch)
                }else
                {
                    buildTasks["Build-${osName}"]=executeBuildLinux(projectBranch, osName)
                }
                parallel buildTasks
            }
            if(gpuNames)
            {
                stage("TestStage-${osName}")
                {
                    def testTasks = [:]
                    gpuNames.split(',').each()
                    {
                        if(osName == 'Windows')
                        {
                            testTasks["Test-${it}-${osName}"] = executeTestWindows(it, projectBranch)
                        }
                        else
                        if(osName == 'OSX')
                        {
                            testTasks["Test-${it}-${osName}"] = executeTestOSX(it, projectBranch)
                        }
                        else
                        {
                            testTasks["Test-${it}-${osName}"] = executeTestLinux(it, projectBranch, osName)
                        }
                        echo "Scheduling Test ${osName}:${it}"
                    }
                    parallel testTasks
                }
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
                                
                tasks[osName]=executePlatform(osName, gpuNames, buildsGroup, projectBranch, thirdpartyBranch, packageBranch)
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
