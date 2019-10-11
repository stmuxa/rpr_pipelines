def executeTestCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        try
        {
            dir("tools/jenkins")
            {
                bat "pretest.bat >> ..\\..\\${STAGE_NAME}.log 2>&1"
            }
        }catch(e){}
        dir("unittest")
        {
            bat "mkdir testSave"
            bat "..\\bin\\release\\x64\\UnitTest64.exe  --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log  2>&1"
        }
        break;
    case 'OSX':
        try
        {
            dir("tools/jenkins")
            {
                sh """chmod +x pretest.sh
                    ./pretest.sh >> ..\\..\\${STAGE_NAME}.log 2>&1"""
            }
        }catch(e){}
        dir("unittest")
        {
            sh "mkdir testSave"
            sh "../bin/release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1"
        }
        break;
    default:
        try
        {
            dir("tools/jenkins")
            {
                sh """chmod +x pretest.sh
                    ./pretest.sh >> ..\\..\\${STAGE_NAME}.log 2>&1"""
            }
        }catch(e){}
        dir("unittest")
        {
            sh "mkdir testSave"
            sh "../bin/release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1"
        }
    }
}

def executeTests(String osName, String asicName, Map options)
{
    try
    {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')

        outputEnvironmentInfo(osName)
        unstash "app${osName}"

        executeTestCommand(osName)
    }
    catch (e)
    {
        println(e.toString());
        println(e.getMessage());
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
        junit "*.gtest.xml"
    }
}

def executeBuildWindows(String cmakeKeys)
{
    bat """
    set msbuild=\"C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe\"
    set target=build
    set maxcpucount=/maxcpucount
    set PATH=C:\\Python27\\;%PATH%
    .\\tools\\premake\\win\\premake5 --embed_kernels vs2017 --generate_build_info ${cmakeKeys} >> ${STAGE_NAME}.log 2>&1
    set solution=.\\RadeonImageFilters.sln
    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Release;Platform=x64 %parameters% %solution% >> ${STAGE_NAME}.log 2>&1
    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Debug;Platform=x64 %parameters% %solution% >> ${STAGE_NAME}.log 2>&1
    """

    bat """
    mkdir RadeonProImageProcessing_Debug
    xcopy models RadeonProImageProcessing_Debug\\models /s/y/i
    xcopy include RadeonProImageProcessing_Debug\\include /y/i
    xcopy README.md RadeonProImageProcessing_Debug\\README.md*

    xcopy RadeonProImageProcessing_Debug RadeonProImageProcessing_Release /s/y/i

    xcopy bin\\debug\\x64 RadeonProImageProcessing_Debug\\bin /s/y/i
    xcopy bin\\release\\x64 RadeonProImageProcessing_Release\\bin /s/y/i

    cd RadeonProImageProcessing_Release
    del /S Gtest64*
    del /S UnitTest64*

    cd ..\\RadeonProImageProcessing_Debug
    del /S Gtest64*
    del /S UnitTest64*

    cd ..
    mkdir RIF_Debug
    mkdir RIF_Release

    move RadeonProImageProcessing_Debug RIF_Debug
    move RadeonProImageProcessing_Release RIF_Release
    """

    zip archive: true, dir: 'RIF_Debug', glob: '', zipFile: 'RadeonProImageProcessing_Windows_Debug.zip'
    zip archive: true, dir: 'RIF_Release', glob: '', zipFile: 'RadeonProImageProcessing_Windows_Release.zip'

    rtp nullAction: '1', parserName: 'HTML', stableText: """<h3><a href="${BUILD_URL}/artifact/RadeonProImageProcessing_Windows_Release.zip">RadeonProImageProcessing_Windows_Release.zip</a></h3>"""
    rtp nullAction: '1', parserName: 'HTML', stableText: """<h3><a href="${BUILD_URL}/artifact/RadeonProImageProcessing_Windows_Debug.zip">RadeonProImageProcessing_Windows_Debug.zip</a></h3>"""
}

