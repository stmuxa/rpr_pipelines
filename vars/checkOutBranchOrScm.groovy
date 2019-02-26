
def call(String branchName, String repoName, Boolean polling=false, Boolean changelog=false, String SCMname="") {
    polling = polling ?: true
    changelog = changelog ?: true

    // TODO: implement retray - WipeWorkspace
    if(branchName != "")
    {
        echo "checkout from user branch: ${branchName}; repo: ${repoName}"
        checkout changelog: changelog, poll: polling, csm: 
        [$class: 'GitSCM', branches: [[name: "${branchName}"]], doGenerateSubmoduleConfigurations: false,
            extensions: [
                [$class: 'ScmName', name: "${SCMname}"],
                [$class: 'PruneStaleBranch'],
                [$class: 'CleanBeforeCheckout'],
                [$class: 'CleanCheckout'],
                [$class: 'CheckoutOption', timeout: 30],
                [$class: 'CloneOption', timeout: 30, noTags: false],
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
