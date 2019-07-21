
def call(String branchName, String repoName, Boolean polling=null, Boolean changelog=true, String scmName=null) {
    polling = polling ?: false
    changelog = changelog ?: false
    scmName = scmName ?: "git"

    // TODO: implement retray - WipeWorkspace
    if(branchName != "")
    {
        echo "checkout from user branch: ${branchName}; repo: ${repoName}"
        checkout changelog: changelog, poll: polling, scm: 
        [$class: 'GitSCM', branches: [[name: "${branchName}"]], doGenerateSubmoduleConfigurations: false,
            extensions: [
                [$class: 'PruneStaleBranch'],
                [$class: 'CleanBeforeCheckout'],
                [$class: 'ScmName', name: ''],
                [$class: 'CleanCheckout'],
                [$class: 'CheckoutOption', timeout: 30],
                [$class: 'CloneOption', timeout: 30, noTags: false, shallow: true],
                [$class: 'IgnoreNotifyCommit'],
                [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: '', trackingSubmodules: false]
            ],
            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: 'radeonprorender', url: "${repoName}"]]
        ]
    }
    else
    {
        echo 'checkout from scm options'
        checkout scm
    }
}
