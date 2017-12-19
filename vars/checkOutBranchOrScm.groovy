
def call(String branchName, String repoName) {
    if(branchName != "")
    {
        echo "checkout from user branch ${branchName}"
        checkout([$class: 'GitSCM', branches: [[name: "*/${branchName}"]], doGenerateSubmoduleConfigurations: false, extensions: [
            [$class: 'CleanCheckout'],
            [$class: 'CheckoutOption', timeout: 30],
            [$class: 'CloneOption', timeout: 30]
            ], submoduleCfg: [], userRemoteConfigs: [[url: "https://github.com/Radeon-Pro/${repoName}.git"]]])
    }
    else
    {
        echo 'checkout from scm options'
        checkout scm
    }
}
