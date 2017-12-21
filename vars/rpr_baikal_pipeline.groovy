

def call(String buildsGroup = "AutoBuilds", String projectBranch='') {

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
            UPLOAD_PATH="builds/Radeon-Pro/RadeonProRender-Baikal/${buildsGroup}/${JOB_BASE_NAME_FMT}/Build-${BUILD_ID}"
            BASELINE_PATH="builds/Radeon-Pro/RadeonProRender-Baikal/Tests/BaseLine"
        }      
      
        stages {
            stage('Build') {
                parallel {
                    stage('Build On Windows') {
                        agent {
                            label "Windows && VS2015"
                        }
                        steps {
                            bat 'set'
                            checkOutBranchOrScm(projectBranch, 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git')

                            bat '''
                            HOSTNAME > Build_Windows_VS2015.log

                            set msbuild="C:\\Program Files (x86)\\MSBuild\\14.0\\Bin\\MSBuild.exe"
                            if not exist %msbuild% (
                                set msbuild="C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe"
                            )

                            set target=build
                            set maxcpucount=/maxcpucount 
                            set PATH=C:\\Python27\\;%PATH%
                            rem .\\Tools\\premake\\win\\premake5 --rpr vs2015
                            .\\Tools\\premake\\win\\premake5 vs2015 >> Build_Windows_VS2015.log 2>&1
                            set solution=.\\Baikal.sln

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
                    stage('Build On OSX') {
                        agent {
                            label "OSX"
                        }
                        steps {
                            sh '''
                            uname -a > Build_OSX.log
                            Tools/premake/osx/premake5 gmake >> Build_OSX.log 2>&1
                            make config=release_x64          >> Build_OSX.log 2>&1
                            '''
                            stash includes: 'Bin/**/*', name: 'appOSX'
                        }
                        post {
                            always {
                                archiveArtifacts 'Build_OSX.log'
                            }
                        }
                    }
                    stage('Build On Ubuntu') {
                        agent {
                            label "Ubuntu"
                        }
                        steps {
                            sh '''
                            uname -a > Build_Ubuntu.log
                            chmod +x Tools/premake/linux64/premake5
                            Tools/premake/linux64/premake5 gmake    >> Build_Ubuntu.log 2>&1
                            make config=release_x64                 >> Build_Ubuntu.log 2>&1
                            '''
                            stash includes: 'Bin/**/*', name: 'appUbuntu'
                        }
                        post {
                            always {
                                archiveArtifacts 'Build_Ubuntu.log'
                            }
                        }
                    }
                }
            }
            stage('Test') {
                parallel {
                    stage('Test-Windows-WX9100') {
                        agent {
                            label "Windows && Tester && OpenCL && gpuAMD_WX9100"
                        }
                        steps {
                            bat '''
                            HOSTNAME > TestWindows_WX9100.log
                            '''
                            unstash 'appWindows'

                            dir('BaikalTest')
                            {
                                bat '''
                                ..\\Bin\\Release\\x64\\BaikalTest64.exe -genref 1 --gtest_output=xml:../TestWindows_WX9100.xml >> ..\\TestWindows_WX9100.log 2>&1
                                '''
                            }
                        }
                        post {
                            always {
                                junit 'TestWindows_WX9100.xml'
                                archiveArtifacts 'TestWindows_WX9100.log'
                            }
                        }
                    }
                    stage('Test-Windows-WX7100') {
                        agent {
                            label "Windows && Tester && OpenCL && gpuAMD_WX7100"
                        }
                        steps {
                            bat '''
                            HOSTNAME > TestWindows_WX7100.log
                            '''
                            unstash 'appWindows'

                            dir('BaikalTest')
                            {
                                bat '''
                                ..\\Bin\\Release\\x64\\BaikalTest64.exe -genref 1 --gtest_output=xml:../TestWindows_WX7100.xml >> ..\\TestWindows_WX7100.log 2>&1
                                '''
                            }
                        }
                        post {
                            always {
                                junit 'TestWindows_WX7100.xml'
                                archiveArtifacts 'TestWindows_WX7100.log'
                            }
                        }
                    }
                    stage('Test-OSX-IntelIris') {
                        agent {
                            label "OSX && Tester && OpenCL && gpuIntel_Iris"
                        }
                        steps {
                            sh '''
                            uname -a > TestOSX_Iris.log
                            '''
                            unstash 'appOSX'

                            dir('BaikalTest')
                            {
                                sh '''
                                ../Bin/Release/x64/BaikalTest64 -genref 1 --gtest_output=xml:../TestOSX_Iris.xml >> ../TestOSX_Iris.log 2>&1
                                '''
                            }
                        }
                        post {
                            always {
                                junit 'TestOSX_Iris.xml'
                                archiveArtifacts 'TestOSX_Iris.log'
                            }
                        }
                    }
                }
            }
        }
        post {
            always {
                echo 'sending notification result...'
                sendBuildStatusNotification(currentBuild.result, "${SLACK_BAIKAL_CHANNEL}", "${SLACK_BAIKAL_BASE_URL}", "${SLACK_BAIKAL_TOCKEN}")
            }
        }
    }
}
