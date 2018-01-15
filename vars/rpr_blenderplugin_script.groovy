def executeGenTestRefCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat """
        echo 'sample image' > .\\ReferenceImages\\sample_image.txt
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

def executeTestCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        dir('temp/install_plugin')
        {
            unstash 'appWindows'

            bat """
            msiexec /i "RadeonProRenderForBlender.msi" /quiet /qn PIDKEY=GPUOpen2016 /log install_blender_plugin_${current_profile}.log /norestart
            """
        }

        dir('scripts')
        {
            bat """
            runFull.bat >> ../Test${current_profile}.log  2>&1
            """
        }
        dir("Results/Blender")
        {
            bat """
            copy session_report_embed_img.html session_report_${current_profile}.html
            rem copy session_report.html session_report_${current_profile}.html
            """

            bat """
            IF EXIST \"%CIS_TOOLS%\\sendFiles.bat\" (
                %CIS_TOOLS%\\sendFiles.bat session_report_${current_profile}.html ${UPLOAD_PATH}
                )
            """                        
            archiveArtifacts "session_report_${current_profile}.html"
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
    String PRJ_PATH="builds/sample-projects/MultiplatformSampleProject"
    String REF_PATH="${PRJ_PATH}/ReferenceImages/${asicName}-${osName}"
    String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}/${asicName}-${osName}".replace('%2F', '_')

    try {
        checkOutBranchOrScm(options['testBranch'], 'https://github.com/luxteam/jobs_test_blender.git')

        outputEnvironmentInfo(osName)
        
        if(options['updateRefs'])
        {
            executeGenTestRefCommand(osName)
            //sendFiles(osName, './ReferenceImages/*.*', REF_PATH)
        }
        else
        {
            receiveFiles(osName, "${REF_PATH}/*", './ReferenceImages/')
            executeTestCommand(osName)
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
                //sendFiles(osName, './ReferenceImages/*.*', REF_PATH)

            }
            else
            {
                //receiveFiles(osName, "${REF_PATH}/*", './ReferenceImages/')
            }
        }
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        sendFiles(osName, '*.log', "${PRJ_PATH}")
    }
}

def executeBuildWindows()
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
        build.cmd %CIS_TOOLS%\\castxml\\bin\\castxml.exe >> ../Build_${osName}.log  2>&1
        """
    }                              
    dir('RadeonProRenderPkgPlugin\\BlenderPkg')
    {
        bat """
        build_win_installer.cmd >> ../../Build_${osName}.log  2>&1
        """

        bat """
        IF EXIST \"%CIS_TOOLS%\\sendFiles.bat\" (
            %CIS_TOOLS%\\sendFiles.bat out/_pb/RadeonProRender*.msi ${UPLOAD_PATH}
            )
        """

        bat """
            c:\\JN\\create_refhtml.bat build.html "https://builds.rpr.cis.luxoft.com/${UPLOAD_PATH}"
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

def executeBuildOSX()
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
        ./build_osx.sh /usr/bin/castxml >> ../Build_${osName}.log  2>&1
        """
    }
    dir('RadeonProRenderPkgPlugin/BlenderPkg')
    {
        sh """
        ./build_osx_installer.sh >> ../../Build_${osName}.log  2>&1
        """

        dir('installer_build')
        {
            sh 'cp RadeonProRenderBlender*.dmg ../RadeonProRenderBlender.dmg'

            sh """
            ${CIS_TOOLS}/sendFiles.sh RadeonProRenderBlender*.dmg ${UPLOAD_PATH}
            """
        }
        /*
        stash includes: 'RadeonProRenderBlender.dmg', name: "app${osName}"
        */
    }
}

def executeBuildLinux()
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
        ./build.sh /usr/bin/castxml >> ../Build_${osName}.log  2>&1
        """
    }                              
    dir('RadeonProRenderPkgPlugin/BlenderPkg')
    {
        sh """
        ./build_linux_installer.sh >> ../../Build_${osName}.log  2>&1
        """

        dir('installer_build')
        {
            sh 'cp RadeonProRenderForBlender*.run ../RadeonProRenderForBlender.run'

            sh """
            /var/data/JN/cis_tools/sendFiles.sh RadeonProRenderForBlender*.run ${UPLOAD_PATH}
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
            checkOutBranchOrScm(projectBranch, 'https://github.com/Radeon-Pro/RadeonProRenderBlenderAddon.git')
        }
        dir('RadeonProRenderThirdPartyComponents')
        {
            checkOutBranchOrScm(thirdpartyBranch, 'https://github.com/Radeon-Pro/RadeonProRenderThirdPartyComponents.git')
        }
        dir('RadeonProRenderPkgPlugin')
        {
            checkOutBranchOrScm(packageBranch, 'https://github.com/Radeon-Pro/RadeonProRenderPkgPlugin.git')
        }
        outputEnvironmentInfo(osName)

        switch(osName)
        {
        case 'Windows': 
            executeBuildWindows(); 
            break;
        case 'OSX':
            executeBuildOSX();
            break;
        default: 
            executeBuildLinux();
        }
        
        //stash includes: 'Bin/**/*', name: "app${osName}"
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "${STAGE_NAME}.log"
        sendFiles(osName, '*.log', "${PRJ_PATH}")
        sendFiles(osName, '*.gtest.xml', "${PRJ_PATH}")
    }                        

}

def executeDeploy(Map options)
{
}

def call(String projectBranch = "", String thirdpartyBranch = "master", 
         String packageBranch = "master", String testsBranch = "master",
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;OSX:Intel_Iris;Ubuntu;AMD_RX460', 
         Boolean updateRefs = false, Boolean enableNotifications = true) {
    
    multiplatform_pipeline(platforms, this.&executeBuild, this.&executeTests, null, 
                           [projectBranch:projectBranch,
                           updateRefs:updateRefs, 
                           enableNotifications:enableNotifications])
}


