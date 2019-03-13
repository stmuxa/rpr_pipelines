def setCommitStatus(String sha, String context, String repository, String backref, String message, String status)
{
    step([$class: 'GitHubCommitStatusSetter',
        commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: "${sha}"],
        contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: "${context}"],
        reposSource: [$class: 'ManuallyEnteredRepositorySource', url: "${repository}"],
        statusBackrefSource: [$class: 'ManuallyEnteredBackrefSource', backref: "${backref}"],
        statusResultSource: [$class: 'ConditionalStatusResultSource',results: [[$class: 'AnyBuildResult', message: "${message}", state: "${status}"]]]
        ])
}


def executeGenTestRefCommand(String osName)
{
    switch(osName)
    {
    case 'Windows':
        bat """
        echo 'sample image' > .\\ReferenceImages\\sample_image.txt
        """
        break;
    case 'OSX':
        sh """
        echo 'sample image' > ./ReferenceImages/sample_image.txt
        """
        break;
    default:
        sh """
        echo 'sample image' > ./ReferenceImages/sample_image.txt
        """
    }
}

def executeTestCommand(String osName, Map options)
{
    switch(osName)
    {
    case 'Windows':
        bat """
        echo 'sample image' > ${STAGE_NAME}.${options.RENDER_QUALITY}.log
        """
        break;
    case 'OSX':
        sh """
        echo 'sample image' > ${STAGE_NAME}.${options.RENDER_QUALITY}.log
        """
        break;
    default:
        sh """
        echo 'sample image' > ${STAGE_NAME}.${options.RENDER_QUALITY}.log
        """
    }
}

def executeTestsCustomQuality(String osName, String asicName, Map options)
{
    cleanWs()
    executeTestCommand(osName, options)
}

def executeTests(String osName, String asicName, Map options)
{
    options['testsQuality'].split(",").each() {
        options['RENDER_QUALITY'] = "${it}"
        String status = "success"
        try {
            executeTestsCustomQuality(osName, asicName, options)
        } catch(e) {
            status = "failure"
            println("Exception during [${options.RENDER_QUALITY}] quality tests execution")
        }
        finally {
            archiveArtifacts "*.log"
            setCommitStatus(pullRequest.head, "[TEST] ${osName}-${asicName}-${it}", "https://github.com/luxteam/statustest",
                "${env.JOB_URL}/artifact/${STAGE_NAME}.${options.RENDER_QUALITY}.log",
                "Testing finished", "SUCCESS")
            // pullRequest.createStatus(status,
            //     "[TEST] ${osName}-${asicName}-${it}",
            //     "Testing finished",
            //     "${env.JOB_URL}/artifact/${STAGE_NAME}.${options.RENDER_QUALITY}.log")
        }
    }
}

def executeBuildWindows()
{
    bat """
    HOSTNAME > ${STAGE_NAME}.log

    """
}

def executeBuildOSX()
{
    sh """
    uname -a > ${STAGE_NAME}.log

    """
}

def executeBuildLinux()
{
    sh """
    uname -a > ${STAGE_NAME}.log

    """
}

def executePreBuild(Map options)
{
    checkOutBranchOrScm(options['projectBranch'], options['projectRepo'])

    if(env.CHANGE_ID)
    {
        options['platforms'].split(';').each()
        { platform ->
            List tokens = platform.tokenize(':')
            String osName = tokens.get(0)
            // Statuses for builds
            pullRequest.createStatus("pending", "[BUILD] ${osName}", "Init", "${env.JOB_URL}")
            if (tokens.size() > 1)
            {
                gpuNames = tokens.get(1)
                gpuNames.split(',').each()
                { gpuName ->
                    options['testsQuality'].split(",").each()
                    { testQuality ->
                        // Statuses for tests
                        pullRequest.createStatus("pending",
                            "[TEST] ${osName}-${gpuName}-${testQuality}",
                            "Init",
                            "${env.JOB_URL}")
                    }
                }
            }
        }
    }
}

def executeBuild(String osName, Map options)
{
    try {
        checkOutBranchOrScm(options['projectBranch'], 'https://github.com/luxteam/MultiplatformSampleProject.git')
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
        
        //stash includes: 'Bin/**/*', name: "app${osName}"
    }
    catch (e) {
        pullRequest.removeLabel('Build Failed')
        pullRequest.addLabel('Build Failed')
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        pullRequest.removeLabel('Build Passed')
        pullRequest.addLabel('Build Passed')
        archiveArtifacts "*.log"
        String status = currentBuild.result ? "failure" : "success"
        pullRequest.createStatus(status,
            "[BUILD] ${osName}", "Build finished", "${env.BUILD_URL}/artifact/${STAGE_NAME}.log")
    }                        

}

def executeDeploy(Map options, List platformList, List testResultList)
{
    pullRequest.removeLabel('Tests Passed')
    pullRequest.addLabel('Tests Passed')
    String testsTable = """| total | failed | passsed |\n|-------|--------|---------|\n| 30    | 5      | 25      |"""
    // def comment = pullRequest.comment("Tests summary:\n\n ${testsTable}")
    def comment = pullRequest.comment("Checks for ${pullRequest.head} has been finished")
    
    /*echo "------------------"
    echo "Statuses"
    for (status in pullRequest.statuses) {
        echo "Commit: ${pullRequest.head}, State: ${status.state}, Context: ${status.context}, URL: ${status.targetUrl}"
    }
    echo "------------------"
    echo "Commit's Statuses"
    for (commit in pullRequest.commits) {
        for (status  in commit.statuses) {
            echo "Commit: ${commit.sha}, State: ${status.state}, Context: ${status.context}, URL: ${status.targetUrl}"
        }
    }*/
}

def call(String projectBranch = "", 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100,NVIDIA_GF1080TI;OSX:RadeonPro560;Ubuntu:AMD_WX7100', 
         String testsQuality = "low,medium",
         Boolean updateRefs = false,
         Boolean enableNotifications = false) {
    
    multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, this.&executeDeploy,
                           [projectBranch:projectBranch,
                           updateRefs:updateRefs, 
                           enableNotifications:enableNotifications,
                           platforms:platforms,
                           executeTests:true,
                           executeBuild:true,
                           testsQuality:testsQuality])
}
