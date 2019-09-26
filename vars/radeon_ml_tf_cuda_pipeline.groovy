 def executeGenTestRefCommand(String osName, Map options)
{
}

def executeTestCommand(String osName, Map options)
{
    dir('build-direct/Release') {
        switch(osName) {
            case 'Windows':
                bat """
                tests.exe --gtest_output=xml:..\\..\\${STAGE_NAME}.gtest.xml >> ..\\..\\${STAGE_NAME}.log 2>&1
                """
                break;
            case 'OSX':
                sh """
                echo "skip"
                """
                break;
            default:
                sh """
                chmod +x tests
                tests --gtest_output=xml:../../${STAGE_NAME}.gtest.xml >> ../../${STAGE_NAME}.log 2>&1
                """
        }
    }
}


def executeTests(String osName, String asicName, Map options)
{
    try {
        String REF_PATH_PROFILE="${options.REF_PATH}/${asicName}-${osName}"
        String JOB_PATH_PROFILE="${options.JOB_PATH}/${asicName}-${osName}"

        outputEnvironmentInfo(osName)
        unstash "app${osName}"

        executeTestCommand(osName, options)
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        junit "*gtest.xml"
    }
}


def executeBuildWindows(Map options)
{
    bat """
    cd tensorflow
    python3 configure.py
    mklink RadeonML ..\\RadeonML
    bazel build --config=opt --config=monolithic //RadeonML:RadeonML-TF.dll
    """
}

def executeBuildOSX(Map options)
{
}

def executeBuildLinux(Map options)
{
    sh """
    cd tensorflow
    python3 configure.py
    ln -s ../RadeonML RadeonML
    bazel build --config=opt --config=monolithic //RadeonML:test_app
    """
}

def executePreBuild(Map options)
{
}

def executeBuild(String osName, Map options)
{
    try
    {
        dir('tensorflow') {
            checkOutBranchOrScm(options['tfRepoVersion'], options['tfRepo'])
        }
        dir('RadeonML') {
            checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])
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

        // TODO: save build artifacts
        // bazel-bin/RadeonML/RadeonML-TF.dll
        // bazel-bin/RadeonML/RadeonML-TF.lib
        // bazel-bin/RadeonML/libRadeonML-TF.so
        // bazel-bin/RadeonML/test_app
        // stash includes: 'build-direct/Release/**/*', name: "app${osName}"
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
        // zip archive: true, dir: 'build-direct/Release', glob: '', zipFile: "${osName}Release.zip"
    }
}

def executeDeploy(Map options, List platformList, List testResultList)
{
}

def call(String projectBranch = "",
         String platforms = 'Windows;Ubuntu18',
         String PRJ_ROOT='rpr-ml',
         String PRJ_NAME='TF_CUDA',
         String projectRepo='https://github.com/Radeon-Pro/RadeonML.git',
         String tfRepo='https://github.com/tensorflow/tensorflow.git',
         String tfRepoVersion='v1.13.1',
         Boolean updateRefs = false,
         Boolean enableNotifications = false,
         ) {

    multiplatform_pipeline(platforms, null, this.&executeBuild, this.&executeTests, this.&executeDeploy,
                           [platforms:platforms,
                            projectBranch:projectBranch,
                            updateRefs:updateRefs,
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            projectRepo:projectRepo,
                            BUILDER_TAG:'BuilderML && CUDA',
                            executeBuild:true,
                            executeTests:true,
                            cmakeKeys:cmakeKeys])
}