def executeBuildOSX(String cmakeKeys)
{
    sh """
    export PATH="/usr/local/opt/llvm/bin:$PATH"
    export LDFLAGS="-L/usr/local/opt/llvm/lib"
    export CPPFLAGS="-I/usr/local/opt/llvm/include"

    export CC=/usr/local/Cellar/llvm/8.0.0_1/bin/clang
    export CXX=/usr/local/Cellar/llvm/8.0.0_1/bin/clang++
    export CPP=/usr/local/Cellar/llvm/8.0.0_1/bin/clang-cpp
    export LD=/usr/local/Cellar/llvm/8.0.0_1/bin/lld

    alias c++=/usr/local/Cellar/llvm/8.0.0_1/bin/clang++
    alias g++=/usr/local/Cellar/llvm/8.0.0_1/bin/clang++
    alias gcc=/usr/local/Cellar/llvm/8.0.0_1/bin/clang
    alias cpp=/usr/local/Cellar/llvm/8.0.0_1/bin/clang-cpp
    alias ld=/usr/local/Cellar/llvm/8.0.0_1/bin/lld
    alias cc=/usr/local/Cellar/llvm/8.0.0_1/bin/llc

    tools/premake/osx/premake5 --embed_kernels gmake --generate_build_info ${cmakeKeys} >> ${STAGE_NAME}.log 2>&1
    make config=release_x64                                         >> ${STAGE_NAME}.log 2>&1
    make config=debug_x64                                           >> ${STAGE_NAME}.log 2>&1
    """

    sh """
    mkdir RadeonProImageProcessing_Debug
    cp -R models RadeonProImageProcessing_Debug
    cp -R include RadeonProImageProcessing_Debug
    cp README.md RadeonProImageProcessing_Debug

    cp -R RadeonProImageProcessing_Debug RadeonProImageProcessing_Release

    mkdir RadeonProImageProcessing_Debug/bin
    cp -R bin/debug/x64/* RadeonProImageProcessing_Debug/bin

    mkdir RadeonProImageProcessing_Release/bin
    cp -R bin/release/x64/* RadeonProImageProcessing_Release/bin

    rm RadeonProImageProcessing_Release/bin/UnitTest*
    rm RadeonProImageProcessing_Release/bin/libGtest*

    rm RadeonProImageProcessing_Debug/bin/UnitTest*
    rm RadeonProImageProcessing_Debug/bin/libGtest*

    tar cf RadeonProImageProcessing_OSX_Release.tar RadeonProImageProcessing_Release
    tar cf RadeonProImageProcessing_OSX_Debug.tar RadeonProImageProcessing_Debug
    """

    archiveArtifacts "RadeonProImageProcessing_OSX*.tar"
    rtp nullAction: '1', parserName: 'HTML', stableText: """<h3><a href="${BUILD_URL}/artifact/RadeonProImageProcessing_OSX_Release.tar">RadeonProImageProcessing_OSX_Release.tar</a></h3>"""
    rtp nullAction: '1', parserName: 'HTML', stableText: """<h3><a href="${BUILD_URL}/artifact/RadeonProImageProcessing_OSX_Debug.tar">RadeonProImageProcessing_OSX_Debug.tar</a></h3>"""
}

def executeBuildLinux(String cmakeKeys, String osName)
{
    sh """
    chmod +x tools/premake/linux64/premake5
    tools/premake/linux64/premake5 --embed_kernels gmake --generate_build_info ${cmakeKeys} >> ${STAGE_NAME}.log 2>&1
    make config=release_x64                                             >> ${STAGE_NAME}.log 2>&1
    make config=debug_x64                                               >> ${STAGE_NAME}.log 2>&1
    """

    sh """
    mkdir RadeonProImageProcessing_Debug
    cp -r models RadeonProImageProcessing_Debug
    cp -r include RadeonProImageProcessing_Debug
    cp README.md RadeonProImageProcessing_Debug

    cp -r RadeonProImageProcessing_Debug RadeonProImageProcessing_Release

    mkdir RadeonProImageProcessing_Debug/bin
    cp -r bin/debug/x64/* RadeonProImageProcessing_Debug/bin

    mkdir RadeonProImageProcessing_Release/bin
    cp -r bin/release/x64/* RadeonProImageProcessing_Release/bin

    rm RadeonProImageProcessing_Release/bin/UnitTest*
    rm RadeonProImageProcessing_Release/bin/libGtest*

    rm RadeonProImageProcessing_Debug/bin/UnitTest*
    rm RadeonProImageProcessing_Debug/bin/libGtest*

    tar cf RadeonProImageProcessing_${osName}_Debug.tar RadeonProImageProcessing_Debug
    tar cf RadeonProImageProcessing_${osName}_Release.tar RadeonProImageProcessing_Release
    """

    archiveArtifacts "RadeonProImageProcessing_${osName}*.tar"
    rtp nullAction: '1', parserName: 'HTML', stableText: """<h3><a href="${BUILD_URL}/artifact/RadeonProImageProcessing_${osName}_Release.tar">RadeonProImageProcessing_${osName}_Release.tar</a></h3>"""
    rtp nullAction: '1', parserName: 'HTML', stableText: """<h3><a href="${BUILD_URL}/artifact/RadeonProImageProcessing_${osName}_Debug.tar">RadeonProImageProcessing_${osName}_Debug.tar</a></h3>"""
}

