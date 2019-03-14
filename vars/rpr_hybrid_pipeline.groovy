def executeGenTestRefCommand(String osName, Map options)
{
    dir('BaikalNext/RprTest')
    {
        switch(osName)
        {
            case 'Windows':
                bat """
                ..\\bin\\RprTest -quality ${options.RENDER_QUALITY} -genref 1 --gtest_output=xml:../../${STAGE_NAME}.${options.RENDER_QUALITY}.gtest.xml >> ..\\..\\${STAGE_NAME}.${options.RENDER_QUALITY}.log 2>&1
                """
                break;
            case 'OSX':
                sh """
                export LD_LIBRARY_PATH=`pwd`/../bin/:\$LD_LIBRARY_PATH
                ../bin/RprTest -quality ${options.RENDER_QUALITY} -genref 1 --gtest_output=xml:../../${STAGE_NAME}.${options.RENDER_QUALITY}.gtest.xml >> ../../${STAGE_NAME}.${options.RENDER_QUALITY}.log 2>&1
                """
                break;
            default:
                sh """
                export LD_LIBRARY_PATH=`pwd`/../bin/:\$LD_LIBRARY_PATH
                ../bin/RprTest -quality ${options.RENDER_QUALITY} -genref 1 --gtest_output=xml:../../${STAGE_NAME}.${options.RENDER_QUALITY}.gtest.xml >> ../../${STAGE_NAME}.${options.RENDER_QUALITY}.log 2>&1
                """
        }
    }
}

def executeTestCommand(String osName, Map options)
{
    dir('BaikalNext/RprTest')
    {
        switch(osName)
        {
            case 'Windows':
                bat """
                ..\\bin\\RprTest -quality ${options.RENDER_QUALITY} --gtest_output=xml:../../${STAGE_NAME}.${options.RENDER_QUALITY}.gtest.xml >> ..\\..\\${STAGE_NAME}.${options.RENDER_QUALITY}.log 2>&1
                """
                break;
            case 'OSX':
                sh """
                export LD_LIBRARY_PATH=`pwd`/../Build/bin/:\$LD_LIBRARY_PATH
                RprTest.sh -quality ${options.RENDER_QUALITY} --gtest_output=xml:../../${STAGE_NAME}.${options.RENDER_QUALITY}.gtest.xml >> ../../${STAGE_NAME}.${options.RENDER_QUALITY}.log 2>&1
                """
                break;
            default:
                sh """
                export LD_LIBRARY_PATH=`pwd`/../Build/bin/:\${LD_LIBRARY_PATH}
                RprTest.sh -quality ${options.RENDER_QUALITY} --gtest_output=xml:../../${STAGE_NAME}.${options.RENDER_QUALITY}.gtest.xml >> ../../${STAGE_NAME}.${options.RENDER_QUALITY}.log 2>&1
                """
        }
    }
}

def executeTestsCustomQuality(String osName, String asicName, Map options)
{
    cleanWs()
    String REF_PATH_PROFILE="${options.REF_PATH}/${options.RENDER_QUALITY}/${asicName}-${osName}"
    String JOB_PATH_PROFILE="${options.JOB_PATH}/${options.RENDER_QUALITY}/${asicName}-${osName}"
    
    try {
        outputEnvironmentInfo(osName, "${STAGE_NAME}.${options.RENDER_QUALITY}")
        unstash "app${osName}"
        switch(osName)
        {
            case 'Windows':
                unzip dir: '.', glob: '', zipFile: 'BaikalNext_Build-Windows.zip'
                break
            default:
                sh "tar -xJf BaikalNext_Build*"
        }
            
        if(options['updateRefs']) {
            echo "Updating Reference Images"
            executeGenTestRefCommand(osName, options)
            sendFiles('./BaikalNext/RprTest/ReferenceImages/*.*', "${REF_PATH_PROFILE}")
        } else {
            echo "Execute Tests"
            receiveFiles("${REF_PATH_PROFILE}/*", './BaikalNext/RprTest/ReferenceImages/')
            executeTestCommand(osName, options)
        }
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        
        dir('BaikalNext/RprTest')
        {
            sendFiles('./ReferenceImages/*.*', "${JOB_PATH_PROFILE}/ReferenceImages")
            sendFiles('./OutputImages/*.*', "${JOB_PATH_PROFILE}/OutputImages")
        }
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        junit "*.gtest.xml"
    }
}


def executeTests(String osName, String asicName, Map options)
{
    def error_signal = false
    options['testsQuality'].split(",").each()
    {
        options['RENDER_QUALITY'] = "${it}"
        String error_message = ""
        try
        {
            executeTestsCustomQuality(osName, asicName, options)
        }
        catch(e)
        {
            println("Exception during [${options.RENDER_QUALITY}] quality tests execution")
            error_message = e.getMessage()
            error_signal = true
            currentBuild.result = "FAILED"
        }
        finally
        {
            if (env.CHANGE_ID)
            {
                String context = "[TEST] ${osName}-${asicName}-${it}"
                String description = error_message ? "Testing finished with error message: ${error_message}" : "Testing finished"
                String status = error_message ? "failure" : "success"
                pullRequest.createStatus(status, context, description, "${env.BUILD_URL}/artifact/${STAGE_NAME}.${options.RENDER_QUALITY}.log")
                options['commitContexts'].remove(context)
            }
        }
    }
    if (error_signal)
    {
        error "Error during tests execution"
    }
}


