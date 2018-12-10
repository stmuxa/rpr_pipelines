def call()
{
    stage("init")
    {
        echo "start"
        properties([parameters([string(defaultValue: 'PR', description: 'Test suite', name: 'Tests', trim: false)]), [$class: 'JobPropertyImpl', throttle: [count: 6, durationName: 'hour', userBoost: true]]])
    }
    try{
        stage("second")
        {
            sleep(10)
            properties([[$class: 'BuildDiscarderProperty', strategy: 	
                     [$class: 'LogRotator', artifactDaysToKeepStr: '', 	
                      artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
            //error "f"
            
        }
    }
    catch(e)
    {}
    finally
    {
        stage('th')
        {
            sleep(1)
            node("master")
            {
                //step([$class: 'GitHubCommitStatusSetter', commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: '901a3633fedc2be3f4fb73aadbc88e5265618919'], contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'th'], reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'https://github.com/luxteam/branch_detect_test'], statusBackrefSource: [$class: 'ManuallyEnteredBackrefSource', backref: 'https://github.com/'], statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'BetterThanOrEqualBuildResult', message: 'Some tests failed. You need to validate it manual.', result: 'FAILURE', state: 'FAILURE']]]])
                //step([$class: 'GitHubCommitStatusSetter', contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'ci/manual'], statusBackrefSource: [$class: 'ManuallyEnteredBackrefSource', backref: 'https://github.com/'], statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'BetterThanOrEqualBuildResult', message: 'Some tests failed. You need to validate it manual.', result: 'FAILURE', state: 'FAILURE']]]])
            }
        }
    }   
}
