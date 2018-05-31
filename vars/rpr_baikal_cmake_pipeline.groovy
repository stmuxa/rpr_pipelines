def executeGenTestRefCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat """
        ..\\Build\\bin\\Release\\BaikalTest.exe -genref 1 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log 2>&1
        """
        break;
    case 'OSX':
        // export LD_LIBRARY_PATH=`pwd`/../Build/bin/:"\$LD_LIBRARY_PATH"
        sh """
           
            ../Build/bin/BaikalTest -genref 1 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
        """
        break;
    default:
        sh """
            export LD_LIBRARY_PATH=`pwd`/../Build/bin/:\${LD_LIBRARY_PATH}
            ../Build/bin/BaikalTest -genref 1 --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
        """
    }
}

def executeTestCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat """
            ..\\Build\\bin\\Release\\BaikalTest.exe --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log 2>&1
        """
        break;
    case 'OSX':
        sh """
            ../Build/bin/BaikalTest --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
        """
        break;
    default:
        sh """
            export LD_LIBRARY_PATH=`pwd`/../Build/bin/:\${LD_LIBRARY_PATH}
            ../Build/bin/BaikalTest --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log 2>&1
        """
    }
}

def executeTests(String osName, String asicName, Map options)
{
    //String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
    String REF_PATH_PROFILE="rpr-core/RadeonProRender-Baikal/ReferenceImages/${asicName}-${osName}"
    String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"

    try {
        checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])

        outputEnvironmentInfo(osName)
        unstash "app${osName}"
        
        dir('BaikalTest')
        {
            if(options['updateRefs'])
            {
                executeGenTestRefCommand(osName)
                sendFiles('./ReferenceImages/*.*', "${REF_PATH_PROFILE}")
            }
            else
            {
                receiveFiles("${REF_PATH_PROFILE}/*", './ReferenceImages/')
                executeTestCommand(osName)
            }
        }
        echo "Stashing test results to : ${options.testResultsName}"
        if(options['updateRefs'])
        {
            dir('BaikalTest/ReferenceImages')
            {
                stash includes: '**/*', name: "${options.testResultsName}"
            }
        }
        else
        {
            dir('BaikalTest/OutputImages')
            {
                stash includes: '**/*', name: "${options.testResultsName}"
            }
        }
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        //println(e.getStackTrace());    
        
        dir('BaikalTest')
        {
            sendFiles('./ReferenceImages/*.*', "${JOB_PATH_PROFILE}/ReferenceImages")
            sendFiles('./OutputImages/*.*', "${JOB_PATH_PROFILE}/OutputImages")
        }
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        junit "*.gtest.xml"
    }
}

def executeBuildWindows(Map options)
{
    bat """
    mkdir Build
    cd Build
    cmake ${options['cmakeKeys']} -G "Visual Studio 14 2015 Win64" .. >> ..\\${STAGE_NAME}.log 2>&1
    cmake --build . --config Release >> ..\\${STAGE_NAME}.log 2>&1
    """
}

def executeBuildOSX(Map options)
{
    sh """
    mkdir Build
    cd Build
    cmake ${options['cmakeKeys']} .. >> ../${STAGE_NAME}.log 2>&1
    make >> ../${STAGE_NAME}.log 2>&1
    """
}

def executeBuildLinux(Map options)
{
    sh """
    mkdir Build
    cd Build
    cmake ${options['cmakeKeys']} .. >> ../${STAGE_NAME}.log 2>&1
    make >> ../${STAGE_NAME}.log 2>&1
    """
}

def executePreBuild(Map options)
{
    checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])

    AUTHOR_NAME = bat (
            script: "git show -s --format=%%an HEAD ",
            returnStdout: true
            ).split('\r\n')[2].trim()

    echo "The last commit was written by ${AUTHOR_NAME}."
    options.AUTHOR_NAME = AUTHOR_NAME

    commitMessage = bat ( script: "git log --format=%%B -n 1", returnStdout: true ).split('\r\n')[2].trim()
    echo "Commit message: ${commitMessage}"
    options.commitMessage = commitMessage
}

def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])
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
        
        stash includes: 'Build/bin/**/*', name: "app${osName}"
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "${STAGE_NAME}.log"
    }                        

}

def executeDeploy(Map options, List platformList, List testResultList)
{
    try
    {
        if(testResultList)
        {
            /*bat """
            rmdir /S /Q summaryTestResults
            """*/
            
            dir("summaryTestResults")
            {
                testResultList.each()
                {
                    try {
                        dir("$it".replace("testResult-", "")) {
                            unstash "$it"
                        }
                    }
                    catch(e) {
                        echo "Error while unstash ${it}"
                    }
                }
            }

            dir("summaryTestResults")
            {
                bat """
                C:\\Python35\\python.exe %CIS_TOOLS%\\baikal_html\\main.py --input_path %CD%
                """
            }
            
            if(options['updateRefs'])
            {
                String REF_PATH_PROFILE="rpr-core/RadeonProRender-Baikal/ReferenceImages"
                sendFiles('./summaryTestResults/compare.html', "${REF_PATH_PROFILE}")
            }

            publishHTML([allowMissing: false, 
                         alwaysLinkToLastBuild: false, 
                         keepAll: true, 
                         reportDir: 'summaryTestResults', 
                         reportFiles: 'compare.html', reportName: 'Test Report', reportTitles: 'Summary Report'])
        }
    }
    catch (e) {
        currentBuild.result = "FAILED"        
        println(e.toString());
        println(e.getMessage());
        throw e
    }
    finally {

    }  
}

def call(String projectBranch = "", 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;OSX:Intel_Iris;Ubuntu:AMD_WX7100;CentOS7', 
         String PRJ_ROOT='rpr-core',
         String PRJ_NAME='RadeonProRender-Baikal',
         String projectRepo='https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRender-Baikal.git',
         Boolean updateRefs = false, 
         Boolean enableNotifications = true,
         String cmakeKeys = "-DCMAKE_BUILD_TYPE=Release -DBAIKAL_ENABLE_RPR=ON") {

    multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, this.&executeDeploy,
                           [projectBranch:projectBranch,
                            updateRefs:updateRefs, 
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            projectRepo:projectRepo,
                            BUILDER_TAG:'BuilderS',
                            executeBuild:true,
                            executeTests:true,
                            slackChannel:"${SLACK_BAIKAL_CHANNEL}",
                            slackBaseUrl:"${SLACK_BAIKAL_BASE_URL}",
                            slackTocken:"${SLACK_BAIKAL_TOCKEN}",
                            cmakeKeys:cmakeKeys])
}
