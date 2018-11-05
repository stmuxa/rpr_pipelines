def call()
{
    stage("init")
    {
        echo "start"
        error 'stage failed'
        properties([parameters([string(defaultValue: 'PR', description: 'Test suite', name: 'Tests', trim: false)]), [$class: 'JobPropertyImpl', throttle: [count: 6, durationName: 'hour', userBoost: true]]])
    }
    stage("second")
    {
        
        sleep(10)
                properties([[$class: 'BuildDiscarderProperty', strategy: 	
                         [$class: 'LogRotator', artifactDaysToKeepStr: '', 	
                          artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10']]]);
    }
}
