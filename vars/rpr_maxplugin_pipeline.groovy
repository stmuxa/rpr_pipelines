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
            echo 'sample image' > ./ReferenceImages/sample_image.txt
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
                powershell"""
                \$uninstall = Get-WmiObject -Class Win32_Product -Filter "Name = 'Radeon ProRender for Autodesk 3ds Max®'"
                if (\$uninstall) {
                Write "Uninstalling..."
                \$uninstall = \$uninstall.IdentifyingNumber
                start-process "msiexec.exe" -arg "/X \$uninstall /qn /quiet /L+ie ${STAGE_NAME}.uninstall.log /norestart" -Wait
                }else{
                Write "Plugin not found"}
                """
            }
            catch(e)
            {
                echo "Error while deinstall plugin"
                echo e.toString()
                //throw e
            }
            finally
            {}
            
            dir('temp/install_plugin')
            {
                unstash 'appWindows'

                bat """
                msiexec /i "RadeonProRenderForMax.msi" /quiet /qn PIDKEY=${env.RPR_PLUGIN_KEY} /L+ie ../../${STAGE_NAME}.install.log /norestart
                """
            }
        }

        dir('scripts')
        {
            bat"""
            run.bat ${options.renderDevice} ${options.testsPackage} \"${options.tests}\">> ../${STAGE_NAME}.log  2>&1
            """
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
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
    }
}

def executeBuildWindows(Map options)
{
    String osName = 'Windows'
   
    dir('RadeonProRenderPkgPlugin\\MaxPkg2')
    {
        bat """
        build_windows_installer.cmd >> ../../${STAGE_NAME}.log  2>&1
        """
        
        String branch_postfix = ""
        if(env.BRANCH_NAME && BRANCH_NAME != "master")
        {
            branch_postfix = BRANCH_NAME.replace('/', '-')
        }
        if(env.Branch && Branch != "master")
        {
            branch_postfix = Branch.replace('/', '-')
        }
        if(branch_postfix)
        {
            bat """
            rename RadeonProRender*msi *.(${branch_postfix}).msi
            """
        }
        
        archiveArtifacts "RadeonProRender3dsMax*.msi"
        
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
    }                        
}

def executePreBuild(Map options)
{
    dir('RadeonProRenderMaxPlugin')
    {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderMaxPlugin.git')

        AUTHOR_NAME = bat (
                script: "git show -s --format=%%an HEAD ",
                returnStdout: true
                ).split('\r\n')[2].trim()

        echo "The last commit was written by ${AUTHOR_NAME}."
        options.AUTHOR_NAME = AUTHOR_NAME
        
        commitMessage = bat ( script: "git log --format=%%B -n 1", returnStdout: true )
        echo "Commit message: ${commitMessage}"
        options.commitMessage = commitMessage.split('\r\n')[2].trim()
        options['commitSHA'] = bat(script: "git log --format=%%H -1 ", returnStdout: true).split('\r\n')[2].trim()
        options.branchName = bat(script: "git branch --contains", returnStdout: true).split('\r\n')[2].trim()
        
        if(options['incrementVersion'])
        {
            if("${BRANCH_NAME}" == "master" && "${AUTHOR_NAME}" != "radeonprorender")
            {
                options.testsPackage = "master"
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
                options.testsPackage = "smoke"
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
                    options.testsPackage = "PR"
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
            checkOutBranchOrScm(options['testsBranch'], 'https://github.com/luxteam/jobs_test_max.git')

            dir("summaryTestResults")
            {
                testResultList.each()
                {
                    try {
                        dir("$it".replace("testResult-", ""))
                        {
                            unstash "$it"
                        }
                    }catch(e) {
                        echo "Can't unstash ${it}"
                    }
                }
            }

            dir("jobs_launcher")
            {
                if(options.projectBranch != "") {
                    options.branchName = options.projectBranch
                } else {
                    options.branchName = env.BRANCH_NAME
                }
                if(options.incrementVersion) {
                    options.branchName = "master"
                }
                
                options.commitMessage = options.commitMessage.replace("'", "")
                bat """
                build_reports.bat ..\\summaryTestResults Max2017 ${options.commitSHA} ${options.branchName} \\"${options.commitMessage}\\"
                """
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
        throw e
    }
    finally
    {}   
}


def call(String projectBranch = "", String thirdpartyBranch = "master", 
         String packageBranch = "master", String testsBranch = "master",
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI', 
         Boolean updateRefs = false, Boolean enableNotifications = true,
         Boolean incrementVersion = true,
         Boolean skipBuild = false,
         String renderDevice = "2",
         String testsPackage = "",
         String tests = "",
         Boolean forceBuild = false) {

    String PRJ_NAME="RadeonProRenderMaxPlugin"
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
                            renderDevice:renderDevice,
                            testsPackage:testsPackage,
                            tests:tests.replace(',', ' '),
                            executeBuild:false,
                            executeTests:false,
                            forceBuild:forceBuild,
                            reportName:'Test_20Report'])
}
