def executeTestWindows(String asicName, String projectBranch)
{
    def retNode = {
        node("Windows && Tester && OpenCL && gpu${asicName}") {
            String current_profile="${asicName}-Windows"

            stage("Test-${current_profile}") {
                bat 'set'
                checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')
                unstash 'appWindows'

                try {
                    dir('UnitTest')
                    {
                        bat "mkdir testSave"
                        bat "..\\Bin\\Release\\x64\\UnitTest64.exe >> ..\\Test${current_profile}.log"
                    }
                }
                finally {
                    archiveArtifacts "Test${current_profile}.log"
                }
            }
        }
    }
    return retNode
}
def executeTestOSX(String asicName, String projectBranch, String osName = "OSX")
{
    def retNode = {
        node("OSX && Tester && OpenCL && gpu${asicName}") {
            String current_profile="${asicName}-${osName}"

            stage("Test-${current_profile}") {
                sh 'env'
                checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')
                unstash "app${osName}"

                try {
                    dir('UnitTest')
                    {
                        sh "mkdir testSave"
                        sh "../Bin/Release/x64/UnitTest64 >> ../Test${current_profile}.log"
                    }
                }
                finally {
                    archiveArtifacts "Test${current_profile}.log"
                }
            }
        }
    }
    return retNode
}
def executeBuildWindowsVS2015(String projectBranch)
{
    def retNode = {
        node("Windows && VS2015") {

            stage("Build-Windows") {
                bat 'set'
                checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')

                try {
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
                    stash includes: 'Bin/**/*', name: 'appWindows'
                }
                finally {
                    archiveArtifacts 'Build_Windows_VS2015.log'
                }
            }
        }
    }
    return retNode
}
def executeBuildLinux(String projectBranch, String linuxName)
{
    def retNode = {
        node("${linuxName}") {

            stage("Build-${linuxName}") {
                sh 'env'
                checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')

                try {
                    sh """
                    uname -a > Build_${linuxName}.log
                    chmod +x Tools/premake/linux64/premake5
                    Tools/premake/linux64/premake5 --use_opencl --embed_kernels gmake   >> Build_${linuxName}.log 2>&1
                    make config=release_x64                                             >> Build_${linuxName}.log 2>&1
                    """
                    stash includes: 'Bin/**/*', name: "app${linuxName}"
                }
                finally {
                    archiveArtifacts "Build_${linuxName}.log"
                }
            }
        }
    }
    return retNode
}

def executeBuildOSX(String projectBranch, String osName = "OSX")
{
    def retNode = {
        node("${osName}") {

            stage("Build-${osName}") {
                sh 'env'
                checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')

                try {
                    sh """
                        uname -a > Build_${osName}.log
                        Tools/premake/osx/premake5 --use_opencl --embed_kernels gmake >> Build_${osName}.log 2>&1
                        make config=release_x64          >> Build_${osName}.log 2>&1
                    """
                    stash includes: 'Bin/**/*', name: "app${osName}"
                }
                finally {
                    archiveArtifacts "Build_${osName}.log"
                }
            }
        }
    }
    return retNode
}

def executeTests(String projectBranch, String testPlatforms)
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
            /*
            tasks["${it}"] = executeTestLinux("${gpuName}", projectBranch, osName)
            */
            echo "Invalid Test Configuration ${it}"
        }
    }
    
    parallel tasks
}
def executeBuilds(String projectBranch)
{
    def tasks = [:]

    tasks["Build-Windows"] = executeBuildWindowsVS2015(projectBranch)
    tasks["Build-Ubuntu"] = executeBuildLinux(projectBranch, "Ubuntu")
    tasks["Build-OSX"] = executeBuildOSX(projectBranch)

    parallel tasks
}
def call(String projectBranch='', String testPlatforms = 'Windows:AMD_RXVEGA;Windows:AMD_WX9100;Windows:AMD_WX7100', Boolean enableNotifications = true) {
      
    try {
        timestamps {
            executeBuilds(projectBranch)
            executeTests(projectBranch, testPlatforms)
        }
    }
    finally {
        if(enableNotifications)
        {
            sendBuildStatusNotification(currentBuild.result)
        }
    }
}
