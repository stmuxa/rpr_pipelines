def call()
{
    stage("build")
    {        
        if (env.CHANGE_ID)
        {
            for (status in pullRequest.statuses)
            {
                echo "Commit: ${pullRequest.head}, State: ${status.state}, Context: ${status.context}, URL: ${status.targetUrl}"
                pullRequest.createStatus("pending", "${status.context}", "Replay", "https://rpr.cis.luxoft.com/targetUrl")
            }
            
            /*pullRequest.addLabel("Build Success")
            pullRequest.createStatus("success", "context", "description", "https://rpr.cis.luxoft.com/targetUrl")*/
        }
    }

    stage("test")
    {
        echo "test"
    }

    stage("deploy")
    {
        echo "deploy"
    }
}
