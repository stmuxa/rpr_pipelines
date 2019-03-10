def call() {
    stage("build") {
        echo "build"
        if (env.CHANGE_ID) {
            
            for (commit in pullRequest.commits) {
              for (status  in commit.statuses) {
                 echo "Commit: ${commit.sha}, State: ${status.state}, Context: ${status.context}, URL: ${status.targetUrl}"
              }
            }
            
            echo "----statuses"
            
            for (status in pullRequest.statuses) {
                echo "Commit: ${pullRequest.head}, State: ${status.state}, Context: ${status.context}, URL: ${status.targetUrl}"
            }
            
            pullRequest.addLabel("Build Success")
            pullRequest.createStatus("success", "context", "description", "https://rpr.cis.luxoft.com/targetUrl")
        }
    }

    stage("test") {
        echo "test"
    }

    stage("deploy") {
        echo "deploy"
    }
}
