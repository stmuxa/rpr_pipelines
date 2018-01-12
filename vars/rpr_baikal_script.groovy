def executeTestWindows(String asicName, String osName = "Windows")
{
    def retNode = {
        node("${osName} && Tester && OpenCL && gpu${asicName}")
        {
            stage("Test-${current_profile}-${osName}")
            {
                try {
                    bat "set > ${STAGE_NAME}.log"
                    unstash "app${osName}"
                }
                catch (e) {
                    // If there was an exception thrown, the build failed
                    currentBuild.result = "FAILED"
                    throw e
                }
                finally {
                    archiveArtifacts "${STAGE_NAME}.log"
                }
            }
        }
    }
    return retNode
}

def executeTestOSX(String asicName, String osName = "OSX")
{
    def retNode = {
        node("${osName} && Tester && OpenCL && gpu${asicName}")
        {
            stage("Test-${current_profile}-${osName}") {
            {
                try {
                    sh "env > ${STAGE_NAME}.log"
                    unstash "app${osName}"
                }
                catch (e) {
                    // If there was an exception thrown, the build failed
                    currentBuild.result = "FAILED"
                    throw e
                }
                finally {
                    archiveArtifacts "${STAGE_NAME}.log"
                }
            }
        }
    }
    return retNode
}

def executeBuildWindows(String projectBranch, String osName = "Windows")
{
    def retNode = {
        node("${osName} && Builder")
        {
            stage("Build-${osName}")
            {
                String JOB_NAME_FMT="${JOB_NAME}".replace('%2F', '_')

                ws("WS/${JOB_NAME_FMT}") {
                    try {
                        bat "set > Build_${osName}.log"

                    }
                    catch (e) {
                        // If there was an exception thrown, the build failed
                        currentBuild.result = "FAILED"
                        throw e
                    }
                    finally {
                        archiveArtifacts "Build_${osName}.log"
                    }
                }
            }
        }
    }
    return retNode
}

def executeBuildOSX(String projectBranch, String osName = "OSX")
{
    def retNode = {
        node("${osName} && Builder")
        {
            stage("Build-${osName}")
            {
                ws("WS/${JOB_NAME_FMT}") {
                    sh "env > Build_${osName}.log"
                    try {
                        
                    }
                    catch (e) {
                        // If there was an exception thrown, the build failed
                        currentBuild.result = "FAILED"
                        throw e
                    }
                    finally {
                        archiveArtifacts "Build_${osName}.log"
                    }
                }
            }
        }
    }
    return retNode
}

def executeBuildLinux(String projectBranch, String osName)
{
    def retNode = {
        node("${osName} && Builder")
        {
            stage("Build-${osName}")
            {
                ws("WS/${JOB_NAME_FMT}") {
                    sh "env > Build_${osName}.log"
                    try {

                    }
                    catch (e) {
                        // If there was an exception thrown, the build failed
                        currentBuild.result = "FAILED"
                        throw e
                    }
                    finally {
                        archiveArtifacts "Build_${osName}.log"
                    }
                }
            }
        }
    }
    return retNode
}

def executeTests(String testPlatforms)
{
    def tasks = [:]
    
    testPlatforms.split(';').each()
    {
        def (osName, gpuName) = "${it}".tokenize(':')
        if(osName == 'Windows')
        {
            tasks["${it}"] = executeTestWindows("${gpuName}")
        }
        else
        if(osName == 'OSX')
        {
            tasks["${it}"] = executeTestOSX("${gpuName}")
        }
        else
        {
            tasks["${it}"] = executeTestLinux("${gpuName}", osName)
        }
    }
    
    parallel tasks
}

def executeBuilds(String buildsGroup, String projectBranch, String thirdpartyBranch, String packageBranch)
{
    def tasks = [:]

    tasks["Build-Windows"] = executeBuildWindowsVS2015(projectBranch)
    tasks["Build-Ubuntu"] = executeBuildLinux(projectBranch, "Ubuntu")
    tasks["Build-OSX"] = executeBuildOSX(projectBranch)

    parallel tasks
}

def call(String buildsGroup = "AutoBuilds", String projectBranch = "", 
         String testPlatforms = 'Windows:AMD_RXVEGA;Windows:AMD_WX9100;Windows:AMD_WX7100', Boolean enableNotifications = true) {
      
    try {
        timestamps {
            executeBuilds(projectBranch)
            executeTests(testPlatforms)
        }
    }
    catch (e) {
        // If there was an exception thrown, the build failed
        currentBuild.result = "FAILED"
    }
    finally {
        if(enableNotifications)
        {
            sendBuildStatusNotification(currentBuild.result)
        }
    }
}
