def executeTestCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat "mkdir testSave"
        bat "..\\Bin\\Release\\x64\\UnitTest64.exe  --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ..\\${STAGE_NAME}.log  2>&1"
        break;
    case 'OSX':
        sh "mkdir testSave"
        sh "../Bin/Release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1"
        break;
    default:
        sh "mkdir testSave"
        sh "../Bin/Release/x64/UnitTest64           --gtest_output=xml:../${STAGE_NAME}.gtest.xml >> ../${STAGE_NAME}.log  2>&1"
    }
}

def executeTests(String osName, String asicName, Map options)
{
    //String PRJ_PATH="builds/rpr-core/RadeonProImageProcessor"
    //String REF_PATH="${PRJ_PATH}/ReferenceImages/${asicName}-${osName}"
    //String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}/${asicName}-${osName}".replace('%2F', '_')

    try {
        timeout(time: 40, unit: 'MINUTES')
        {
            checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')

            outputEnvironmentInfo(osName)
            unstash "app${osName}"

            dir('UnitTest')
            {
                executeTestCommand(osName)
            }
        }
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        
        //send if needed
        //sendFiles(osName, './OutputImages/*.*', PRJ_PATH)

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
    .\\Tools\\premake\\win\\premake5 --use_opencl --embed_kernels --use_ml_denoiser vs2015    >> ${STAGE_NAME}.log 2>&1
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
        default: 
            executeBuildLinux();
        }

        stash includes: 'Bin/**/*', name: "app${osName}"
        stash includes: 'RadeonImageFilters/*.h', name: "headers${osName}"
        stash includes: 'README.md', name: "readme"
        
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
    try {
        platformList.each()
        {
            String osName = it;
            dir(osName)
            {
                unstash "app${osName}"
                unstash "headers${osName}"
                
     
            }
            unstash "readme"
        }
       
    bat """
            mkdir Linux
            mkdir Mac
            mkdir Win

            mkdir Win\\inc
            mkdir Win\\lib

            mkdir Linux\\Ubuntu
            mkdir Linux\\Ubuntu\\include
            mkdir Linux\\Ubuntu\\lib64

            mkdir Mac\\inc
            mkdir Mac\\lib

            move Windows\\RadeonImageFilters\\RadeonImageFilters.h Win\\inc
            move Windows\\RadeonImageFilters\\RadeonImageFilters_cl.h Win\\inc
            move Windows\\RadeonImageFilters\\RadeonImageFilters_gl.h Win\\inc
            move Windows\\Bin\\Release\\x64\\RadeonImageFilters64.dll Win\\lib
            move Windows\\Bin\\Release\\x64\\RadeonImageFilters64.lib Win\\lib
            move Windows\\Bin\\Debug\\x64\\RadeonImageFilters64D.dll Win\\lib
            move Windows\\Bin\\Debug\\x64\\RadeonImageFilters64D.lib Win\\lib

            move README.md Linux
            move Ubuntu\\RadeonImageFilters\\RadeonImageFilters.h Linux\\Ubuntu\\include
            move Ubuntu\\RadeonImageFilters\\RadeonImageFilters_cl.h Linux\\Ubuntu\\include
            move Ubuntu\\RadeonImageFilters\\RadeonImageFilters_gl.h Linux\\Ubuntu\\include
            move Ubuntu\\Bin\\Release\\x64\\libRadeonImageFilters64.so Linux\\Ubuntu\\lib64

            move OSX\\RadeonImageFilters\\RadeonImageFilters.h Mac\\inc
            move OSX\\RadeonImageFilters\\RadeonImageFilters_cl.h Mac\\inc
            move OSX\\RadeonImageFilters\\RadeonImageFilters_gl.h Mac\\inc
            move OSX\\Bin\\Release\\x64\\libRadeonImageFilters64.dylib Mac\\lib

            rmdir /s /q Windows
            rmdir /s /q OSX
            rmdir /s /q Ubuntu

            "..\\..\\cis_tools\\7-Zip\\7z.exe" a RadeonProImageProcessing.zip Win Linux Mac """
        
    archiveArtifacts "RadeonProImageProcessing.zip"
        
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        //archiveArtifacts "${STAGE_NAME}.log"
    }   
}

def call(String projectBranch = "", 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100;Ubuntu;OSX:Intel_Iris,RadeonPro560', 
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
