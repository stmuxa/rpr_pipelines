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
        outputEnvironmentInfo(osName)
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
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        junit "*.gtest.xml"
    }
}


def executeTests(String osName, String asicName, Map options)
{
    options['testsQuality'].split(",").each() {
        options['RENDER_QUALITY'] = "${it}"
        executeTestsCustomQuality(osName, asicName, options)
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
        
        dir('Build')
        {
            stash includes: "BaikalNext_${STAGE_NAME}*", name: "app${osName}"
        }
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "${STAGE_NAME}.log"
        archiveArtifacts "Build/BaikalNext_${STAGE_NAME}*"
    }                        

}

def executeDeploy(Map options, List platformList, List testResultList)
{
    cleanWs()
}

def call(String projectBranch = "",
         String platforms = 'Windows;Ubuntu18',
         String testsQuality = "",
         String PRJ_ROOT='rpr-core',
         String PRJ_NAME='RadeonProRender-Hybrid',
         String projectRepo='https://github.com/Radeon-Pro/RPRHybrid.git',
         Boolean updateRefs = false,
         Boolean enableNotifications = true,
         String cmakeKeys = "-DCMAKE_BUILD_TYPE=Release -DBAIKAL_ENABLE_RPR=ON") {

    multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, null,
                           [projectBranch:projectBranch,
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
                            cmakeKeys:cmakeKeys])
}
