def call() {
    stage("build") {
        echo "build"
        echo "${env}"
        
        if (env.CHANGE_ID) {
            
            /*for (commit in pullRequest.commits) {
              for (status  in commit.statuses) {
                 echo "Commit: ${commit.sha}, State: ${status.state}, Context: ${status.context}, URL: ${status.targetUrl}"
                  
              }
            }*/
            
            echo "----statuses"
            
            for (status in pullRequest.statuses) {
                echo "Commit: ${pullRequest.head}, State: ${status.state}, Context: ${status.context}, URL: ${status.targetUrl}"
            }
            
            /*pullRequest.addLabel("Build Success")
            pullRequest.createStatus("success", "context", "description", "https://rpr.cis.luxoft.com/targetUrl")*/
        }
        else {
            /*node("master") {
            //commit.createStatus("success", "context", "description", "https://rpr.cis.luxoft.com/targetUrl")
            checkOutBranchOrScm("", "")
                step([$class: 'GitHubCommitStatusSetter',
                  commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: 'dfb2604ddf563b13265e3a2a4c9b66f1c9e7764c'],
                  contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: 'context'],
                  reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'https://github.com/luxteam/statustest'],
                  statusResultSource: [$class: 'ConditionalStatusResultSource',
                                       results: [[$class: 'AnyBuildResult', message: 'message', state: 'PENDING']]
                                      ]
                 ])
            
            }*/
        }
    }

    stage("test") {
        echo "test"
    }

    stage("deploy") {
        echo "deploy"
    }
}
