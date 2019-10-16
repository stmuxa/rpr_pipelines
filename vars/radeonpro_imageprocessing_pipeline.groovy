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
    String osName = "Windows"

    commit = bat (
        script: '''@echo off
                   git rev-parse --short=6 HEAD''',
        returnStdout: true
    ).trim()

    String branch = env.BRANCH_NAME ? env.BRANCH_NAME : env.Branch
    branch = branch.replace('origin/', '')

    String packageName = 'radeonimagefilters' + (branch ? '-' + branch : '') + (commit ? '-' + commit : '') + '-' + osName
    packege_name = packageName.replaceAll('[^a-zA-Z0-9-_]+','')

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
    mkdir ${packageName}-dbg
    xcopy models ${packageName}-dbg\\models /s/y/i
    xcopy include ${packageName}-dbg\\include /y/i
    xcopy README.md ${packageName}-dbg\\README.md*

    xcopy ${packageName}-dbg ${packageName}-rel /s/y/i

    xcopy bin\\debug\\x64 ${packageName}-dbg\\bin /s/y/i
    xcopy bin\\release\\x64 ${packageName}-rel\\bin /s/y/i

    cd ${packageName}-rel
    del /S Gtest64*
    del /S UnitTest64*

    cd ..\\${packageName}-dbg
    del /S Gtest64*
    del /S UnitTest64*

    cd ..
    mkdir RIF_Debug
    mkdir RIF_Release

    move ${packageName}-dbg RIF_Debug
    move ${packageName}-rel RIF_Release
    """

    zip archive: true, dir: 'RIF_Debug', glob: '', zipFile: "${packageName}-dbg.zip"
    zip archive: true, dir: 'RIF_Release', glob: '', zipFile: "${packageName}-rel.zip"

    rtp nullAction: '1', parserName: 'HTML', stableText: """<h4>${osName}: <a href="${BUILD_URL}/artifact/${packageName}-rel.zip">release</a> / <a href="${BUILD_URL}/artifact/${packageName}-dbg.zip">debug</a></h4>"""
}

def executeBuildUnix(String cmakeKeys, String osName, String premakeDir, String copyKeys)
{

    commit = sh (
        script: 'git rev-parse --short=6 HEAD',
        returnStdout: true
    ).trim()

    String branch = env.BRANCH_NAME ? env.BRANCH_NAME : env.Branch
    branch = branch.replace('origin/', '')

    String packageName = 'radeonimagefilters' + (branch ? '-' + branch : '') + (commit ? '-' + commit : '') + '-' + osName
    packageName = packageName.replaceAll('[^a-zA-Z0-9-_]+','')

    sh """
    chmod +x tools/premake/${premakeDir}/premake5
    tools/premake/${premakeDir}/premake5 --embed_kernels gmake --generate_build_info ${cmakeKeys} >> ${STAGE_NAME}.log 2>&1
    make config=release_x64                                             >> ${STAGE_NAME}.log 2>&1
    make config=debug_x64                                               >> ${STAGE_NAME}.log 2>&1
    """

    sh """
    mkdir ${packageName}-dbg
    cp ${copyKeys} models ${packageName}-dbg
    cp ${copyKeys} include ${packageName}-dbg
    cp README.md ${packageName}-dbg

    cp ${copyKeys} ${packageName}-dbg ${packageName}-rel

    mkdir ${packageName}-dbg/bin
    cp ${copyKeys} bin/debug/x64/* ${packageName}-dbg/bin

    mkdir ${packageName}-rel/bin
    cp ${copyKeys} bin/release/x64/* ${packageName}-rel/bin

    rm ${packageName}-rel/bin/UnitTest*
    rm ${packageName}-rel/bin/libGtest*

    rm ${packageName}-dbg/bin/UnitTest*
    rm ${packageName}-dbg/bin/libGtest*

    tar cf ${packageName}-dbg.tar ${packageName}-dbg
    tar cf ${packageName}-rel.tar ${packageName}-rel
    """

    archiveArtifacts "${packageName}*.tar"
    rtp nullAction: '1', parserName: 'HTML', stableText: """<h4>${osName}: <a href="${BUILD_URL}/artifact/${packageName}-rel.tar">release</a> / <a href="${BUILD_URL}/artifact/${packageName}-dbg.tar">debug</a></h4>"""
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
            executeBuildUnix(options.cmakeKeys, osName, 'osx', '-R');
            break;
        case 'CentOS7':
            executeBuildUnix(options.cmakeKeys, osName, 'centos7', '-r');
            break;
        case 'Ubuntu':
            executeBuildUnix(options.cmakeKeys, 'Ubuntu16', 'linux64', '-r');
            break;
        case 'Ubuntu18':
            executeBuildUnix(options.cmakeKeys, osName, 'linux64', '-r');
            break;
        default:
            error('Unsupported OS');
        }

        //TODO: add samples to archives after samples update
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
