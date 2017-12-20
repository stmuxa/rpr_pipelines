
def call(String branchName, String repoName) {
    if(branchName != "")
    {
        echo "checkout from user branch: ${branchName}; repo: ${repoName}"
        checkout([$class: 'GitSCM', branches: [[name: "*/${branchName}"]], doGenerateSubmoduleConfigurations: false, extensions: [
            [$class: 'CleanCheckout'],
            [$class: 'CheckoutOption', timeout: 30],
            [$class: 'CloneOption', timeout: 30, noTags: false]
            ], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '37792118-83c9-474d-8549-e0b1ab07e442', url: "${repoName}"]]])
    }
    else
    {
        echo 'checkout from scm options'
        checkout scm
    }
}
