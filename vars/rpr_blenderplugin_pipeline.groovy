
def call(String buildsGroup = "AutoBuilds", String pluginBranch = "", String thirdpartyBranch = "master", String packageBranch = "master") {
  
    pipeline {
        agent none
        options {
            timestamps()
            skipDefaultCheckout()
        }
        environment
        {
            JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
            JOB_BASE_NAME_FMT="${JOB_BASE_NAME}".replace('%2F', '_')
            UPLOAD_PATH="builds/rpr-plugins/RadeonProRenderBlenderPlugin/${buildsGroup}/${JOB_BASE_NAME_FMT}/Build-${BUILD_ID}"
        }
        stages {
            stage('Build') {
                parallel {
                    stage('Build On Windows') {
                        agent {
                            label "Windows && VS2015"
                        }

                        steps {
                            ws("WS/${JOB_NAME_FMT}") {
                                bat 'set'
                                dir('RadeonProRenderBlenderAddon')
                                {
                                    checkOutBranchOrScm(pluginBranch, 'https://github.com/Radeon-Pro/RadeonProRenderBlenderAddon.git')
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
                        }
                    }
                    stage('Build On Ubuntu') {
                        agent {
                            label "Ubuntu"
                        }

                        steps {
                            ws("WS/${JOB_NAME_FMT}") {
                                sh 'env'
                                dir('RadeonProRenderBlenderAddon')
                                {
                                    checkOutBranchOrScm(pluginBranch, 'https://github.com/Radeon-Pro/RadeonProRenderBlenderAddon.git')
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
                    }
                }
            }
        }
        post {
            always {
                echo 'sending notification result...'
                sendBuildStatusNotification(currentBuild.result)
            }
        }
    }
}
