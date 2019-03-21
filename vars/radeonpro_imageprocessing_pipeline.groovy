def executeTestCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        try
        {
            dir("Tools/Jenkins")
            {
                bat "pretest.bat >> ..\\..\\${STAGE_NAME}.log 2>&1"
            }
        }catch(e){}
        dir("UnitTest")
        {
            bat "mkdir testSave"
            bat "..\\Bin\\Release\\x64\\UnitTest64.exe  --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log  2>&1"
        }
        break;
    case 'OSX':
        try
        {
            dir("Tools/Jenkins")
            {
                sh """cmhod +x pretest.sh
                    ./pretest.sh >> ..\\..\\${STAGE_NAME}.log 2>&1"""
            }
        }catch(e){}
        dir("UnitTest")
        {
            sh "mkdir testSave"
            sh "../Bin/Release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1"
        }
        break;
    default:
        try
        {
            dir("Tools/Jenkins")
            {
                sh """cmhod +x pretest.sh
                    ./pretest.sh >> ..\\..\\${STAGE_NAME}.log 2>&1"""
            }
        }catch(e){}
        dir("UnitTest")
        {
            sh "mkdir testSave"
            sh "../Bin/Release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1"
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

def executeBuildWindows()
{
    bat """
    set msbuild=\"C:\\Program Files (x86)\\MSBuild\\14.0\\Bin\\MSBuild.exe\"
    if not exist %msbuild% (
        set msbuild=\"C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\MSBuild\\15.0\\Bin\\MSBuild.exe\"
    )
    set target=build
    set maxcpucount=/maxcpucount 
    set PATH=C:\\Python27\\;%PATH%
    .\\Tools\\premake\\win\\premake5 --use_opencl --embed_kernels vs2015    >> ${STAGE_NAME}.log 2>&1
    set solution=.\\RadeonImageFilters.sln
    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Release;Platform=x64 %parameters% %solution% >> ${STAGE_NAME}.log 2>&1
    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Debug;Platform=x64 %parameters% %solution% >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuildOSX()
{
    sh """
    export CXX=clang++
        Tools/premake/osx/premake5 --metal --embed_kernels gmake   >> ${STAGE_NAME}.log 2>&1
        make config=release_x64                                         >> ${STAGE_NAME}.log 2>&1
        make config=debug_x64                                           >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuildLinux()
{
    sh """
    chmod +x Tools/premake/linux64/premake5
    Tools/premake/linux64/premake5 --use_opencl --embed_kernels gmake   >> ${STAGE_NAME}.log 2>&1
    make config=release_x64                                             >> ${STAGE_NAME}.log 2>&1
    make config=debug_x64                                               >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuildCentOS7()
{
    sh """
    Tools/premake/centos7/premake5 --use_opencl --embed_kernels gmake   >> ${STAGE_NAME}.log 2>&1
    make config=release_x64                                             >> ${STAGE_NAME}.log 2>&1
    make config=debug_x64                                               >> ${STAGE_NAME}.log 2>&1
    """
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

    stash includes: 'README.md', name: "readme"
    stash includes: 'Samples/**/*', name: 'Samples'
    stash includes: 'models/**/*', name: 'models'   
}

def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')
        outputEnvironmentInfo(osName)

        switch(osName)
        {
        case 'Windows': 
            executeBuildWindows(); 
            break;
        case 'OSX':
            executeBuildOSX();
            break;
        case 'CentOS7':
            executeBuildCentOS7();
            break;
        default: 
            executeBuildLinux();
        }

        stash includes: 'Bin/**/*', name: "app${osName}"
        stash includes: 'RadeonImageFilters/*.h', name: "headers${osName}"
        stash includes: 'models/**/*', name: "modelsFolder${osName}"
        stash includes: 'README.md', name: "readme${osName}"
        
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
    cleanWs()
    try
    {
        dir("RadeonProImageProcessing")
        {
            platformList.each()
            {
                String osName = it;
                try {
                    dir(osName)
                    {
                        unstash "app${osName}"
                        unstash "headers${osName}"
                    }
                } catch(e)
                {
                    println(e.getMessage())
                    println("Can't unstash ${osName} build")
                }
            }
            try
            {
                unstash "readme"
                unstash "models"
                unstash "Samples"
            }
            catch(e)
            {
                println(e.getMessage())
                currentBuild.result = "FAILED"
            }
        }

        bat "xcopy RadeonProImageProcessing RadeonProImageProcessing_Release /s/y/i"
        dir('RadeonProImageProcessing_Release')
        {
            platformList.each()
            {
                dir("${it}")
                {
                    bat "rmdir /s/q Bin\\Debug"
                }
            }
            try
            {
                bat "del /S Gtest64.lib"
                bat "del /S OpenImageIO.dll"        
                bat "del /S UnitTest64*"
                bat "del /S libGtest64*"
                bat "del /S *.lua"
            }
            catch(e)
            {
                println(e.getMessage())
            }
        }
    }
    catch (e)
    {
        println(e.getMessage())
        currentBuild.result = "FAILED"
        throw e
    }
    finally
    {
        zip archive: true, dir: 'RadeonProImageProcessing', glob: '', zipFile: 'RadeonProImageProcessing.zip'
        zip archive: true, dir: 'RadeonProImageProcessing_Release', glob: '', zipFile: 'RadeonProImageProcessing_Release.zip'
    }
}

def call(String projectBranch = "", 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;Ubuntu;OSX:RadeonPro560;CentOS7', 
         Boolean updateRefs = false, Boolean enableNotifications = true) {
    
    String PRJ_NAME="RadeonProImageProcessor"
    String PRJ_ROOT="rpr-core"
    properties([[$class: 'BuildDiscarderProperty', strategy: 
                 [$class: 'LogRotator', artifactDaysToKeepStr: '', 
                  artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);

    multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, this.&executeDeploy, 
                           [projectBranch:projectBranch, 
                            enableNotifications:enableNotifications,
                            BUILDER_TAG:'BuilderS',
                            executeBuild:true,
                            executeTests:true,
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT])
}