def executeBuildWindows(Map options)
{
    bat """
    mkdir Build
    cd Build
    cmake ${options['cmakeKeys']} -G "Visual Studio 15 2017 Win64" .. >> ..\\${STAGE_NAME}.log 2>&1
    cmake --build . --target PACKAGE --config Release >> ..\\${STAGE_NAME}.log 2>&1
    rename BaikalNext.zip BaikalNext_${STAGE_NAME}.zip
    """
}

def executeBuildOSX(Map options)
{
    sh """
    mkdir Build
    cd Build
    cmake ${options['cmakeKeys']} .. >> ../${STAGE_NAME}.log 2>&1
    make >> ../${STAGE_NAME}.log 2>&1
    make package >> ../${STAGE_NAME}.log 2>&1
    mv BaikalNext.tar.xz BaikalNext_${STAGE_NAME}.tar.xz
    """
}

def executeBuildLinux(Map options)
{
    sh """
    mkdir Build
    cd Build
    cmake ${options['cmakeKeys']} .. >> ../${STAGE_NAME}.log 2>&1
    make >> ../${STAGE_NAME}.log 2>&1
    make package >> ../${STAGE_NAME}.log 2>&1
    mv BaikalNext.tar.xz BaikalNext_${STAGE_NAME}.tar.xz
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

    // set pending status for all
    if(env.CHANGE_ID)
    {
        def commitContexts = []
        options['platforms'].split(';').each()
        { platform ->
            List tokens = platform.tokenize(':')
            String osName = tokens.get(0)
            // Statuses for builds
            String context = "[BUILD] ${osName}"
            commitContexts << context
            pullRequest.createStatus("pending", context, "Scheduled", "${env.JOB_URL}")
            if (tokens.size() > 1)
            {
                gpuNames = tokens.get(1)
                gpuNames.split(',').each()
                { gpuName ->
                    options['testsQuality'].split(",").each()
                    { testQuality ->
                        // Statuses for tests
                        context = "[TEST] ${osName}-${gpuName}-${testQuality}"
                        commitContexts << context
                        pullRequest.createStatus("pending", context, "Scheduled", "${env.JOB_URL}")
                    }
                }
            }
        }
        options['commitContexts'] = commitContexts
    }
}

def executeBuild(String osName, Map options)
{
    String error_message = ""
    try
    {
        checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])
        outputEnvironmentInfo(osName)

        if (env.CHANGE_ID)
        {
            pullRequest.createStatus("pending",
                "[BUILD] ${osName}", "Checkout has been finished. Trying to build...",
                "${env.BUILD_URL}/artifact/${STAGE_NAME}.log")
        }

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
        
        dir('Build')
        {
            stash includes: "BaikalNext_${STAGE_NAME}*", name: "app${osName}"
        }
    }
    catch (e)
    {
        println(e.getMessage())
        error_message = e.getMessage()
        currentBuild.result = "FAILED"
        throw e
    }
    finally
    {
        archiveArtifacts "${STAGE_NAME}.log"
        archiveArtifacts "Build/BaikalNext_${STAGE_NAME}*"
        if (env.CHANGE_ID)
        {
            String status = currentBuild.result ? "failure" : "success"
            String description = error_message ? "Build ${status}: '${error_message}'" : "Build finished as '${status}'"
            pullRequest.createStatus(status, "[BUILD] ${osName}", description, "${env.BUILD_URL}/artifact/${STAGE_NAME}.log")
            options['commitContexts'].remove("[BUILD] ${osName}")
        }
    }
}

def executeDeploy(Map options, List platformList, List testResultList)
{
    // TODO: build and publish html page with rendered images
    if (env.CHANGE_ID)
    {
        // if jobs was aborted or crushed remove pending status for unfinished stages
        options['commitContexts'].each()
        {
            pullRequest.createStatus("error", it, "Build has been terminated unexpectedly", "${env.BUILD_URL}")
        }
        // TODO: add tests summary results fom gtestmxml
        // TODO: when html report will be finished - add link to comment message
        String status = currentBuild.result ?: "success"
        def comment = pullRequest.comment("Jenkins build for ${pullRequest.head} finished as ${status}")
    }
}

def call(String projectBranch = "",
         //String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,NVIDIA_GF1080TI;Ubuntu18',
         String platforms = 'Windows;Ubuntu18;CentOS7',
         String testsQuality = "low,medium",
         String PRJ_ROOT='rpr-core',
         String PRJ_NAME='RadeonProRender-Hybrid',
         String projectRepo='https://github.com/Radeon-Pro/RPRHybrid.git',
         Boolean updateRefs = false,
         Boolean enableNotifications = true,
         String cmakeKeys = "-DCMAKE_BUILD_TYPE=Release -DBAIKAL_ENABLE_RPR=ON") {

    multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, null,
                           [platforms:platforms,
                            projectBranch:projectBranch,
                            updateRefs:updateRefs, 
                            testsQuality:testsQuality,
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
                            TEST_TIMEOUT:60,
                            cmakeKeys:cmakeKeys])
}
