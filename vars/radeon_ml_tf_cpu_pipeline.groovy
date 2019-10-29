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
    cleanWs()

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
    dir('RadeonML') {
        bat """
        mkdir build
        cd build
        call "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\VC\\Auxiliary\\Build\\vcvarsall.bat" amd64 >> ..\\..\\${STAGE_NAME}.log 2>&1
        cmake -G "Visual Studio 15 2017 Win64" ${options['cmakeKeys']} -DRML_TENSORFLOW_DIR=${WORKSPACE}\\tensorflow_cc .. >> ..\\..\\${STAGE_NAME}.log 2>&1
        MSBuild.exe RadeonML.sln -property:Configuration=Release >> ..\\..\\${STAGE_NAME}.log 2>&1
        """
    }
}

def executeBuildOSX(Map options)
{
}

def executeBuildLinux(Map options)
{
    dir('RadeonML') {
        sh """
        mkdir build
        cd build
        cmake ${options['cmakeKeys']} -DRML_TENSORFLOW_DIR=${WORKSPACE}/tensorflow_cc .. >> ../../${STAGE_NAME}.log 2>&1
        make -j >> ../../${STAGE_NAME}.log 2>&1
        make
        """
    }
}

def executePreBuild(Map options)
{
}

def executeBuild(String osName, Map options)
{
    try
    {
        /*dir('tensorflow') {
            checkOutBranchOrScm(options['tfRepoVersion'], options['tfRepo'])
        }*/
        dir('tensorflow_cc') {
            receiveFiles("${options.PRJ_ROOT}/tensorflow_cc/", "./")
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

        stash includes: 'build/Release/**/*', name: "app${osName}"
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
        zip archive: true, dir: 'build/Release', glob: '', zipFile: "${osName}_Release.zip"
    }
}

def executeDeploy(Map options, List platformList, List testResultList)
{
}

// TODO: add tests
def call(String projectBranch = "",
         String platforms = 'Windows;Ubuntu18;CentOS7_6',
         String PRJ_ROOT='rpr-ml',
         String PRJ_NAME='TF-CPU',
         String projectRepo='https://github.com/Radeon-Pro/RadeonML.git',
         String tfRepo='https://github.com/tensorflow/tensorflow.git',
         String tfRepoVersion='v1.13.1',
         Boolean updateRefs = false,
         Boolean enableNotifications = false,
         String cmakeKeys = "-DRML_DIRECTML=OFF -DRML_MIOPEN=OFF -DRML_TENSORFLOW_CPU=ON -DRML_TENSORFLOW_CUDA=OFF"
         ) {

    multiplatform_pipeline(platforms, null, this.&executeBuild, this.&executeTests, null,
                           [platforms:platforms,
                            projectBranch:projectBranch,
                            tfRepo:tfRepo,
                            tfRepoVersion:tfRepoVersion,
                            updateRefs:updateRefs,
                            enableNotifications:enableNotifications,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            projectRepo:projectRepo,
                            BUILDER_TAG:'BuilderML',
                            executeBuild:true,
                            executeTests:true,
                            cmakeKeys:cmakeKeys])
}
