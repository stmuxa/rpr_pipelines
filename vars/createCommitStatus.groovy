def call(String sha, String context, String repository, String backref, String message, String status)
{
    step([$class: 'GitHubCommitStatusSetter',
        commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: "${sha}"],
        contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: "${context}"],
        reposSource: [$class: 'ManuallyEnteredRepositorySource', url: "${repository}"],
        statusBackrefSource: [$class: 'ManuallyEnteredBackrefSource', backref: "${backref}"],
        statusResultSource: [$class: 'ConditionalStatusResultSource',
        					results: [[$class: 'AnyBuildResult', message: "${message}", state: "${status}"]]
        					]
        ])
}
