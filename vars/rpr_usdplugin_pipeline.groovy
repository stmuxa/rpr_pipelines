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
    String CMD_BUILD_USD = """
    if exist USDgen rmdir /s/q USDgen
    if exist USDinst rmdir /s/q USDinst
    python USD\\build_scripts\\build_usd.py -v --build ${WORKSPACE}/USDgen/build --src ${WORKSPACE}/USDgen/src ${WORKSPACE}/USDinst > ${STAGE_NAME}_USD.log 2>&1
    """

    CMD_BUILD_USD = options.rebuildUSD ? CMD_BUILD_USD : "echo \"Skip USD build\""

    withEnv(["PATH=c:\\python27\\;c:\\python27\\scripts\\;${PATH}", "WORKSPACE=${env.WORKSPACE.toString().replace('\\', '/')}"]) {
        bat """
        call "C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\Community\\VC\\Auxiliary\\Build\\vcvarsall.bat" amd64 >> ${STAGE_NAME}.log 2>&1
        python -m pip install --upgrade pip >> ${STAGE_NAME}.log 2>&1
        python -m pip install pyside PyOpenGL >> ${STAGE_NAME}.log 2>&1

        ${CMD_BUILD_USD}

        set PATH=${WORKSPACE}\\USDinst\\bin;${WORKSPACE}\\USDinst\\lib;%PATH%
        set PYTHONPATH=${WORKSPACE}\\USDinst\\lib\\python;%PYTHONPATH%

        pushd RadeonProRenderThirdPartyComponents\\RadeonProRender-SDK\\Win
        mkdir rprTools
        move *.cpp rprTools
        move *.h rprTools
        popd

        mkdir RadeonProRenderUSD\\build
        pushd RadeonProRenderUSD\\build

        cmake -G "Visual Studio 15 2017 Win64" -DUSD_INCLUDE_DIR="${WORKSPACE}/USDinst/include" -DUSD_LIBRARY_DIR="${WORKSPACE}/USDinst/lib" ^
        -DRPR_LOCATION="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Win" ^
        -DRPR_LOCATION_LIB="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Win/lib" ^
        -DRPR_LOCATION_INCLUDE="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Win/inc" ^
        -DRPR_BIN_LOCATION="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Win/bin" ^
        -DRIF_LOCATION="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Windows" ^
        -DRIF_LOCATION_LIB="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Windows/lib" ^
        -DRIF_LOCATION_INCLUDE="${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Windows/inc" ^
        -DRPR_BUILD_AS_HOUDINI_PLUGIN=${options.enableHoudini.toString().toUpperCase()} ^
        -DHOUDINI_ROOT="C:/Program Files/Side Effects Software/Houdini 18.0.260" ^
        -DGLEW_LOCATION="${WORKSPACE}/USDinst" ^
        -DCMAKE_INSTALL_PREFIX="${WORKSPACE}/USDinst" .. >> ..\\..\\${STAGE_NAME}.log 2>&1

        msbuild /t:Build /p:Configuration=Release usdai.sln >> ..\\..\\${STAGE_NAME}.log 2>&1
        """
    }
}

def executeBuildOSX(Map options) {
    String CMD_BUILD_USD = """
    if [ -d "./USDgen" ]; then
        rm -fdr ./USDgen
    fi

    if [ -d "./USDinst" ]; then
        rm -fdr ./USDinst
    fi

    mkdir -p USDgen
    mkdir -p USDinst

    python USD/build_scripts/build_usd.py -vvv --build USDgen/build --src USDgen/src USDinst > ${STAGE_NAME}_USD.log 2>&1
    """

    CMD_BUILD_USD = options.rebuildUSD ? CMD_BUILD_USD : "echo \"Skip USD build\""

    withEnv(["OS="]) {
        sh """
        pip install --user --upgrade pip >> ${STAGE_NAME}.log 2>&1
        pip install --user pyside2 >> ${STAGE_NAME}.log 2>&1
        pip install --user PyOpenGL >> ${STAGE_NAME}.log 2>&1

        ${CMD_BUILD_USD}

        export PATH=${WORKSPACE}/USDinst/bin:\$PATH
        export PYTHONPATH=${WORKSPACE}/USDinst/lib/python:\$PYTHONPATH

        pushd RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Mac
        mkdir rprTools
        mv *.cpp rprTools
        mv *.h rprTools
        popd

        mkdir -p RadeonProRenderUSD/build
        pushd RadeonProRenderUSD/build

        cmake -DUSD_INCLUDE_DIR=${WORKSPACE}/USDinst/include -DUSD_LIBRARY_DIR=${WORKSPACE}/USDinst/lib \
        -DRPR_LOCATION=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Mac \
        -DRPR_LOCATION_LIB=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Mac/lib \
        -DRPR_LOCATION_INCLUDE=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Mac/inc \
        -DRIF_LOCATION=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Mac \
        -DRIF_LOCATION_LIB=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Mac/lib \
        -DRIF_LOCATION_INCLUDE=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Mac/inc \
        -DRPR_BUILD_AS_HOUDINI_PLUGIN=${options.enableHoudini.toString().toUpperCase()} \
        -DHOUDINI_ROOT=/Applications/Houdini/Current/Frameworks/Houdini.framework/Versions/Current/Resources \
        -DGLEW_LOCATION=${WORKSPACE}/USDinst \
        -DCMAKE_INSTALL_PREFIX=${WORKSPACE}/USDinst \
        -DCMAKE_PREFIX_PATH=${WORKSPACE}/USDinst \
        -DCMAKE_BUILD_TYPE=Release \
        .. >> ../../${STAGE_NAME}.log 2>&1

        make >> ../../${STAGE_NAME}.log 2>&1
        """
    }
}

