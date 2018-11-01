def call()
{
    stage("init")
    {
        echo "start"
        properties([[$class: 'JobPropertyImpl', throttle: [count: 1, durationName: 'hour', userBoost: true]]])
    }
    stage("second")
    {
        sleep(10)
    }
}
