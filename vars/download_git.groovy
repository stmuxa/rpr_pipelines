def call(String branchName, String repoName) {
    if(branchName != "")
    {
        echo "checkout from user branch: ${branchName}; repo: ${repoName}"
        status = checkout_git(branchName, repoName)
        echo "STATUS:"
        echo status
        try_git = 1
        while (status == "error" and try_git < 4) {
            try_git += 1
            status = checkout_git(branchName, repoName)
        }
            
    }
    else
    {
        echo 'checkout from scm options'
        checkout scm
    }
}

def checkout_git(String branchName, String repoName) {
    
    try {
        checkout([$class: 'GitSCM', branches: [[name: "${branchName}"]], doGenerateSubmoduleConfigurations: false, extensions: [  
            [$class: 'CleanBeforeCheckout'],
            [$class: 'CleanCheckout'],
         //   [$class: 'WipeWorkspace'],
            [$class: 'CheckoutOption', timeout: 30],
            [$class: 'CloneOption', timeout: 30, noTags: false],
            [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: '', trackingSubmodules: false]
            ], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'radeonprorender', url: "${repoName}"]]])
            return "success"
     } catch (e) {
            print (e)
            return "error"
     }
         
}
