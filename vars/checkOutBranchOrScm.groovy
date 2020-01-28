
def call(String branchName, String repoName, Boolean disableSubmodules=false, Boolean polling=false, Boolean changelog=false) {
    // TODO: fix processing
    polling = polling ?: true
    changelog = changelog ?: true

    // TODO: implement retray - WipeWorkspace
    if(branchName != "")
    {
        echo "checkout from user branch: ${branchName}; repo: ${repoName}"
        checkout changelog: changelog, poll: polling, scm:
        [$class: 'GitSCM', branches: [[name: "${branchName}"]], doGenerateSubmoduleConfigurations: false,
            extensions: [
                [$class: 'PruneStaleBranch'],
                [$class: 'CleanBeforeCheckout'],
                [$class: 'CleanCheckout'],
                [$class: 'CheckoutOption', timeout: 30],
                [$class: 'CloneOption', timeout: 60, noTags: false],
                [$class: 'SubmoduleOption', disableSubmodules: disableSubmodules, parentCredentials: true, recursiveSubmodules: true, timeout: 60, reference: '', trackingSubmodules: false]
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