def executeBuildCentOS7(String cmakeKeys)
{
    sh """
    tools/premake/centos7/premake5 --embed_kernels gmake --generate_build_info ${cmakeKeys} >> ${STAGE_NAME}.log 2>&1
    make config=release_x64                                             >> ${STAGE_NAME}.log 2>&1
    make config=debug_x64                                               >> ${STAGE_NAME}.log 2>&1
    """

    sh """
    mkdir RadeonProImageProcessing_Debug
    cp -r models RadeonProImageProcessing_Debug
    cp -r include RadeonProImageProcessing_Debug
    cp README.md RadeonProImageProcessing_Debug

    cp -r RadeonProImageProcessing_Debug RadeonProImageProcessing_Release

    mkdir RadeonProImageProcessing_Debug/bin
    cp -r bin/debug/x64/* RadeonProImageProcessing_Debug/bin

    mkdir RadeonProImageProcessing_Release/bin
    cp -r bin/release/x64/* RadeonProImageProcessing_Release/bin

    rm RadeonProImageProcessing_Release/bin/UnitTest*
    rm RadeonProImageProcessing_Release/bin/libGtest*

    rm RadeonProImageProcessing_Debug/bin/UnitTest*
    rm RadeonProImageProcessing_Debug/bin/libGtest*

    tar cf RadeonProImageProcessing_CentOS7_Debug.tar RadeonProImageProcessing_Debug
    tar cf RadeonProImageProcessing_CentOS7_Release.tar RadeonProImageProcessing_Release
    """

    archiveArtifacts "RadeonProImageProcessing_CentOS7*.tar"
    rtp nullAction: '1', parserName: 'HTML', stableText: """<h3><a href="${BUILD_URL}/artifact/RadeonProImageProcessing_CentOS7_Release.tar">RadeonProImageProcessing_CentOS7_Release.tar</a></h3>"""
    rtp nullAction: '1', parserName: 'HTML', stableText: """<h3><a href="${BUILD_URL}/artifact/RadeonProImageProcessing_CentOS7_Debug.tar">RadeonProImageProcessing_CentOS7_Debug.tar</a></h3>"""
}

def executePreBuild(Map options)
{
    checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')

    AUTHOR_NAME = bat (
            script: "git show -s --format=%%an HEAD ",
            returnStdout: true
            ).split('\r\n')[2].trim()

    echo "The last commit was written by ${AUTHOR_NAME}."
    options.AUTHOR_NAME = AUTHOR_NAME

    commitMessage = bat ( script: "git log --format=%%B -n 1", returnStdout: true ).split('\r\n')[2].trim()
    echo "Commit message: ${commitMessage}"
    options.commitMessage = commitMessage

    if (env.BRANCH_NAME && env.BRANCH_NAME == "master") {
        properties([[$class: 'BuildDiscarderProperty', strategy:
                         [$class: 'LogRotator', artifactDaysToKeepStr: '',
                          artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
    } else if (env.BRANCH_NAME && BRANCH_NAME != "master") {
        properties([[$class: 'BuildDiscarderProperty', strategy:
                         [$class: 'LogRotator', artifactDaysToKeepStr: '',
                          artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '3']]]);
    }
}

def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')
        outputEnvironmentInfo(osName)

        switch(osName)
        {
        case 'Windows':
            executeBuildWindows(options.cmakeKeys);
            break;
        case 'OSX':
            executeBuildOSX(options.cmakeKeys);
            break;
        case 'CentOS7':
            executeBuildCentOS7(options.cmakeKeys);
            break;
        default:
            executeBuildLinux(options.cmakeKeys, osName);
        }

        stash includes: 'bin/**/*', name: "app${osName}"
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
}

def call(String projectBranch = "",
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;Ubuntu;Ubuntu18:NVIDIA_GTX980;OSX:RadeonPro560;CentOS7',
         Boolean updateRefs = false,
         Boolean enableNotifications = true,
         String cmakeKeys = '') {

    String PRJ_NAME="RadeonProImageProcessor"
    String PRJ_ROOT="rpr-core"

    multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, null,
                           [projectBranch:projectBranch,
                            enableNotifications:enableNotifications,
                            BUILDER_TAG:'BuilderS',
                            TESTER_TAG:'RIF',
                            executeBuild:true,
                            executeTests:true,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT,
                            cmakeKeys:cmakeKeys])
}
