def executeGenTestRefCommand(String osName, Map options)
{
    executeTestCommand(osName, options)
    
    switch(osName)
    {
    case 'Windows':
        bat """
        set PATH=c:\\python35\\;c:\\python35\\scripts\\;%PATH%

        python jobs_launcher\\common\\scripts\\generate_baseline.py --results_root Results\\Maya --baseline_root Baseline
        """
        break;
    case 'OSX':
        sh """
        echo 'sample image' > ./ReferenceImages/sample_image.txt
        """
        break;
    default:
        sh """
        echo 'sample image' > ./ReferenceImages/sample_image.txt
        """
    }
}

def executeTestCommand(String osName, Map options)
{
    switch(osName)
    {
    case 'Windows':
        dir('temp/install_plugin')
        {
            unstash 'appWindows'
            
            //crutch for new installer
            /*bat """
            rename "C:/Users/%USERNAME%/Documents/Radeon ProRender/Maya" Maya_bu
            """*/

            bat """
            msiexec /i "RadeonProRenderForMaya.msi" /quiet /qn PIDKEY=GPUOpen2016 /L+ie ${STAGE_NAME}.log /norestart
            """
            
            //continue the crutch
            /*bat """
            rename "C:/Users/%USERNAME%/Documents/Radeon ProRender/Maya_bu" Maya
            """*/
        }

        dir('scripts')
        {
            bat'''
            auto_config.bat
            '''
            bat'''
            run.bat
            '''
        }

        dir("Results/Maya")
        {
            bat """
            copy session_report_embed_img.html session_report_${STAGE_NAME}.html
            """

            bat """
            IF EXIST \"%CIS_TOOLS%\\sendFiles.bat\" (
                %CIS_TOOLS%\\sendFiles.bat session_report_${STAGE_NAME}.html ${options.JOB_PATH}
                )
            """                        
            archiveArtifacts "session_report_${STAGE_NAME}.html"
        }

        break;
    case 'OSX':
        sh """
        echo 'sample image' > ./OutputImages/sample_image.txt
        """
        break;
    default:
        sh """
        echo 'sample image' > ./OutputImages/sample_image.txt
        """
    }
}

def executeTests(String osName, String asicName, Map options)
{
    try {
        checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_maya.git')


        String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
        String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"
        
        outputEnvironmentInfo(osName)
        
        if(options['updateRefs'])
        {
            executeGenTestRefCommand(osName, options)
            sendFiles('./Baseline/', REF_PATH_PROFILE)
        }
        else
        {            
            receiveFiles("${REF_PATH_PROFILE}/*", './Baseline/')
            executeTestCommand(osName, options)
        }
        
        echo "Stashing test results to : ${options.testResultsName}"
        dir('Results/Maya')
        {
            stash includes: '**/*', name: "${options.testResultsName}"
        }
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        println(e.getStackTrace());

        dir('Tests')
        {
            if(options['updateRefs'])
            {
                //sendFiles('./ReferenceImages/*.*', JOB_PATH_PROFILE)
            }
            else
            {
                //receiveFiles("${JOB_PATH_PROFILE}/*", './ReferenceImages/')
            }
        }
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        sendFiles('*.log', "${options.JOB_PATH}")
    }
}

def executeBuildWindows(Map options)
{
    /*dir('RadeonProRenderMayaPlugin')
    {
        bat '''
        mklink /D ".\\ThirdParty\\AxfPackage\\"               "%workspace%\\RadeonProRenderThirdPartyComponents\\AxfPackage\\"
        mklink /D ".\\ThirdParty\\Expat 2.1.0\\"              "%workspace%\\RadeonProRenderThirdPartyComponents\\Expat 2.1.0\\"
        mklink /D ".\\ThirdParty\\OpenCL\\"                   "%workspace%\\RadeonProRenderThirdPartyComponents\\OpenCL\\"
        mklink /D ".\\ThirdParty\\OpenColorIO\\"              "%workspace%\\RadeonProRenderThirdPartyComponents\\OpenColorIO\\"
        mklink /D ".\\ThirdParty\\RadeonProImageProcessing\\" "%workspace%\\RadeonProRenderThirdPartyComponents\\RadeonProImageProcessing\\"
        mklink /D ".\\ThirdParty\\RadeonProRender SDK\\"      "%workspace%\\RadeonProRenderThirdPartyComponents\\RadeonProRender SDK\\"
        mklink /D ".\\ThirdParty\\RadeonProRender-GLTF\\"     "%workspace%\\RadeonProRenderThirdPartyComponents\\RadeonProRender-GLTF\\"
        mklink /D ".\\ThirdParty\\ffmpeg\\"                   "%workspace%\\RadeonProRenderThirdPartyComponents\\ffmpeg\\"
        mklink /D ".\\ThirdParty\\glew\\"                     "%workspace%\\RadeonProRenderThirdPartyComponents\\glew\\"
        mklink /D ".\\ThirdParty\\json\\"                     "%workspace%\\RadeonProRenderThirdPartyComponents\\json\\"
        mklink /D ".\\ThirdParty\\oiio\\"                     "%workspace%\\RadeonProRenderThirdPartyComponents\\oiio\\"
        mklink /D ".\\ThirdParty\\oiio-mac\\"                 "%workspace%\\RadeonProRenderThirdPartyComponents\\oiio-mac\\"
        mklink /D ".\\ThirdParty\\synColor\\"                 "%workspace%\\RadeonProRenderThirdPartyComponents\\synColor\\"
        '''                
    }*/
    dir('RadeonProRenderPkgPlugin\\MayaPkg')
    {
        bat """
        build_windows_installer.cmd >> ../../${STAGE_NAME}.log  2>&1
        """

        bat """
          for /r %%i in (RadeonProRender*.msi) do copy %%i RadeonProRenderForMaya.msi
        """
        
        stash includes: 'RadeonProRenderForMaya.msi', name: 'appWindows'
        archiveArtifacts "RadeonProRender*.msi"
    }
}

