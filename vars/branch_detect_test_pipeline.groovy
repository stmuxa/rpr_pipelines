def call()
{
    stage("init")
    {
        echo "start"
    }
    stage("second")
    {
        sleep(10)
    }
}
