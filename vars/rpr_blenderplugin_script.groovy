
def executeTestWindows(String asicName, String testsBranch)
{
    def retNode = {
        node("Windows && Tester && OpenCL && gpu${asicName}") {
            String current_profile="${asicName}-Windows"

            stage("Test-${current_profile}") {
                bat 'set' > "Test${current_profile}.log"

                try {
                    unstash 'appWindows'
                }
                finally {
                    archiveArtifacts "Test${current_profile}.log"
                }
            }
        }
    }
    return retNode
}

def executeTestOSX(String asicName, String testsBranch, String osName = "OSX")
{
    def retNode = {
        node("OSX && Tester && OpenCL && gpu${asicName}") {
            String current_profile="${asicName}-${osName}"

            stage("Test-${current_profile}") {
                sh 'env' > "Test${current_profile}.log"

                try {
                    unstash "app${osName}"
                }
                finally {
                    archiveArtifacts "Test${current_profile}.log"
                }
            }
        }
    }
    return retNode
}

def executeBuildWindowsVS2015(String projectBranch, String thirdpartyBranch, String packageBranch)
{
    def retNode = {
        node("Windows && VS2015") {

            stage("Build-Windows") {
                bat 'set'

                try {
                    ws("WS/${JOB_NAME_FMT}") {
                        dir('RadeonProRenderBlenderAddon')
                        {
                            checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProRenderBlenderAddon.git')
                        }
                        dir('RadeonProRenderThirdPartyComponents')
                        {
                            checkOutBranchOrScm(thirdpartyBranch, 'https://github.com/Radeon-Pro/RadeonProRenderThirdPartyComponents.git')
                        }
                        dir('RadeonProRenderPkgPlugin')
                        {
                            checkOutBranchOrScm(packageBranch, 'https://github.com/Radeon-Pro/RadeonProRenderPkgPlugin.git')
                        }

                        dir('RadeonProRenderBlenderAddon')
                        {
                            bat '''
                            mklink /D ".\\ThirdParty\\AxfPackage\\"              "%workspace%\\RadeonProRenderThirdPartyComponents\\AxfPackage\\"
                            mklink /D ".\\ThirdParty\\Expat 2.1.0\\"             "%workspace%\\RadeonProRenderThirdPartyComponents\\Expat 2.1.0\\"
                            mklink /D ".\\ThirdParty\\ffmpeg\\"                  "%workspace%\\RadeonProRenderThirdPartyComponents\\ffmpeg\\"
                            mklink /D ".\\ThirdParty\\glew\\"                    "%workspace%\\RadeonProRenderThirdPartyComponents\\glew\\"
                            mklink /D ".\\ThirdParty\\oiio\\"                    "%workspace%\\RadeonProRenderThirdPartyComponents\\oiio\\"
                            mklink /D ".\\ThirdParty\\OpenCL\\"                  "%workspace%\\RadeonProRenderThirdPartyComponents\\OpenCL\\"
                            mklink /D ".\\ThirdParty\\RadeonProRender SDK\\"     "%workspace%\\RadeonProRenderThirdPartyComponents\\RadeonProRender SDK\\"
                            mklink /D ".\\ThirdParty\\RadeonProRender-GLTF\\"    "%workspace%\\RadeonProRenderThirdPartyComponents\\RadeonProRender-GLTF\\"
                            mklink /D ".\\ThirdParty\\RadeonProImageProcessing\\"    "%workspace%\\RadeonProRenderThirdPartyComponents\\RadeonProImageProcessing\\"
                            '''                                    
                        }
                        dir('RadeonProRenderBlenderAddon')
                        {
                            bat '''
                            build.cmd %CIS_TOOLS%\\castxml\\bin\\castxml.exe
                            '''
                        }                              
                        dir('RadeonProRenderPkgPlugin\\BlenderPkg')
                        {
                            bat '''
                            build_win_installer.cmd
                            '''

                            bat '''
                            IF EXIST "%CIS_TOOLS%\\sendFiles.bat" (
                                %CIS_TOOLS%\\sendFiles.bat out/_pb/RadeonProRender*.msi %UPLOAD_PATH%
                                )
                            '''

                            bat '''
                                c:\\JN\\create_refhtml.bat build.html "https://builds.rpr.cis.luxoft.com/%UPLOAD_PATH%"
                            '''
                            
                            archiveArtifacts 'build.html'
                        }
                    }
                    dir('output/_ProductionBuild')
                    {
                        bat '''
                            for /r %%i in (RadeonProRenderForBlender*.msi) do copy %%i ..\\..\\RadeonProRenderForBlender.msi
                        '''
                    }
                    stash includes: 'RadeonProRenderForBlender.msi', name: 'appWindows'
                }
                finally {
                    archiveArtifacts 'Build_Windows_VS2015.log'
                }
            }
        }
    }
    return retNode
}

def executeBuildLinux(String projectBranch, String thirdpartyBranch, String packageBranch, String osName)
{
    def retNode = {
        node("${osName}") {

            stage("Build-${osName}") {
                sh 'env' > "Build_${osName}.log"

                try {
                    ws("WS/${JOB_NAME_FMT}") {
                        dir('RadeonProRenderBlenderAddon')
                        {
                            checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProRenderBlenderAddon.git')
                        }
                        dir('RadeonProRenderThirdPartyComponents')
                        {
                            checkOutBranchOrScm(thirdpartyBranch, 'https://github.com/Radeon-Pro/RadeonProRenderThirdPartyComponents.git')
                        }
                        dir('RadeonProRenderPkgPlugin')
                        {
                            checkOutBranchOrScm(packageBranch, 'https://github.com/Radeon-Pro/RadeonProRenderPkgPlugin.git')
                        }

                        dir('RadeonProRenderBlenderAddon/ThirdParty')
                        {
                            sh '''
                            ./unix_update.sh
                            '''                                    
                        }
                        dir('RadeonProRenderBlenderAddon')
                        {
                            sh '''
                            ./build.sh /usr/bin/castxml
                            '''
                        }                              
                        dir('RadeonProRenderPkgPlugin/BlenderPkg')
                        {
                            sh '''
                            ./build_linux_installer.sh
                            '''
                        }
                    }
                }
                finally {
                    archiveArtifacts "Build_${osName}.log"
                }
            }
        }
    }
    return retNode
}

def executeTests(String testsBranch, String testPlatforms)
{
    def tasks = [:]
    
    testPlatforms.split(';').each()
    {
        def (osName, gpuName) = "${it}".tokenize(':')
        if(osName == 'Windows')
        {
            tasks["${it}"] = executeTestWindows("${gpuName}", testsBranch)
        }
        else /*
        if(osName == 'OSX')
        {
            tasks["${it}"] = executeTestOSX("${gpuName}", projectBranch)
        }
        else */
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
    /*tasks["Build-OSX"] = executeBuildOSX(projectBranch)*/

    parallel tasks
}

def call(String projectBranch, String thirdpartyBranch, String packageBranch, String testsBranch, String testPlatforms = 'Windows:AMD_RXVEGA;Windows:AMD_WX9100;Windows:AMD_WX7100', Boolean enableNotifications = true) {
      
    try {
        timestamps {
            executeBuilds(projectBranch, thirdpartyBranch, packageBranch)
            executeTests(testsBranch, testPlatforms)
        }
    }
    finally {
        if(enableNotifications)
        {
            sendBuildStatusNotification(currentBuild.result)
        }
    }
}
