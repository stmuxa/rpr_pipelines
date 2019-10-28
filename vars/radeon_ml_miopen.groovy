 def executeGenTestRefCommand(String osName, Map options)
{
}

def executeTestCommand(String osName, Map options)
{
    dir('build-direct/Release') {
        switch(osName) {
            case 'Windows':
                bat """
                tests-MIOpen.exe --gtest_output=xml:..\\..\\${STAGE_NAME}.gtest.xml >> ..\\..\\${STAGE_NAME}.log 2>&1
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
                export LD_LIBRARY_PATH=\$PWD:\$LD_LIBRARY_PATH
                ./tests-MIOpen --gtest_output=xml:../../${STAGE_NAME}.gtest.xml >> ../../${STAGE_NAME}.log 2>&1
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
    mkdir build-direct
    cd build-direct
    cmake -G "Visual Studio 15 2017 Win64" ${options['cmakeKeys']} .. >> ..\\${STAGE_NAME}.log 2>&1
    set msbuild=\"C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe\"
    %msbuild% RadeonML.sln -property:Configuration=Release >> ..\\${STAGE_NAME}.log 2>&1
    xcopy ..\\third_party\\miopen\\MIOpen.lib .\\Release\\MIOpen.lib*
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
    cp ../third_party/miopen/libMIOpen.so ./Release
    cp ../third_party/miopen/libMIOpen.so.1 ./Release
    """
}

def executePreBuild(Map options)
{
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
         String cmakeKeys = '-DRML_DIRECTML=OFF -DRML_MIOPEN=ON -DRML_TENSORFLOW_CPU=OFF -DRML_TENSORFLOW_CUDA=OFF -DMIOpen_INCLUDE_DIR=../third_party/miopen -DMIOpen_LIBRARY_DIR=../third_party/miopen') {

    multiplatform_pipeline(platforms, null, this.&executeBuild, this.&executeTests, null,
                           [platforms:platforms,
                            projectBranch:projectBranch,
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
