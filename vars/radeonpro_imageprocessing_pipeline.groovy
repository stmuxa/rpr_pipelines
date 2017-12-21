def executeTest(String asicName)
{
    def retNode = {
        node("Windows && Tester && OpenCL && gpu${asicName}") {
            environment { 
                current_host="${env.COMPUTERNAME}"
                current_profile="${asicName}-Windows"
            }
            steps {

                ws("WS/${JOB_NAME_FMT}") {
                    bat 'set'
                    checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')
                    unstash 'appWindows'
                }
            }
        }
    }
    return retNode
}

def executeTests()
{
    /*
    def tasks = [:]
    
    tasks["AMD_RXVEGA"] = executeTest('AMD_RXVEGA')
    parallel tasks
    */
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
                            post {
                                always {
                                    archiveArtifacts 'Build_Windows_VS2015.log'
                                }
                            }                     
                        }
                    }
                }
            }
            stage('Test') {
                steps {
                    executeTests()
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
