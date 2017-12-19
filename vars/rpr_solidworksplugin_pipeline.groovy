

def call(Map pipelineParams) {
  
    pipeline {
        agent none
        options {
            timestamps()
            skipDefaultCheckout()
        }
        environment
        {
            JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')
            UPLOAD_PATH="builds/rpr-plugins/${JOB_NAME_FMT}/Build-${BUILD_ID}"
            BRANCH_NAME=pipelineParams.Branch
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
                                dir('RadeonProRenderSolidWorksAddin')
                                {
                                    checkout scm
                                }
                                dir('RadeonProRenderThirdPartyComponents')
                                {
                                    checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [
                                        [$class: 'CleanCheckout'],
                                        [$class: 'CheckoutOption', timeout: 30],
                                        [$class: 'CloneOption', timeout: 30]
                                        ], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/Radeon-Pro/RadeonProRenderThirdPartyComponents.git']]])
                                }
                                dir('RadeonProRenderPkgPlugin')
                                {
                                    checkout([$class: 'GitSCM', timeout: 30, branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [
                                        [$class: 'CleanCheckout'],
                                        [$class: 'CheckoutOption', timeout: 30],
                                        [$class: 'CloneOption', timeout: 60]
                                        ], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/Radeon-Pro/RadeonProRenderPkgPlugin.git']]])
                                }

                                dir('RadeonProRenderSolidWorksAddin')
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
                                dir('RadeonProRenderSolidWorksAddin')
                                {
                                    bat '''
                                    mkdir "bin\\Release\\"
                                    copy %SOLIDWORKS_SDK%\\*.* bin\\Release\\

                                    set msbuild="C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe"
                                    set msbuild="C:\\Program Files (x86)\\MSBuild\\14.0\\Bin\\MSBuild.exe"
                                    set target=build
                                    set maxcpucount=/maxcpucount 

                                    set solution=.\\RadeonProRenderSolidWorksAddin.sln
                                    set proj_name="FireRender.SolidWorks.PlugIn"
                                    c:\\nuget\\nuget.exe restore %solution%

                                    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Release;Platform=x64 %parameters% %solution%
                                    '''
                                }
                            
                                dir('RadeonProRenderPkgPlugin\\SolidWorksPkg')
                                {
                                    bat '''
                                    makeInstaller.bat
                                    '''

                                    bat '''
                                    IF EXIST "%CIS_TOOLS%\\sendFiles.bat" (
                                        %CIS_TOOLS%\\sendFiles.bat output/FireRender*.exe %UPLOAD_PATH%
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
