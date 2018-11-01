def call()
{
    stage("init")
    {
        echo "start"
        properties([[$class: 'JobPropertyImpl', throttle: [count: 3, durationName: 'hour', userBoost: true]]])
    }
    stage("second")
    {
        sleep(10)
    }
}
