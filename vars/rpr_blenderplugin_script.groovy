
def executeTestWindows(String asicName, String buildsGroup, String testsBranch)
{
    def retNode = {
        node("Windows && Tester && OpenCL && gpu${asicName}") {
            String current_profile="${asicName}-Windows"

            stage("Test-${current_profile}") {
                try {
                    String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                    String JOB_BASE_NAME_FMT="${JOB_BASE_NAME}".replace('%2F', '_')
                    String UPLOAD_PATH="builds/rpr-plugins/RadeonProRenderBlenderPlugin/${buildsGroup}/${JOB_BASE_NAME_FMT}/Build-${BUILD_ID}"

                    checkout([$class: 'GitSCM', branches: [[name: "*/${testsBranch}"]], doGenerateSubmoduleConfigurations: false, extensions: [
                        [$class: 'CleanCheckout'],
                        [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: '', trackingSubmodules: false]
                        ], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/luxteam/jobs_test_blender.git']]])

                    bat 'set'
                    bat "set > Test${current_profile}.log"

                    dir('temp/install_plugin')
                    {
                        unstash 'appWindows'

                        bat """
                        msiexec /i "RadeonProRenderForBlender.msi" /quiet /qn PIDKEY=GPUOpen2016 /log install_blender_plugin_${current_profile}.log /norestart
                        """
                    }

                    dir('scripts')
                    {
                        bat """
                        runFull.bat >> ../Test${current_profile}.log  2>&1
                        """
                    }
                    dir("Results/Blender")
                    {
                        bat """
                        copy session_report_embed_img.html session_report_${current_profile}.html
                        rem copy session_report.html session_report_${current_profile}.html
                        """

                        bat """
                        IF EXIST \"%CIS_TOOLS%\\sendFiles.bat\" (
                            %CIS_TOOLS%\\sendFiles.bat session_report_${current_profile}.html ${UPLOAD_PATH}
                            )
                        """                        
                        archiveArtifacts "session_report_${current_profile}.html"
                    }
                }
                catch (e) {
                    // If there was an exception thrown, the build failed
                    currentBuild.result = "FAILED"
                    throw e
                }
                finally {
                    archiveArtifacts "Test${current_profile}.log"
                }
            }
        }
    }
    return retNode
}

def executeTestOSX(String asicName, String buildsGroup, String testsBranch, String osName = "OSX")
{
    def retNode = {
        node("OSX && Tester && OpenCL && gpu${asicName}") {
            String current_profile="${asicName}-${osName}"

            stage("Test-${current_profile}") {
                sh 'env'
                sh "env > Test${current_profile}.log"

                try {
                    unstash "app${osName}"
                }
                catch (e) {
                    // If there was an exception thrown, the build failed
                    currentBuild.result = "FAILED"
                    throw e
                }
                finally {
                    archiveArtifacts "Test${current_profile}.log"
                }
            }
        }
    }
    return retNode
}

def executeBuildWindowsVS2015(String buildsGroup, String projectBranch, String thirdpartyBranch, String packageBranch, String osName = "Windows")
{
    def retNode = {
        node("Windows && VS2015") {

            stage("Build-Windows") {
                
                String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                String JOB_BASE_NAME_FMT="${JOB_BASE_NAME}".replace('%2F', '_')
                String UPLOAD_PATH="builds/rpr-plugins/RadeonProRenderBlenderPlugin/${buildsGroup}/${JOB_BASE_NAME_FMT}/Build-${BUILD_ID}"

                ws("WS/${JOB_NAME_FMT}") {
                    bat "set"
                    bat "set > Build_${osName}.log"
                    try {
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
                            bat """
                            build.cmd %CIS_TOOLS%\\castxml\\bin\\castxml.exe >> ../Build_${osName}.log  2>&1
                            """
                        }                              
                        dir('RadeonProRenderPkgPlugin\\BlenderPkg')
                        {
                            bat """
                            build_win_installer.cmd >> ../../Build_${osName}.log  2>&1
                            """

                            bat """
                            IF EXIST \"%CIS_TOOLS%\\sendFiles.bat\" (
                                %CIS_TOOLS%\\sendFiles.bat out/_pb/RadeonProRender*.msi ${UPLOAD_PATH}
                                )
                            """

                            bat """
                                c:\\JN\\create_refhtml.bat build.html "https://builds.rpr.cis.luxoft.com/${UPLOAD_PATH}"
                            """
                            
                            archiveArtifacts 'build.html'

                            dir('out/_pb')
                            {
                                bat '''
                                    for /r %%i in (RadeonProRenderForBlender*.msi) do copy %%i ..\\..\\RadeonProRenderForBlender.msi
                                '''
                            }
                            stash includes: 'RadeonProRenderForBlender.msi', name: 'appWindows'
                        }
                    }
                    catch (e) {
                        // If there was an exception thrown, the build failed
                        currentBuild.result = "FAILED"
                        throw e
                    }
                    finally {
                        archiveArtifacts "Build_${osName}.log"
                    }
                }
            }
        }
    }
    return retNode
}

def executeBuildOSX(String buildsGroup, String projectBranch, String thirdpartyBranch, String packageBranch, String osName = "OSX")
{
    def retNode = {
        node("${osName} && Builder") {

            stage("Build-${osName}") {

                String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                String JOB_BASE_NAME_FMT="${JOB_BASE_NAME}".replace('%2F', '_')
                String UPLOAD_PATH="builds/rpr-plugins/RadeonProRenderBlenderPlugin/${buildsGroup}/${JOB_BASE_NAME_FMT}/Build-${BUILD_ID}"

                ws("WS/${JOB_NAME_FMT}") {
                    sh "env"
                    sh "env > Build_${osName}.log"
                    try {
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
                            sh """
                            ./build_osx.sh /usr/bin/castxml >> ../Build_${osName}.log  2>&1
                            """
                        }
                        dir('RadeonProRenderPkgPlugin/BlenderPkg')
                        {
                            sh """
                            ./build_osx_installer.sh >> ../../Build_${osName}.log  2>&1
                            """
                            
                            dir('.installer_build')
                            {
                                sh 'cp RadeonProRenderBlender*.dmg ../RadeonProRenderBlender.dmg'
                                
                                sh """
                                ${CIS_TOOLS}/sendFiles.sh RadeonProRenderBlender*.dmg ${UPLOAD_PATH}
                                """
                            }
                            /*
                            stash includes: 'RadeonProRenderBlender.dmg', name: "app${osName}"
                            */
                        }
                    }
                    catch (e) {
                        // If there was an exception thrown, the build failed
                        currentBuild.result = "FAILED"
                        throw e
                    }
                    finally {
                        archiveArtifacts "Build_${osName}.log"
                    }
                }
            }
        }
    }
    return retNode
}

