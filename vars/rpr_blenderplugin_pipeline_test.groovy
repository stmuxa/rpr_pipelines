
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
            break;
        case 'OSX':
            sh """
            echo 'sample image' > ./ReferenceImages/sample_image.txt
            """
            break;
        default:
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
        
        if (options['withBuildStage'])
        {
            try
            {
                /*powershell'''
                (Get-WmiObject -Class Win32_Product -Filter "Name = 'Radeon ProRender for Blender'").Uninstall()
                '''*/
                
                powershell"""
                $uninstall32 = gci "HKLM:\\SOFTWARE\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall" | foreach { gp $_.PSPath } | ? { $_ -match "Radeon ProRender for Blender" } | select UninstallString
                $uninstall64 = gci "HKLM:\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall" | foreach { gp $_.PSPath } | ? { $_ -match "Radeon ProRender for Blender" } | select UninstallString

                if ($uninstall64) {
                $uninstall64 = $uninstall64.UninstallString -Replace "msiexec.exe","" -Replace "/I","" -Replace "/X",""
                $uninstall64 = $uninstall64.Trim()
                Write "Uninstalling..."
                start-process "msiexec.exe" -arg "/X $uninstall64 /qn /quiet /L+ie ../../${STAGE_NAME}.uninstall.log /norestart" -Wait}
                if ($uninstall32) {
                $uninstall32 = $uninstall32.UninstallString -Replace "msiexec.exe","" -Replace "/I","" -Replace "/X",""
                $uninstall32 = $uninstall32.Trim()
                Write "Uninstalling..."
                start-process "msiexec.exe" -arg "/X $uninstall32 /qn /quiet /L+ie ../../${STAGE_NAME}.uninstall.log /norestart" -Wait}
                """
            }
            catch(e)
            {
                echo "Error while deinstall plugin"
                echo e.toString()
                echo e.getMessage()
                echo e.getStackTrace()
                //throw e
            }
            finally
            {

            }
            
            dir('temp/install_plugin')
            {
                unstash 'appWindows'

                bat """
                msiexec /i "RadeonProRenderForBlender.msi" /quiet /qn PIDKEY=GPUOpen2016 /L+ie ../../${STAGE_NAME}.install.log /norestart
                """
            }
        }

        dir('scripts')
        {
            bat """
            run.bat ${options.executionParameters} >> ../${STAGE_NAME}.log  2>&1
            """
        }

        dir("Work/Results/Blender")
        {
            bat """
            copy session_report_embed_img.html session_report_${STAGE_NAME}.html
            """

            //sendFiles("session_report_${STAGE_NAME}.html", "${options.JOB_PATH}")
         
            archiveArtifacts "session_report_${STAGE_NAME}.html"
        }

        break;
    case 'OSX':
        sh """
        echo 'sample image' > ./OutputImages/sample_image.txt
        """
        break;
    default:
        /*
        if (options['withBuildStage']){
            dir('temp/install_plugin')
            {
                unstash 'appLinux'
        TODO: add log file and silent install
        
                sh """
                chmod +x RadeonProRenderForBlender.run
                ./RadeonProRenderForBlender.run ~/Desktop/blender-2.79-linux-glibc219-x86_64/
                """
            }
        }
        */
        
        dir("scripts")
        {
            //TODO: fix parameters
            //echo "./run.sh ${options.runParameters}>> ../${STAGE_NAME}.log 2>&1"
            
            sh """
            ./run.sh ${options.runParameters}>> ../${STAGE_NAME}.log 2>&1
            """
        }
        dir("Work/Results/Blender")
        {
            sh """
            cp session_report_embed_img.html session_report_${STAGE_NAME}.html
            """
            
            //sendFiles("session_report_${STAGE_NAME}.html", "${options.JOB_PATH}")
            
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
            sendFiles('./Work/Baseline/', REF_PATH_PROFILE)
        }
        else
        {            
            receiveFiles("${REF_PATH_PROFILE}/*", './Work/Baseline/')
            executeTestCommand(osName, options)
        }

        echo "Stashing test results to : ${options.testResultsName}"
        
        dir('Work/Results/Blender')
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
    /*dir('RadeonProRenderBlenderAddon')
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
    }
    
    dir('RadeonProRenderBlenderAddon')
    {
        bat """
        build.cmd %CIS_TOOLS%\\castxml\\bin\\castxml.exe >> ../${STAGE_NAME}.log  2>&1
        """
    }*/
    dir('RadeonProRenderPkgPlugin\\BlenderPkg')
    {
        bat """
        build_win_installer.cmd >> ../../${STAGE_NAME}.log  2>&1
        """
        
        archiveArtifacts "RadeonProRender*.msi"
        //sendFiles('RadeonProRenderForBlender*.msi', "${options.JOB_PATH}")

        bat '''
        for /r %%i in (RadeonProRender*.msi) do copy %%i RadeonProRenderForBlender.msi
        '''
        
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

        }
        //stash includes: 'RadeonProRenderBlender.dmg', name: "app${osName}"
        archiveArtifacts "installer_build/RadeonProRender*.dmg"
        //sendFiles('installer_build/RadeonProRender*.dmg', "${options.JOB_PATH}")
    }
}

