def call()
{
    stage("init")
    {
        echo "start"
        properties([parameters([string(defaultValue: 'PR', description: 'Test suite', name: 'Tests', trim: false)]), [$class: 'JobPropertyImpl', throttle: [count: 6, durationName: 'hour', userBoost: true]]])
    }
    stage("second")
    {
        error 'stage failed'
        sleep(10)
                properties([[$class: 'BuildDiscarderProperty', strategy: 	
                         [$class: 'LogRotator', artifactDaysToKeepStr: '', 	
                          artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
    }
}
