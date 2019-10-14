def executeGenTestRefCommand(String osName, Map options)
{
    executeTestCommand(osName, options)

    try
    {
        //for update existing manifest file
        receiveFiles("${options.REF_PATH_PROFILE}/baseline_manifest.json", './Work/Baseline/')
    }
    catch(e)
    {
        println("baseline_manifest.json not found")
    }

    dir('scripts')
    {
        switch(osName)
        {
            case 'Windows':
                bat """
                make_results_baseline.bat
                """
                break;
            case 'OSX':
                sh """
                ./make_results_baseline.sh
                """
                break;
            default:
                sh """
                ./make_results_baseline.sh
                """
        }
    }
}

def executeTestCommand(String osName, Map options)
{
    if (!options['skipBuild'])
    {
        installPlugin(osName, options)
        //duct tape for migration to maya2019
        try {
            buildRenderCache(osName, "${options.stageName}.log")
        } catch(e) {
            println(e.toString())
            println("ERROR during building render cache")
        }
    }

    switch(osName)
    {
    case 'Windows':
        dir('scripts')
        {
            bat """
            run.bat ${options.renderDevice} ${options.testsPackage} \"${options.tests}\">> ../${options.stageName}.log  2>&1
            """
        }
        break;
    case 'OSX':
        dir('scripts')
        {
            sh """
            ./run.sh ${options.renderDevice} ${options.testsPackage} \"${options.tests}\" >> ../${options.stageName}.log 2>&1
            """
        }
        break;
    default:
        sh """
        echo 'sample image' > ./OutputImages/sample_image.txt
        """
    }
}

def executeTests(String osName, String asicName, Map options)
{
}

def executeBuildWindows(Map options)
{
    //TODO: remove binWin64 renaming
    withEnv(["PATH=c:\\python27\\;c:\\python27\\scripts\\;${PATH}"]) {
        bat """
        if exists USDgen rmdir /s/q USDgen
        if exists USDinst rmdir /s/q USDinst

        "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\VC\\Auxiliary\\Build\\vcvarsall.bat" amd64 >> ${STAGE_NAME}.log 2>&1
        python -m pip install --upgrade pip >> ${STAGE_NAME}.log 2>&1
        python -m pip install pyside PyOpenGL >> ${STAGE_NAME}.log 2>&1

        pushd USD
        python build_scripts\\build_usd.py  -vvv --build ${WORKSPACE}/USDgen/build --src ${WORKSPACE}/USDgen/src ${WORKSPACE}/USDinst >> ..\\${STAGE_NAME}.log 2>&1
        popd

        move RadeonProRenderThirdPartyComponents\\RadeonProRender-SDK\\Win\\bin RadeonProRenderThirdPartyComponents\\RadeonProRender-SDK\\Win\\binWin64 >> ${STAGE_NAME}.log 2>&1

        mkdir RadeonProRenderUSD\\build
        pushd RadeonProRenderUSD\\build

        cmake -G "Visual Studio 15 2017 Win64" -DUSD_INCLUDE_DIR="${WORKSPACE}/USDgen/build/include" -DUSD_LIBRARY_DIR="${WORKSPACE}/USDgen/build/lib" ^
        -DRPR_LOCATION="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Win" ^
        -DRPR_LOCATION_LIB="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Win/lib" ^
        -DRPR_LOCATION_INCLUDE="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Win/inc" ^
        -DRPR_BIN_LOCATION="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Win/bin" ^
        -DRIF_LOCATION="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Windows" ^
        -DRIF_LOCATION_LIB="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Windows/lib" ^
        -DRIF_LOCATION_INCLUDE="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Windows/inc" .. >> ..\\..\\${STAGE_NAME}.log 2>&1

        msbuild /t:Build /p:Configuration=Release usdai.sln >> ..\\..\\${STAGE_NAME}.log 2>&1
        """
    }
}

def executeBuildOSX(Map options)
{
    sh """
    if [ -d "${WORKSPACE}/USDgen" ]; then
        rm -fdr ${WORKSPACE}/USDgen
    fi
    if [ -d "${WORKSPACE}/USDsrc" ]; then
        rm -fdr ${WORKSPACE}/USDsrc
    fi
    """
}

def executeBuildLinux(Map options)
{
}

def executeBuild(String osName, Map options)
{
    try {
        dir('RadeonProRenderUSDPlugin')
        {
            checkoutGit(options['projectBranch'], 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRenderUSD.git')
        }
        dir('RadeonProRenderThirdPartyComponents')
        {
            checkoutGit(options['thirdpartyBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderThirdPartyComponents.git')
        }
        dir('USD')
        {
            checkoutGit(options['usdBranch'], 'https://github.com/PixarAnimationStudios/USD.git')
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
    }
    catch (e) {
        currentBuild.result = "FAILED"
        if (options.sendToRBS)
        {
            try {
                options.rbs_prod.setFailureStatus()
                options.rbs_dev.setFailureStatus()
            } catch (err) {
                println(err)
            }
        }
        throw e
    }
    finally {
        archiveArtifacts "*.log"
    }
}

def executePreBuild(Map options)
{
    currentBuild.description = ""
    ['projectBranch', 'thirdpartyBranch', 'packageBranch'].each
    {
        if(options[it] != 'master' && options[it] != "")
        {
            currentBuild.description += "<b>${it}:</b> ${options[it]}<br/>"
        }
    }
}

def executeDeploy(Map options, List platformList, List testResultList)
{
}

def call(String projectBranch = "",
        String thirdpartyBranch = "master",
        String usdBranch = "dev",
        String testsBranch = "master",
        String platforms = 'Windows;Ubuntu18;OSX',
        Boolean updateRefs = false,
        Boolean enableNotifications = true,
        Boolean incrementVersion = false,
        Boolean skipBuild = false,
        String renderDevice = "gpu",
        String testsPackage = "",
        String tests = "",
        Boolean forceBuild = false,
        Boolean splitTestsExectuion = false,
        Boolean sendToRBS = false)
{
    try
    {
        String PRJ_NAME="RadeonProRenderUSDPlugin"
        String PRJ_ROOT="rpr-plugins"

        multiplatform_pipeline(platforms, null, this.&executeBuild, this.&executeTests, null,
                               [projectBranch:projectBranch,
                                thirdpartyBranch:thirdpartyBranch,
                                usdBranch:usdBranch,
                                testsBranch:testsBranch,
                                updateRefs:updateRefs,
                                enableNotifications:enableNotifications,
                                PRJ_NAME:PRJ_NAME,
                                PRJ_ROOT:PRJ_ROOT,
                                incrementVersion:incrementVersion,
                                skipBuild:skipBuild,
                                renderDevice:renderDevice,
                                testsPackage:testsPackage,
                                tests:tests,
                                executeBuild:true,
                                executeTests:false,
                                forceBuild:forceBuild,
                                reportName:'Test_20Report',
                                splitTestsExectuion:splitTestsExectuion,
                                sendToRBS:sendToRBS,
                                TEST_TIMEOUT:720,
                                ])
    }
    catch(e) {
        currentBuild.result = "FAILED"
        println(e.toString());
        println(e.getMessage());
        throw e
    }
}