def executeBuildLinux(String buildsGroup, String projectBranch, String thirdpartyBranch, String packageBranch, String osName)
{
    def retNode = {
        node("${osName}") {

            stage("Build-${osName}") {

                String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
                String JOB_BASE_NAME_FMT="${JOB_BASE_NAME}".replace('%2F', '_')
                String UPLOAD_PATH="builds/rpr-plugins/RadeonProRenderBlenderPlugin/${buildsGroup}/${JOB_BASE_NAME_FMT}/Build-${BUILD_ID}"

                ws("WS/${JOB_NAME_FMT}") {
                    sh "env"
                    sh "env > Build_${osName}.log"
                    try {
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
                            sh """
                            ./build.sh /usr/bin/castxml >> ../Build_${osName}.log  2>&1
                            """
                        }                              
                        dir('RadeonProRenderPkgPlugin/BlenderPkg')
                        {
                            sh """
                            ./build_linux_installer.sh >> ../../Build_${osName}.log  2>&1
                            """
                            
                            dir('.installer_build')
                            {
                                sh 'cp RadeonProRenderForBlender*.run ../RadeonProRenderForBlender.run'
                                
                                sh """
                                /var/data/JN/cis_tools/sendFiles.sh RadeonProRenderForBlender*.run ${UPLOAD_PATH}
                                """
                            }
                            //stash includes: 'RadeonProRenderForBlender.run', name: "app${osName}"
                        }
                    }
                    catch (e) {
                        // If there was an exception thrown, the build failed
                        currentBuild.result = "FAILED"
                        throw e
                    }
                    finally {
                        archiveArtifacts "Build_${osName}.log"
                    }
                }
            }
        }
    }
    return retNode
}

def executePlatform(String osName, String gpuNames, String buildsGroup, String projectBranch, String thirdpartyBranch, String packageBranch)
{
    if(osName == 'Windows')
    {
        executeBuildWindowsVS2015(buildsGroup, projectBranch, thirdpartyBranch, packageBranch)
    }else
    if(osName == 'OSX')
    {
        executeBuildOSX(buildsGroup, projectBranch, thirdpartyBranch, packageBranch)
    }else
    {
        executeBuildLinux(buildsGroup, projectBranch, thirdpartyBranch, packageBranch, osName)
    }
    
    def tasks = [:]
    gpuNames.split(',').each()
    {
        echo "parsed3 [${it}]"
        
        if(osName == 'Windows')
        {
            tasks[it] = executeTestWindows(it, buildsGroup, testsBranch)
        }
        else
        if(osName == 'OSX')
        {
            //tasks[it] = executeTestOSX(it, buildsGroup, testsBranch)
            echo "Not implemented Configuration ${it}"
        }
        else
        {
            //tasks[it] = executeTestLinux(it, buildsGroup, testsBranch)
            echo "Not implemented Configuration ${it}"
        }
    }
    parallel tasks
}

def call(String buildsGroup = "AutoBuilds", String projectBranch = "", String thirdpartyBranch = "master", 
         String packageBranch = "master", String testsBranch = "master", 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100;Ubuntu;OSX', Boolean enableNotifications = true) {
      
    try {
        timestamps {
            def tasks = [:]
            
            platforms.split(';').each()
            {
                echo "parsed0 [${it}]"

                def (osName, gpuNames) = it.tokenize(':')
                
                echo "parsed1 osName[${osName}] gpuNames[${gpuNames}]"
                
                tasks[osName]=executePlatform(osName, gpuNames, buildsGroup, projectBranch, thirdpartyBranch, packageBranch)
            }
            parallel tasks
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
