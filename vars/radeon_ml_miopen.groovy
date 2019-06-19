 def executeGenTestRefCommand(String osName, Map options)
{
}

def executeTestCommand(String osName, Map options)
{
}


def executeTests(String osName, String asicName, Map options)
{
    try {
        String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
        String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"

        outputEnvironmentInfo(osName)
        unstash "app${osName}"

        if(options['updateRefs']) {
            executeGenTestRefCommand(osName, options)
            sendFiles('./Work/Baseline/', REF_PATH_PROFILE)
        }
        else {
            try {
                receiveFiles("${REF_PATH_PROFILE}", './Work/Baseline/')
            } catch (e) {println("Baseline doesn't exist.")}

            executeTestCommand(osName, options)
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
    bat """
    mkdir build-direct
    cd build-direct
    cmake -G "Visual Studio 15 2017 Win64" ${options['cmakeKeys']} .. >> ..\\${STAGE_NAME}.log 2>&1
    set msbuild=\"C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe\"
    %msbuild% RadeonML-MIOpen.sln -property:Configuration=Release >> ..\\${STAGE_NAME}.log 2>&1
    xcopy ..\\third_party\\miopen\\MIOpen.dll .\\Release\\MIOpen.dll*
    """
}

def executeBuildOSX(Map options)
{
}

def executeBuildLinux(Map options)
{
    sh """
    mkdir build-direct
    cd build-direct
    cmake ${options['cmakeKeys']} .. >> ../${STAGE_NAME}.log 2>&1
    make -j >> ../${STAGE_NAME}.log 2>&1
    mv bin Release
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

    /*if(commitMessage.contains("[CIS:GENREF]") && env.BRANCH_NAME && env.BRANCH_NAME == "master") {
        options.updateRefs = true
    }*/
}

def executeBuild(String osName, Map options)
{
    try
    {
        checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])
        receiveFiles("bin_storage/MIOpen/*", './third_party/miopen')
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

        stash includes: 'build-direct/Release/**/*', name: "app${osName}"
    }
    catch (e)
    {
        println(e.getMessage())
        currentBuild.result = "FAILED"
        throw e
    }
    finally
    {
        archiveArtifacts "${STAGE_NAME}.log"
        zip archive: true, dir: 'build-direct/Release', glob: '', zipFile: "${osName}Release.zip"
    }
}

def executeDeploy(Map options, List platformList, List testResultList)
{
}

def call(String projectBranch = "",
         String platforms = 'Windows;Ubuntu18;CentOS7_6',
         String PRJ_ROOT='rpr-ml',
         String PRJ_NAME='MIOpen',
         String projectRepo='https://github.com/Radeon-Pro/RadeonML.git',
         Boolean updateRefs = false,
         Boolean enableNotifications = false,
         String cmakeKeys = '-DRML_BACKEND=MIOpen -DRML_LOG_LEVEL=Error -DMIOpen_INCLUDE_DIR=../third_party/miopen -DMIOpen_LIBRARY_DIR=../third_party/miopen') {

    multiplatform_pipeline(platforms, null, this.&executeBuild, this.&executeTests, this.&executeDeploy,
                           [platforms:platforms,
                            projectBranch:projectBranch,
                            updateRefs:updateRefs,
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            projectRepo:projectRepo,
                            BUILDER_TAG:'BuilderML',
                            executeBuild:true,
                            executeTests:false,
                            cmakeKeys:cmakeKeys])
}