def executeBuildLinux(Map options) {
    String CMD_BUILD_USD = """
    if [ -d "./USDgen" ]; then
        rm -fdr ./USDgen
    fi

    if [ -d "./USDinst" ]; then
        rm -fdr ./USDinst
    fi

    mkdir -p USDgen
    mkdir -p USDinst

    python USD/build_scripts/build_usd.py -vvv --build USDgen/build --src USDgen/src USDinst > ${STAGE_NAME}_USD.log 2>&1
    """

    CMD_BUILD_USD = options.rebuildUSD ? CMD_BUILD_USD : "echo \"Skip USD build\""
    // set $OS=null because it use in TBB build script
    withEnv(["OS="]) {
        sh """
        echo ${SUDO_PASS} | sudo -S apt install -y libglew-dev libxrandr-dev libxcursor-dev libxinerama-dev libxi-dev >> ${STAGE_NAME}.log 2>&1
        echo ${SUDO_PASS} | sudo -S apt install -y python-pyside pyside-tools >> ${STAGE_NAME}.log 2>&1
        pip install --user --upgrade pip >> ${STAGE_NAME}.log 2>&1
        pip install --user PyOpenGL >> ${STAGE_NAME}.log 2>&1

        ${CMD_BUILD_USD}

        export PATH=${WORKSPACE}/USDinst/bin:\$PATH
        export PYTHONPATH=${WORKSPACE}/USDinst/lib/python:\$PYTHONPATH

        cd RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Linux-Ubuntu
        mkdir RprTools
        mv *.cpp RprTools
        mv *.h RprTools
        cd ${WORKSPACE}

        mkdir -p RadeonProRenderUSD/build
        cd RadeonProRenderUSD/build

        cmake -DUSD_INCLUDE_DIR=${WORKSPACE}/USDinst/include -DUSD_LIBRARY_DIR=${WORKSPACE}/USDinst/lib \
        -DRPR_LOCATION=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Linux-Ubuntu \
        -DRPR_LOCATION_LIB=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Linux-Ubuntu/lib \
        -DRPR_LOCATION_INCLUDE=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProRender-SDK/Linux-Ubuntu/inc \
        -DRIF_LOCATION=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Linux/Ubuntu \
        -DRIF_LOCATION_LIB=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Linux/Ubuntu/lib64 \
        -DRIF_LOCATION_INCLUDE=${WORKSPACE}/RadeonProRenderThirdPartyComponents/RadeonProImageProcessing/Linux/Ubuntu/include \
        -DRPR_BUILD_AS_HOUDINI_PLUGIN=${options.enableHoudini.toString().toUpperCase()} \
        -DHOUDINI_ROOT=/opt/hfs18.0.260 \
        -DGLEW_LOCATION=${WORKSPACE}/USDinst \
        -DCMAKE_INSTALL_PREFIX=${WORKSPACE}/USDinst \
        -DCMAKE_PREFIX_PATH=${WORKSPACE}/USDinst \
        -DCMAKE_BUILD_TYPE=Release \
        .. >> ../../${STAGE_NAME}.log 2>&1

        make >> ../../${STAGE_NAME}.log 2>&1
        """
    }
}

def executeBuild(String osName, Map options) {
    try {
        dir('RadeonProRenderUSD') {
            checkoutGit(options['projectBranch'], 'https://github.com/GPUOpen-LibrariesAndSDKs/RadeonProRenderUSD.git')
        }

        dir('RadeonProRenderThirdPartyComponents') {
            checkoutGit(options['thirdpartyBranch'], 'https://github.com/Radeon-Pro/RadeonProRenderThirdPartyComponents.git')
        }
        if (options.rebuildUSD) {
            dir('USD') {
                checkoutGit(options['usdBranch'], 'https://github.com/PixarAnimationStudios/USD.git')
            }
        }

        outputEnvironmentInfo(osName)

        switch(osName) {
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
        if (options.sendToRBS) {
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

def executePreBuild(Map options) {
    currentBuild.description = ""
    ['projectBranch', 'thirdpartyBranch', 'packageBranch'].each {
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
        Boolean sendToRBS = false,
        Boolean enableHoudini = false,
        Boolean rebuildUSD = true)
{
    try
    {
        String PRJ_NAME="RadeonProRenderUSD"
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
                                enableHoudini:enableHoudini,
                                rebuildUSD:rebuildUSD,
                                BUILDER_TAG:'Builder6'
                                ])
    }
    catch(e) {
        currentBuild.result = "FAILED"
        println(e.toString());
        println(e.getMessage());
        throw e
    }
}
