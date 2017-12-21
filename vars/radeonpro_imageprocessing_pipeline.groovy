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

def executeTests(String projectBranch)
{
    def tasks = [:]
    
    tasks["AMD_RXVEGA"] = executeTestWindows('AMD_RXVEGA', projectBranch)
    tasks["AMD_WX9100"] = executeTestWindows('AMD_WX9100', projectBranch)
    tasks["AMD_WX7100"] = executeTestWindows('AMD_WX7100', projectBranch)
    tasks["NVIDIA_GF1080TI"] = executeTestWindows('NVIDIA_GF1080TI', projectBranch)
    tasks["AMD_R9_200"] = executeTestWindows('AMD_R9_200', projectBranch)
    tasks["AMD_RX460"] = executeTestWindows('AMD_RX460', projectBranch)
    parallel tasks
}

def call(String projectBranch='') {
  
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
                            //ws("WS/${JOB_NAME_FMT}") {
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
                            //}
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
                steps {
                    executeTests(projectBranch)
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
