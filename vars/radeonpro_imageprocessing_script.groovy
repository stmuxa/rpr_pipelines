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
def executeTests(String projectBranch, String testPlatforms)
{
    def tasks = [:]
    
    testPlatforms.split(';').each()
    {
        tasks[${it}] = executeTestWindows(${it}, projectBranch)
    }
    
    /*
    tasks["NVIDIA_GF1080TI"] = executeTestWindows('NVIDIA_GF1080TI', projectBranch)
    tasks["AMD_R9_200"] = executeTestWindows('AMD_R9_200', projectBranch)
    tasks["AMD_RX460"] = executeTestWindows('AMD_RX460', projectBranch)
    */
    parallel tasks
}
def executeBuilds(String projectBranch)
{
    def tasks = [:]

    tasks["Windows"] = executeBuildWindowsVS2015(projectBranch)

    parallel tasks
}
def call(String projectBranch='', String testPlatforms = 'AMD_RXVEGA;AMD_WX9100;AMD_WX7100') {
      
    try {
        timestamps {
            executeBuilds(projectBranch)
            executeTests(projectBranch, testPlatforms.split(';'))
        }
    }
    finally {
        if("${EnableNotification}" == "true")
        {
            sendBuildStatusNotification(currentBuild.result)
        }
    }
}
