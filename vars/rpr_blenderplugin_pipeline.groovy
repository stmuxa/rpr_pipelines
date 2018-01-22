def executeGenTestRefCommand(String osName, Map options)
{
    executeTestCommand(osName, options)
    
    switch(osName)
    {
    case 'Windows':
        bat """
        set PATH=c:\\python35\\;c:\\python35\\scripts\\;%PATH%

        python jobs_launcher\\common\\scripts\\generate_baseline.py --results_root Results\\Blender --baseline_root Baseline
        """
        break;
    case 'OSX':
        sh """
        echo 'sample image' > ./ReferenceImages/sample_image.txt
        """
        break;
    default:
        sh """
        python jobs_launcher/common/scripts/generate_baseline.py --results_root Results/Blender --baseline_root Baseline
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

            bat """
            msiexec /i "RadeonProRenderForBlender.msi" /quiet /qn PIDKEY=GPUOpen2016 /L+ie ${STAGE_NAME}.log /norestart
            """
        }

        dir('scripts')
        {
            bat """
            run.bat >> ../${STAGE_NAME}.log  2>&1
            """
        }

        dir("Results/Blender")
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
        dir("scripts")
        {
            sh """
            ./run.sh >> ../${STAGE_NAME}.log 2>&1
            """
        }
        dir("Results/Blender")
        {
            sh """
            cp session_report_embed_img.html session_report_${STAGE_NAME}.html
            """
            
            sendFiles(osName, "session_report_${STAGE_NAME}.html", "${options.JOB_PATH}")
            
            archiveArtifacts "session_report_${STAGE_NAME}.html"
        }   
    }
}

def executeTests(String osName, String asicName, Map options)
{
    try {
        checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_blender.git')


        String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
        String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"
        
        outputEnvironmentInfo(osName)
        
        if(options['updateRefs'])
        {
            executeGenTestRefCommand(osName, options)
            sendFiles(osName, './Baseline/', REF_PATH_PROFILE)
        }
        else
        {            
            receiveFiles(osName, "${REF_PATH_PROFILE}/*", './Baseline/')
            executeTestCommand(osName, options)
        }

        echo "Stashing test results to : ${options.testResultsName}"
        
        dir('Results/Blender')
        {
            stash includes: '**/*', name: "${options.testResultsName}"
        }
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        println(e.getStackTrace());

        // TODO: dir Tests doesn't exist
        dir('Tests')
        {
            if(options['updateRefs'])
            {
                //sendFiles(osName, './ReferenceImages/*.*', JOB_PATH_PROFILE)
            }
            else
            {
                //receiveFiles(osName, "${JOB_PATH_PROFILE}/*", './ReferenceImages/')
            }
        }
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        sendFiles(osName, '*.log', "${options.JOB_PATH}")
    }
}

def executeBuildWindows(Map options)
{
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
        build.cmd %CIS_TOOLS%\\castxml\\bin\\castxml.exe >> ../${STAGE_NAME}.log  2>&1
        """
    }                              
    dir('RadeonProRenderPkgPlugin\\BlenderPkg')
    {
        bat """
        build_win_installer.cmd >> ../../${STAGE_NAME}.log  2>&1
        """

        bat """
        IF EXIST \"%CIS_TOOLS%\\sendFiles.bat\" (
            %CIS_TOOLS%\\sendFiles.bat out/_pb/RadeonProRender*.msi ${options.JOB_PATH}
            )
        """

        bat """
            c:\\JN\\create_refhtml.bat build.html "https://builds.rpr.cis.luxoft.com/${options.JOB_PATH}"
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

def executeBuildOSX(Map options)
{
    dir('RadeonProRenderBlenderAddon/ThirdParty')
    {
        sh '''
            ThirdPartyDir="../../RadeonProRenderThirdPartyComponents"

            if [ -d "$ThirdPartyDir" ]; then
                echo Updating $ThirdPartyDir

                rm -rf AxfPackage
                rm -rf "Expat 2.1.0"
                rm -rf OpenCL
                rm -rf OpenColorIO
                rm -rf "RadeonProImageProcessing"
                rm -rf "RadeonProRender SDK"
                rm -rf RadeonProRender-GLTF
                rm -rf ffmpeg
                rm -rf glew
                rm -rf json
                rm -rf oiio
                rm -rf oiio-mac
                rm -rf synColor

                cp -r $ThirdPartyDir/AxfPackage AxfPackage
                cp -r "$ThirdPartyDir/Expat 2.1.0" "Expat 2.1.0"
                cp -r $ThirdPartyDir/OpenCL OpenCL
                cp -r $ThirdPartyDir/OpenColorIO OpenColorIO
                cp -r $ThirdPartyDir/RadeonProImageProcessing RadeonProImageProcessing
                cp -r "$ThirdPartyDir/RadeonProRender SDK" "RadeonProRender SDK"
                cp -r $ThirdPartyDir/RadeonProRender-GLTF RadeonProRender-GLTF
                cp -r $ThirdPartyDir/ffmpeg ffmpeg
                cp -r $ThirdPartyDir/glew glew
                cp -r $ThirdPartyDir/json json
                cp -r $ThirdPartyDir/oiio oiio
                cp -r $ThirdPartyDir/oiio-mac oiio-mac
                cp -r $ThirdPartyDir/synColor synColor

            else
                echo Cannot update as $ThirdPartyDir missing
            fi
            '''                                    
    }
    dir('RadeonProRenderBlenderAddon')
    {
        sh """
        ./build_osx.sh /usr/bin/castxml >> ../${STAGE_NAME}.log  2>&1
        """
    }
    dir('RadeonProRenderPkgPlugin/BlenderPkg')
    {
        sh """
        ./build_osx_installer.sh >> ../../${STAGE_NAME}.log  2>&1
        """

        dir('installer_build')
        {
            sh 'cp RadeonProRenderBlender*.dmg ../RadeonProRenderBlender.dmg'

            sh """
            ${CIS_TOOLS}/sendFiles.sh RadeonProRenderBlender*.dmg ${options.JOB_PATH}
            """
        }
        //stash includes: 'RadeonProRenderBlender.dmg', name: "app${osName}"
    }
}

def executeBuildLinux(Map options)
{
    dir('RadeonProRenderBlenderAddon/ThirdParty')
    {
        sh '''
            ThirdPartyDir="../../RadeonProRenderThirdPartyComponents"

            if [ -d "$ThirdPartyDir" ]; then
                echo Updating $ThirdPartyDir

                rm -rf AxfPackage
                rm -rf "Expat 2.1.0"
                rm -rf OpenCL
                rm -rf OpenColorIO
                rm -rf "RadeonProImageProcessing"
                rm -rf "RadeonProRender SDK"
                rm -rf RadeonProRender-GLTF
                rm -rf ffmpeg
                rm -rf glew
                rm -rf json
                rm -rf oiio
                rm -rf oiio-mac
                rm -rf synColor

                cp -r $ThirdPartyDir/AxfPackage AxfPackage
                cp -r "$ThirdPartyDir/Expat 2.1.0" "Expat 2.1.0"
                cp -r $ThirdPartyDir/OpenCL OpenCL
                cp -r $ThirdPartyDir/OpenColorIO OpenColorIO
                cp -r $ThirdPartyDir/RadeonProImageProcessing RadeonProImageProcessing
                cp -r "$ThirdPartyDir/RadeonProRender SDK" "RadeonProRender SDK"
                cp -r $ThirdPartyDir/RadeonProRender-GLTF RadeonProRender-GLTF
                cp -r $ThirdPartyDir/ffmpeg ffmpeg
                cp -r $ThirdPartyDir/glew glew
                cp -r $ThirdPartyDir/json json
                cp -r $ThirdPartyDir/oiio oiio
                cp -r $ThirdPartyDir/oiio-mac oiio-mac
                cp -r $ThirdPartyDir/synColor synColor

            else
                echo Cannot update as $ThirdPartyDir missing
            fi
        '''                                
    }
    dir('RadeonProRenderBlenderAddon')
    {
        sh """
        ./build.sh /usr/bin/castxml >> ../${STAGE_NAME}.log  2>&1
        """
    }                              
    dir('RadeonProRenderPkgPlugin/BlenderPkg')
    {
        sh """
        ./build_linux_installer.sh >> ../../${STAGE_NAME}.log  2>&1
        """

        dir('installer_build')
        {
            sh 'cp RadeonProRenderForBlender*.run ../RadeonProRenderForBlender.run'

            sh """
            /var/data/JN/cis_tools/sendFiles.sh RadeonProRenderForBlender*.run ${options.JOB_PATH}
            """
        }
        //stash includes: 'RadeonProRenderForBlender.run', name: "app${osName}"
    }
}

def executeBuild(String osName, Map options)
{
    try {        
        dir('RadeonProRenderBlenderAddon')
        {
            checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderBlenderAddon.git')
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
        sendFiles(osName, '*.log', "${options.JOB_PATH}")
    }                        

}

def executeDeploy(Map options, List testResultList)
{
    checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_blender.git')

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
        sendFiles(osName, './summary_report_embed_img.html', "${options.JOB_PATH}")
        archiveArtifacts "summary_report_embed_img.html"
    }
}

def call(String projectBranch = "", String thirdpartyBranch = "master", 
         String packageBranch = "master", String testsBranch = "master",
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;Ubuntu:AMD_WX7100', 
         //String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;OSX;Ubuntu:AMD_WX7100', 
         //String platforms = 'Windows;OSX;Ubuntu', 
         Boolean updateRefs = false, Boolean enableNotifications = true) {

    String PRJ_PATH="builds/rpr-plugins/RadeonProRenderBlenderPlugin"
    String REF_PATH="${PRJ_PATH}/ReferenceImages"
    String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
    
    multiplatform_pipeline(platforms, this.&executeBuild, this.&executeTests, this.&executeDeploy, 
                           [projectBranch:projectBranch, 
                            thirdpartyBranch:thirdpartyBranch, 
                            packageBranch:packageBranch, 
                            testsBranch:testsBranch, 
                            updateRefs:updateRefs, 
                            enableNotifications:enableNotifications,
                            PRJ_PATH:PRJ_PATH,
                            REF_PATH:REF_PATH,
                            JOB_PATH:JOB_PATH])
}