def executeBuildOSX(Map options)
{
    
}

def executeBuildLinux(Map options)
{
    
}

def executeBuild(String osName, Map options)
{
    try {        
        dir('RadeonProRenderMayaPlugin')
        {
            checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderMayaPlugin.git')
        }
        dir('RadeonProRenderThirdPartyComponents')
        {
            checkOutBranchOrScm(options['thirdpartyBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderThirdPartyComponents.git')
        }
        dir('RadeonProRenderPkgPlugin')
        {
            checkOutBranchOrScm(options['packageBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderPkgPlugin.git')
        }
        
        outputEnvironmentInfo(osName)

        switch(osName)
        {
        case 'Windows': 
            executeBuildWindows(options); 
            break;
        case 'OSX':
            executeBuildOSX(options);
            break;
        default: 
            executeBuildLinux(options);
        }
        
        //stash includes: 'Bin/**/*', name: "app${osName}"
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
/*        if(osName == 'Windows')
        {
            archiveArtifacts "RadeonProRenderPkgPlugin/MayaPkg/system/PluginInstaller/logs/*.log"
        }*/
        sendFiles('*.log', "${options.JOB_PATH}")
    }                        
}

def executeDeploy(Map options, List testResultList)
{
    try { 
        checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_maya.git')

        dir("summaryTestResults")
        {
            testResultList.each()
            {
                dir("$it")
                {
                    unstash "$it"
                }
            }
        }

        dir("jobs_launcher")
        {
            bat """
            build_summary_report.bat ..\\summaryTestResults
            """
        }

        dir("summaryTestResults")
        {
            //use "${options.JOB_PATH}"
            //use "${options.REF_PATH}"
            sendFiles('./summary_report_embed_img.html', "${options.JOB_PATH}")
            archiveArtifacts "summary_report_embed_img.html"
        }
        
        if(options['incrementVersion'])
        {
            echo "currentBuild.result : ${currentBuild.result}"
            if("${BRANCH_NAME}"=="master" && currentBuild.result != "FAILED")
            {
                dir('RadeonProRenderMayaPlugin')
                {
                    checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderMayaPlugin.git')

                    AUTHOR_NAME = bat (
                            script: "git show -s --format='%%an' HEAD ",
                            returnStdout: true
                            ).split('\r\n')[2].trim()

                    echo "The last commit was written by ${AUTHOR_NAME}."

                    if (AUTHOR_NAME != "'radeonprorender'") {
                        echo "Incrementing version of change made by ${AUTHOR_NAME}."

                        //String currentversion=version_read('FireRender.Maya.Src/common.h', '#define PLUGIN_VERSION')
                        String currentversion=version_read('version.h', '#define PLUGIN_VERSION')
                        echo "currentversion ${currentversion}"

                        new_version=version_inc(currentversion, 3)
                        echo "new_version ${new_version}"

                        version_write('FireRender.Maya.Src/common.h', '#define PLUGIN_VERSION', new_version)

                        String updatedversion=version_read('version.h', '#define PLUGIN_VERSION')
                        echo "updatedversion ${updatedversion}"

                        bat """
                            git add version.h
                            git commit -m "Update version build"
                            git push origin HEAD:master
                           """        
                    }
                }
            }
        }
    }
    catch (e) {
        currentBuild.result = "FAILED"
        
        println(e.toString());
        println(e.getMessage());
        println(e.getStackTrace());
        
        throw e
    }
    finally {
        //archiveArtifacts "*.log"
        //sendFiles('*.log', "${options.JOB_PATH}")
    }   
}


def call(String projectBranch = "", String thirdpartyBranch = "master", 
         String packageBranch = "master", String testsBranch = "master",
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI', 
         Boolean updateRefs = false, Boolean enableNotifications = true,
         Boolean incrementVersion = true) {

    String PRJ_NAME="RadeonProRenderMayaPlugin"
    String PRJ_ROOT="rpr-plugins"
    
    multiplatform_pipeline(platforms, this.&executeBuild, this.&executeTests, this.&executeDeploy, 
                           [projectBranch:projectBranch, 
                            thirdpartyBranch:thirdpartyBranch, 
                            packageBranch:packageBranch, 
                            testsBranch:testsBranch, 
                            updateRefs:updateRefs, 
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            incrementVersion:incrementVersion])
}