def executeBuildLinux(Map options, String osName)
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

        dir('.installer_build')
        {
            stash includes: 'RadeonProRender*.run', name: "app${osName}"
            sh 'cp RadeonProRender*.run ../RadeonProRenderForBlender.run'
            //sendFiles("RadeonProRender*.run", "${options.JOB_PATH}")
        }
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
            executeBuildLinux(options, osName);
        }
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

def executeDeploy(Map options, List platformList, List testResultList)
{
    try { 
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
            sendFiles('./summary_report_embed_img.html', "${options.JOB_PATH}")
            archiveArtifacts "summary_report_embed_img.html"
        }
        publishHTML([allowMissing: false, 
                     alwaysLinkToLastBuild: false, 
                     keepAll: true, 
                     reportDir: 'summaryTestResults', 
                     reportFiles: 'summary_report.html', reportName: 'Test Report', reportTitles: 'Summary Report'])


        if(options['incrementVersion']){
            echo "currentBuild.result : ${currentBuild.result}"
            if("${BRANCH_NAME}"=="master" && currentBuild.result != "FAILED")
            {
                dir('RadeonProRenderBlenderAddon')
                {
                    checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderMaxPlugin.git')

                    AUTHOR_NAME = bat (
                            script: "git show -s --format='%%an' HEAD ",
                            returnStdout: true
                            ).split('\r\n')[2].trim()

                    echo "The last commit was written by ${AUTHOR_NAME}."

                    if (AUTHOR_NAME != "'radeonprorender'") {
                        echo "Incrementing version of change made by ${AUTHOR_NAME}."


                        String currentversion=version_read('src/rprblender/__init__.py', '"version": (', ', ')
                        echo "currentversion ${currentversion}"

                        new_version=version_inc(currentversion, 3, ', ')
                        echo "new_version ${new_version}"

                        version_write('src/rprblender/__init__.py', '"version": (', new_version, ', ')

                        String updatedversion=version_read('src/rprblender/__init__.py', '"version": (', ', ', true)
                        echo "updatedversion ${updatedversion}"                    
                        /*
                        bat """
                            git add src/rprblender/__init__.py
                            git commit -m "buildmaster: version update to ${updatedversion}"
                            git push origin HEAD:master
                           """  */
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
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;Ubuntu:AMD_WX7100', 
         //String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;OSX;Ubuntu:AMD_WX7100', 
         //String platforms = 'Windows;OSX;Ubuntu', 
         Boolean updateRefs = false, Boolean enableNotifications = true,
         Boolean incrementVersion = true,
         Boolean skipBuild = false,
         String executionParameters = " ") {

    try
    {
        properties([[$class: 'BuildDiscarderProperty', 
                     strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '',
                                artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);

        
        String PRJ_NAME="RadeonProRenderBlenderPlugin"
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
    catch (e) {
        currentBuild.result = "INIT FAILED"
        
        println(e.toString());
        println(e.getMessage());
        println(e.getStackTrace());
        
        throw e
    }
    finally
    {
        node
        {
            //step([$class: 'LogParserPublisher', parsingRulesPath: '/var/lib/jenkins/jenkins-rule-logparser', useProjectRule: false])
            step([$class: 'LogParserPublisher', parsingRulesPath: ' /tmp/log_parsing_rules', useProjectRule: false])
        }
    }
}
