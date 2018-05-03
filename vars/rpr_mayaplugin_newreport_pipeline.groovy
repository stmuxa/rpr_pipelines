
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
            ./make_results_baseline.sh
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
        if(!options['skipBuild'])
        {
            try
            {
                powershell'''
                (Get-WmiObject -Class Win32_Product -Filter "Name = 'Radeon ProRender for Autodesk Maya®'").Uninstall()
                '''
            }
            catch(e)
            {
                echo "Error while deinstall plugin"
                echo e.toString()
                echo e.getMessage()
            }
            finally
            {}

            dir('temp/install_plugin')
            {
                unstash 'appWindows'

                bat """
                msiexec /i "RadeonProRenderForMaya.msi" /quiet /qn PIDKEY=GPUOpen2016 /L+ie ../../${STAGE_NAME}.install.log /norestart
                """
            }
        }
        
        dir('scripts')
        {
            //bat"""
            //auto_config.bat >> ../${STAGE_NAME}.log 2>&1
            //"""
            bat """
            run.bat ${options.executionParameters} >> ../${STAGE_NAME}.log  2>&1
            """
        }

        /*dir("Work/Results/Maya")
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
        }*/

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
        break;
    }
}

def executeTests(String osName, String asicName, Map options)
{
    try {
        timeout(time: 240, unit: 'MINUTES')
        {
            checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_maya.git')

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
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
    }
}

def executeBuildWindows(Map options)
{
    dir('RadeonProRenderPkgPlugin\\MayaPkg')
    {
        bat """
        build_windows_installer.cmd >> ../../${STAGE_NAME}.log  2>&1
        """
        
        String branch_postfix = ""
        if(env.BRANCH_NAME && BRANCH_NAME != "master")
        {
            branch_postfix = BRANCH_NAME.replace('/', '-')
            echo "Detected as autobuild, postfix: ${branch_postfix}"
        }
        if(env.Branch && Branch != "master")
        {
            branch_postfix = Branch.replace('/', '-')
            echo "Detected as manualbuild, postfix: ${branch_postfix}"
        }
        if(branch_postfix)
        {
            bat """
            rename RadeonProRender*msi *.(${branch_postfix}).msi
            """
            echo "Rename build"
        }
        
        archiveArtifacts "RadeonProRender*.msi"
        
        bat """
          for /r %%i in (RadeonProRender*.msi) do copy %%i RadeonProRenderForMaya.msi
        """
        
        stash includes: 'RadeonProRenderForMaya.msi', name: 'appWindows'
    }
}

def executeBuildOSX(Map options)
{
    dir('RadeonProRenderMayaPlugin/ThirdParty')
    {
        sh """
        chmod +x unix_update.sh
        ./unix_update.sh >> ../../${STAGE_NAME}.log 2>&1
        """
    }
    dir('RadeonProRenderPkgPlugin/MayaPkg')
    {
        sh """
        ./build_osx_installer.sh >> ../../${STAGE_NAME}.log 2>&1
        """
        
        dir('.installer_build')
        {
            String branch_postfix = ""
            if(env.BRANCH_NAME && BRANCH_NAME != "master")
            {
                branch_postfix = BRANCH_NAME.replace('/', '-')
                echo "Detected as autobuild, postfix: ${branch_postfix}"
            }
            if(env.Branch && Branch != "master")
            {
                branch_postfix = Branch.replace('/', '-')
                echo "Detected as manualbuild, postfix: ${branch_postfix}"
            }
            if(branch_postfix)
            {
                sh"""
                for i in RadeonProRender*; do name="\${i%.*}"; mv "\$i" "\${name}.(${branch_postfix})\${i#\$name}"; done
                """
                echo "Rename build"
            }
            archiveArtifacts "RadeonProRender*.dmg"
            /*sh"""
            cp RadeonProRender*.dmg RadeonProRenderForMaya.dmg
            """
            */
            //stash includes: 'RadeonProRenderForMaya.dmg', name: "app${osName}"
        }
    }
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
        sendFiles('*.log', "${options.JOB_PATH}")
    }                        
}

