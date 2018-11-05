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
            error "f"
            
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
                step([$class: 'GitHubCommitStatusSetter', contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'ci/user'], statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: 'User message', state: 'SUCCESS']]]])
                //step([$class: 'GitHubCommitStatusSetter', contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'ci/manual'], statusBackrefSource: [$class: 'ManuallyEnteredBackrefSource', backref: 'https://github.com/'], statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'BetterThanOrEqualBuildResult', message: 'Some tests failed. You need to validate it manual.', result: 'FAILURE', state: 'FAILURE']]]])
            }
        }
    }   
}
