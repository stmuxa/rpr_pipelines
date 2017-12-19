def notifyBuild(String buildStatus = 'STARTED') {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'
 
  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>"""
 
  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }
 
  // Send notifications
  slackSend (color: colorCode, message: summary)
}

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    


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
                        environment
                        {
                            MAYA_x64_2015='C:\\Program Files\\Autodesk\\Maya2015\\'
                            MAYA_SDK_2016='C:\\Program Files\\Autodesk\\Maya2016\\devkit\\'
                            MAYA_x64_2016='C:\\Program Files\\Autodesk\\Maya2016\\'
                            MAYA_SDK_2016_5='C:\\Program Files\\Autodesk\\Maya2016.5\\devkit\\'
                            MAYA_x64_2016_5='C:\\Program Files\\Autodesk\\Maya2016.5\\'
                            MAYA_SDK_2017='C:\\Program Files\\Autodesk\\Maya2017\\devkit\\'
                            MAYA_x64_2017='C:\\Program Files\\Autodesk\\Maya2017\\'
                            MAYA_SDK_2018='C:\\Program Files\\Autodesk\\Maya2018\\devkit\\'
                            MAYA_x64_2018='C:\\Program Files\\Autodesk\\Maya2018\\'
                        }

                        steps {
                            ws("WS/${JOB_NAME_FMT}") {
                                bat 'set'
                                dir('RadeonProRenderMayaPlugin')
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

                                dir('RadeonProRenderMayaPlugin')
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
                                    '''                
                                }
                                dir('RadeonProRenderPkgPlugin\\MayaPkg')
                                {
                                    bat '''
                                    set FR_MAYA_PLUGIN_DEV_PATH=%workspace%\\RadeonProRenderMayaPlugin
                                    build_windows_installer.cmd
                                    '''

                                    bat '''
                                    IF EXIST "%CIS_TOOLS%\\sendFiles.bat" (
                                        %CIS_TOOLS%\\sendFiles.bat output/_ProductionBuild/RadeonProRender*.msi %UPLOAD_PATH%
                                        )
                                    '''

                                    bat '''
                                        c:\\JN\\create_refhtml.bat build.html "https://builds.rpr.cis.luxoft.com/%UPLOAD_PATH%"
                                    '''

                                    archiveArtifacts 'build.html'

                                    dir('output/_ProductionBuild')
                                    {
                                        bat '''
                                            for /r %%i in (RadeonProRenderForMaya*.msi) do copy %%i ..\\..\\RadeonProRenderForMaya.msi
                                        '''
                                    }
                                    stash includes: 'RadeonProRenderForMaya.msi', name: 'appWindows'
                                }
                            }
                        }
                        /*
                        post {
                            always {
                                archiveArtifacts 'Build_Windows_VS2015.log'
                            }
                        }*/
                    }
                    /*
                    stage('Build On OSX') {
                        agent {
                            label "OSX"
                        }
                        steps {
                            sh '''

                            '''
                        }
                        post {
                            always {
                                archiveArtifacts 'Build_OSX.log'
                            }
                        }
                    }
                    */
                }
            }
            stage('Test') {
                parallel {
                    stage('Test-Windows-AMD_WX9100') {
                        agent {
                            label "Windows && Tester && OpenCL && gpuAMD_WX9100"
                        }
                        environment { 
                            current_host="${env.COMPUTERNAME}"
                            current_profile="AMD_WX9100-Windows"
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
                    stage('Test-Windows-AMD_RXVEGA') {
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
                    stage('Test-Windows-AMD_WX7100') {
                        agent {
                            label "Windows && Tester && OpenCL && gpuAMD_WX7100"
                        }
                        environment { 
                            current_host="${env.COMPUTERNAME}"
                            current_profile="AMD_WX7100-Windows"
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
                    stage('Test-Windows-AMD_RX460') {
                        agent {
                            label "Windows && Tester && OpenCL && gpuAMD_RX460"
                        }
                        environment { 
                            current_host="${env.COMPUTERNAME}"
                            current_profile="AMD_RX460-Windows"
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
                    stage('Test-Windows-AMD_RX9200') {
                        agent {
                            label "Windows && Tester && OpenCL && gpuAMD_RX9200"
                        }
                        environment { 
                            current_host="${env.COMPUTERNAME}"
                            current_profile="AMD_RX9200-Windows"
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
                    stage('Test-Windows-NVIDIA_GF1080TI') {
                        agent {
                            label "Windows && Tester && OpenCL && gpuNVIDIA_GF1080TI"
                        }
                        environment { 
                            current_host="${env.COMPUTERNAME}"
                            current_profile="NVIDIA_GF1080TI-Windows"
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
                }
            }
        }
        post {
            always {
                echo 'sending notification result...'
                notifyBuild(currentBuild.result)
            }
        }
    }
    
}