def executePreBuild(Map options)
{
    dir('RadeonProRenderBlenderAddon')
    {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderMayaPlugin.git')

        AUTHOR_NAME = bat (
                script: "git show -s --format=%%an HEAD ",
                returnStdout: true
                ).split('\r\n')[2].trim()

        echo "The last commit was written by ${AUTHOR_NAME}."

        if(options['incrementVersion'])
        {
            if("${BRANCH_NAME}" == "master" && "${AUTHOR_NAME}" != "radeonprorender")
            {
                echo "Incrementing version of change made by ${AUTHOR_NAME}."

                String currentversion=version_read('src/rprblender/__init__.py', '"version": (', ', ')
                echo "currentversion ${currentversion}"

                new_version=version_inc(currentversion, 3, ', ')
                echo "new_version ${new_version}"

                version_write('src/rprblender/__init__.py', '"version": (', new_version, ', ')

                String updatedversion=version_read('src/rprblender/__init__.py', '"version": (', ', ', "true")
                echo "updatedversion ${updatedversion}"                    
                
                bat """
                    git add src/rprblender/__init__.py
                    git commit -m "buildmaster: version update to ${updatedversion}"
                    git push origin HEAD:master
                   """ 
                
                //get commit's sha which have to be build
                options['projectBranch'] = bat ( script: "git log --format=%%H -1 ",
                                    returnStdout: true
                                    ).split('\r\n')[2].trim()

                options['executeBuild'] = true
                options['executeTests'] = true
            }
            else
            {
                commitMessage = bat ( script: "git log --format=%%B -n 1", returnStdout: true )
                echo "Commit message: ${commitMessage}"
                
                if(commitMessage.contains("CIS:BUILD"))
                {
                    options['executeBuild'] = true
                }

                if(commitMessage.contains("CIS:TESTS"))
                {
                    options['executeBuild'] = true
                    options['executeTests'] = true
                }

                if (env.CHANGE_URL)
                {
                    echo "branch was detected as Pull Request"
                    options['executeBuild'] = true
                    options['executeTests'] = true
                }
            }
        }
    }
    if(options['forceBuild'])
    {
        options['executeBuild'] = true
        options['executeTests'] = true
    }
}

def executeDeploy(Map options, List platformList, List testResultList)
{
    try { 
        if(options['executeTests'] && testResultList)
        {
            checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_maya.git')

            dir("summaryTestResults")
            {
                testResultList.each()
                {
                    dir("$it".replace("testResult-", ""))
                    {
                        unstash "$it"
                    }
                }
            }

            dir("jobs_launcher")
            {
                bat """
                build_reports.bat ..\\summaryTestResults                
                """
            }   

            /*dir("summaryTestResults")
            {
                archiveArtifacts "summary_report_embed_img.html"
                archiveArtifacts "performance_report.html"
            }*/
            if(options['updateRefs'])
            {
                String REF_PATH_PROFILE="rpr-plugins/RadeonProRenderMayaPlugin-NewReport/ReferenceImages"
                //sendFiles('./summaryTestResults/summary_report.html', "${REF_PATH_PROFILE}")
            }
            publishHTML([allowMissing: false, 
                         alwaysLinkToLastBuild: false, 
                         keepAll: true, 
                         reportDir: 'summaryTestResults', 
                         reportFiles: 'summary_report.html, performance_report.html, compare_report.html',
                         reportName: 'Test Report',
                         reportTitles: 'Summary Report, Performance Report, Compare Report'])
        }
    }
    catch (e) {
        currentBuild.result = "FAILED"
        println(e.toString());
        println(e.getMessage());
        throw e
    }
    finally
    {
    }   
}

def call(String projectBranch = "", String thirdpartyBranch = "master", 
         String packageBranch = "master", String testsBranch = "master",
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;', 
         //String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;OSX;Ubuntu:AMD_WX7100', 
         //String platforms = 'Windows;OSX;Ubuntu', 
         Boolean updateRefs = false, Boolean enableNotifications = true,
         Boolean incrementVersion = true,
         Boolean skipBuild = false,
         String executionParameters = "",
         Boolean forceBuild = false) {

    try
    {
        properties([[$class: 'BuildDiscarderProperty', 
                     strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '',
                                artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);

        
        String PRJ_NAME="RadeonProRenderMayaPlugin-NewReport"
        String PRJ_ROOT="rpr-plugins"

        multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, this.&executeDeploy, 
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
                                executionParameters:executionParameters,
                                forceBuild:forceBuild])
    }
    catch (e) {
        currentBuild.result = "INIT FAILED"      
        println(e.toString());
        println(e.getMessage());
        throw e
    }
    finally {
        node('master')
        {
            sh'''
            pwd
            '''
            step([$class: 'LogParserPublisher',
                  parsingRulesPath: '/var/jenkins_home/log_parsing_rules',
                  useProjectRule: false])    
        }
    }
}