
def call(String branchName, String repoName, Boolean polling=false, Boolean changelog=false) {
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
                [$class: 'CloneOption', timeout: 30, noTags: false],
                [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, timeout: 30, reference: '', trackingSubmodules: false]
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
