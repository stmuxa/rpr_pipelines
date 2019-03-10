def call() {
    stage("build") {
        echo "build"
        for (commit in pullRequest.commits) {
          for (status  in commit.statuses) {
             echo "Commit: ${commit.sha}, State: ${status.state}, Context: ${status.context}, URL: ${status.targetUrl}"
          }
        }
    }

    stage("test") {
        echo "test"
    }

    stage("deploy") {
        echo "deploy"
    }
}
