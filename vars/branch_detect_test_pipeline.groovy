def call()
{
    stage("init")
    {
        echo "start"
        properties([parameters([string(defaultValue: 'PR', description: 'Test suite', name: 'Tests', trim: false)]), [$class: 'JobPropertyImpl', throttle: [count: 6, durationName: 'hour', userBoost: true]]])
    }
    stage("second")
    {
        sleep(10)
    }
}
