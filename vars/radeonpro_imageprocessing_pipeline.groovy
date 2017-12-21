
def executeTests(String asicName)
{
    agent {
        label "Windows && Tester && OpenCL && gpuAMD_RXVEGA"
    }
    environment { 
        current_host="${env.COMPUTERNAME}"
        current_profile="AMD_RXVEGA-Windows"
    }
    steps {

        dir('jobs_test_maya')
        {
            checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [
                [$class: 'CleanCheckout'],
                [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: false, recursiveSubmodules: true, reference: '', trackingSubmodules: true]
                ], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/luxteam/jobs_test_maya.git']]])

        }
        dir('jobs_test_maya/temp/install_plugin')
        {
            unstash 'appWindows'

            bat '''
            msiexec /i "RadeonProRenderForMaya.msi" /quiet /qn PIDKEY=GPUOpen2016 /log install_maya_plugin_%current_profile%.log /norestart
            '''
        }

        dir('jobs_test_maya/scripts')
        {
            bat'''
            auto_config.bat
            '''
            bat'''
            run.bat
            '''
        }
        dir("jobs_test_maya/Results/Maya/${env.COMPUTERNAME}")
        {
            bat'''
            copy session_report_embed_img.html session_report_%current_profile%.html
            '''
            archiveArtifacts "session_report_${env.current_profile}.html"

            bat '''
            IF EXIST "%CIS_TOOLS%\\sendFiles.bat" (
                %CIS_TOOLS%\\sendFiles.bat session_report_%current_profile%.html %UPLOAD_PATH%
                )
            '''
        }
    }
}

def call(String projectBranch) {
  
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
                                checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')
                              
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
                        }
                        post {
                            always {
                                archiveArtifacts 'Build_Windows_VS2015.log'
                            }
                        }                     
                    }
                }
            }
            stage('Test') {
                parallel {
                    stage('Test-Windows-AMD_RXVEGA') {
                        executeTests('AMD_RXVEGA')
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
