def executeGenTestRefCommand(String osName, Map options)
{
    executeTestCommand(osName, options)
    
    dir('scripts')
    {
        switch(osName)
        {
        case 'Windows':
            bat """
            make_results_baseline.bat
            """
            /*bat """
            set PATH=c:\\python35\\;c:\\python35\\scripts\\;%PATH%
            python jobs_launcher\\common\\scripts\\generate_baseline.py --results_root Work\\Results\\Blender --baseline_root Work\\Baseline
            """*/
            break;
        case 'OSX':
            sh """
            echo 'sample image' > ./ReferenceImages/sample_image.txt
            """
            break;
        default:
            /*sh """
            python jobs_launcher/common/scripts/generate_baseline.py --results_root Work/Results/Blender --baseline_root Work/Baseline
            """*/
            sh """
            ./make_results_baseline.sh
            """
        }
    }
}

def executeTestCommand(String osName, Map options)
{
    switch(osName)
    {
    case 'Windows':
        if(!options['skipBuild'])
        {
            dir('temp/install_plugin')
            {
                unstash 'appWindows'

                bat """
                msiexec /i "RadeonProRenderForMax.msi" /quiet /qn PIDKEY=GPUOpen2016 /L+ie ../../${STAGE_NAME}.log /norestart
                """
            }
        }

        dir('scripts')
        {
            /*bat'''
            auto_config.bat
            '''*/
            bat'''
            run.bat
            '''
        }

        dir("Work/Results/Max")
        {
            bat """
            copy session_report_embed_img.html session_report_${STAGE_NAME}.html
            """
                    
            archiveArtifacts "session_report_${STAGE_NAME}.html"
        }
        
        if(!options['skipBuild'])
        {
            dir("temp/install_plugin")
            {
                bat"""
                msiexec /x "RadeonProRenderForMax.msi" /quiet /L+ie ../../${STAGE_NAME}.log /norestart
                """
            }
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
        checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_max.git')


        String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
        String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"
        
        outputEnvironmentInfo(osName)
        
        if(options['updateRefs'])
        {
            executeGenTestRefCommand(osName, options)
            sendFiles('./Work/Baseline/', REF_PATH_PROFILE)
        }
        else
        {            
            receiveFiles("${REF_PATH_PROFILE}/*", './Work/Baseline/')
            executeTestCommand(osName, options)
        }

        echo "Stashing test results to : ${options.testResultsName}"
        
        dir('Work')
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
    String osName = 'Windows'
    /*
    dir('RadeonProRenderMaxPlugin')
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
    }*/

    
    dir('RadeonProRenderPkgPlugin\\MaxPkg2')
    {
        bat """
        build_windows_installer.cmd >> ../../${STAGE_NAME}.log  2>&1
        """

        //remove when installer will be redesigned same way as maya
        //sendFiles('RadeonProRender3dsMax*.msi', "${options.JOB_PATH}")
        archiveArtifacts "RadeonProRender3dsMax*.msi"
        
        //uncomment to use when installer will be redesigned same way as maya
        //sendFiles('output/_ProductionBuild/RadeonProRender*.msi', options[JOB_PATH])
 
        bat '''
        for /r %%i in (RadeonProRender*.msi) do copy %%i RadeonProRenderForMax.msi
        '''
        
        stash includes: 'RadeonProRenderForMax.msi', name: 'appWindows'
        
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
        dir('RadeonProRenderMaxPlugin')
        {
            checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderMaxPlugin.git')
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
        sendFiles('*.log', "${options.JOB_PATH}")
    }                        
}

def executeDeploy(Map options, List testResultList)
{
    try
    {
        checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_max.git')

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
            sendFiles('./summary_report_embed_img.html', "${options.JOB_PATH}")
            archiveArtifacts "summary_report_embed_img.html"
        }
        
        publishHTML([allowMissing: false, 
                     alwaysLinkToLastBuild: false, 
                     keepAll: true, 
                     reportDir: 'summaryTestResults', 
                     reportFiles: 'summary_report.html', reportName: 'Test Report', reportTitles: 'Summary Report'])
        
        if(options['incrementVersion'])
        {
            echo "currentBuild.result : ${currentBuild.result}"
            if("${BRANCH_NAME}"=="master" && currentBuild.result != "FAILED")
            {
                dir('RadeonProRenderMaxPlugin')
                {
                    checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderMaxPlugin.git')

                    AUTHOR_NAME = bat (
                            script: "git show -s --format='%%an' HEAD ",
                            returnStdout: true
                            ).split('\r\n')[2].trim()

                    echo "The last commit was written by ${AUTHOR_NAME}."

                    if (AUTHOR_NAME != "'radeonprorender'") {
                        echo "Incrementing version of change made by ${AUTHOR_NAME}."

                        String currentversion=version_read('version.h', '#define VERSION_STR')
                        echo "currentversion ${currentversion}"

                        new_version=version_inc(currentversion, 3)
                        echo "new_version ${new_version}"

                        version_write('version.h', '#define VERSION_STR', new_version)

                        String updatedversion=version_read('version.h', '#define VERSION_STR')
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
         String platforms = 'Windows', 
         Boolean updateRefs = false, Boolean enableNotifications = true,
         Boolean incrementVersion = true,
         Boolean skipBuild = false,
         String executionParameters = "") {

    String PRJ_NAME="RadeonProRenderMaxPlugin"
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
                            incrementVersion:incrementVersion,
                            skipBuild:skipBuild,
                            executionParameters:executionParameters])
}


