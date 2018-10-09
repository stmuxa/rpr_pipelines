

def executeTests(String osName, String asicName, Map options)
{
    try {
        checkoutGit(options['testsBranch'], 'git@github.com:luxteam/testCleanCIS.git')

        bat '''
          sleep.bat
        '''
    }
    catch (e) {
        println(e.toString());
        println(e.getMessage());
        currentBuild.result = "FAILED"
        throw e
    }
    finally {
        archiveArtifacts "*.log"
    }
}

def executeBuildWindows(Map options)
{
    echo "Build stage"
}


def executeBuild(String osName, Map options)
{
    try {        
        
        switch(osName)
        {
        case 'Windows': 
            executeBuildWindows(options); 
            break;
        case 'OSX':
            executeBuildOSX(options);
            break;
        default: 
            executeBuildLinux(options, osName);
        }
    }
    catch (e) {
        currentBuild.result = "FAILED"
        throw e
    }                    
}

def executePreBuild(Map options)
{
    echo "PreBuild"
    options['executeBuild'] = true
    options['executeTests'] = true
}

def executeDeploy(Map options, List platformList, List testResultList)
{
    echo "Deploy"
}

def call(String projectBranch = "", String thirdpartyBranch = "master", 
         String packageBranch = "master", String testsBranch = "master",
         String platforms = 'Windows:AMD_Test', 
         Boolean updateRefs = false, Boolean enableNotifications = true,
         Boolean incrementVersion = true,
         Boolean skipBuild = false,
         String renderDevice = "gpu",
         String testsPackage = "",
         String tests = "",
         Boolean forceBuild = false) {

    try
    {
        String PRJ_NAME="RadeonProRenderBlenderPlugin"
        String PRJ_ROOT="rpr-plugins"

        multiplatform_pipeline(platforms, this.&executePreBuild, this.&executeBuild, this.&executeTests, this.&executeDeploy, 
                               [projectBranch:projectBranch, 
                                thirdpartyBranch:thirdpartyBranch, 
                                packageBranch:packageBranch, 
                                testsBranch:testsBranch, 
                                updateRefs:updateRefs, 
                                enableNotifications:enableNotifications,
                                PRJ_NAME:PRJ_NAME,
                                PRJ_ROOT:PRJ_ROOT,
                                incrementVersion:incrementVersion,
                                skipBuild:skipBuild,
                                renderDevice:renderDevice,
                                testsPackage:testsPackage,
                                tests:tests.replace(',', ' '),
                                forceBuild:forceBuild,
                                reportName:'Test_20Report',
                                slackChannel:"cis_notification_test",
                                slackBaseUrl:"https://luxcis.slack.com/services/hooks/jenkins-ci/",
                                slackTocken:"${SLACK_LUXCIS_TOKEN}"])
    }
    catch (e) {
        currentBuild.result = "INIT FAILED"
        println(e.toString());
        println(e.getMessage());
        
        throw e
    }
}
