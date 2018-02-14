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
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/Radeon-Pro/RadeonProImageProcessing.git')

        outputEnvironmentInfo(osName)
        unstash "app${osName}"

        dir('UnitTest')
        {
            executeTestCommand(osName)
        }               
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        println(e.getStackTrace());    
        
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
    .\\Tools\\premake\\win\\premake5 vs2015                             >> ${STAGE_NAME}.log 2>&1
    set solution=.\\RadeonImageFilters.sln
    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Release;Platform=x64 %parameters% %solution% >> ${STAGE_NAME}.log 2>&1
    %msbuild% /target:%target% %maxcpucount% /property:Configuration=Debug;Platform=x64 %parameters% %solution% >> ${STAGE_NAME}.log 2>&1
    """
}

def executeBuildOSX()
{
    sh """
        Tools/premake/osx/premake5 --use_opencl --embed_kernels gmake   >> ${STAGE_NAME}.log 2>&1
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
            ren Windows Win
            cd Win
            ren RadeonImageFilters inc
            ren Bin lib
            move "lib\\Debug\\x64\\RadeonImageFilters64D.dll" "lib"
            move "lib\\Debug\\x64\\RadeonImageFilters64D.lib" "lib"
            move "lib\\Release\\x64\\RadeonImageFilters64.dll" "lib"
            move "lib\\Release\\x64\\RadeonImageFilters64.lib" "lib"
            cd lib
            rmdir /s /q Release
            rmdir /s /q Debug
            cd ..\\..

            mkdir Linux
            move Ubuntu Linux
            cd Linux\\Ubuntu
            ren RadeonImageFilters include
            ren Bin lib64
            move "lib64\\Release\\x64\\libRadeonImageFilters64.so" "lib64"
            cd lib64
            rmdir /s /q Release
            rmdir /s /q Debug
            cd ..\\..\\..

            ren OSX Mac
            cd Mac
            ren RadeonImageFilters inc
            ren Bin lib
            move "lib\\Release\\x64\\libRadeonImageFilters64.dylib" "lib"
            cd lib
            rmdir /s /q Release
            rmdir /s /q Debug
            cd ..\\..

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
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100;Ubuntu;OSX', 
         Boolean updateRefs = false, Boolean enableNotifications = true) {
    
    String PRJ_NAME="RadeonProImageProcessor"
    String PRJ_ROOT="rpr-core"
    properties([[$class: 'BuildDiscarderProperty', strategy: 
                 [$class: 'LogRotator', artifactDaysToKeepStr: '', 
                  artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);

    multiplatform_pipeline(platforms, this.&executeBuild, this.&executeTests, this.&executeDeploy, 
                           [projectBranch:projectBranch, 
                            enableNotifications:enableNotifications,
                            BUILDER_TAG:'BuilderS',
                            PRJ_NAME:PRJ_NAME,
                            PRJ_ROOT:PRJ_ROOT])
}
