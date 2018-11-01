def call()
{
    stage("init")
    {
        echo "start"
        properties([pipelineTriggers([<object of type org.jenkinsci.plugins.ghprb.GhprbTrigger>])])
    }
    stage("second")
    {
        sleep(10)
    }
}
