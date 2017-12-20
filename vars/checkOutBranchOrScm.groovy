
def call(String branchName, String repoName) {
    if(branchName != "")
    {
        echo "checkout from user branch: ${branchName}; repo: ${repoName}"
        checkout([$class: 'GitSCM', branches: [[name: "*/${branchName}"]], doGenerateSubmoduleConfigurations: false, extensions: [

            checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: []])

            [$class: 'CleanCheckout'],
            [$class: 'CheckoutOption', timeout: 30],
            [$class: 'CloneOption', timeout: 30, noTags: false]
            ], submoduleCfg: [], userRemoteConfigs: [[url: "${repoName}", credentialsId: '37792118-83c9-474d-8549-e0b1ab07e442']]])
    }
    else
    {
        echo 'checkout from scm options'
        checkout scm
    }
}
