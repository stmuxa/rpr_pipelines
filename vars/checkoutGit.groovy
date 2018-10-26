def call(String branchName, String repoName) {	
        
        try_git = 0	
        status = "error"	
        while (status == "error" && try_git < 10) {	
            try_git += 1	
            echo "TRY: ${try_git}"	
            status = checkout_git(branchName, repoName)	
            echo "STATUS: ${status}"	
            if (status =="error") {	
                sleep(5 * try_git)	
            }	
        }	
        if (status == "error" && try_git == 10) {
                currentBuild.result = 'ABORTED'
                error('Failed to connect github')
        }
            	
    	
}	
 def checkout_git(String branchName, String repoName) {	
    if(branchName != "")	
    {	
        try {	
            echo "checkout from user branch: ${branchName}; repo: ${repoName}"	
            checkout([$class: 'GitSCM', branches: [[name: "${branchName}"]], doGenerateSubmoduleConfigurations: false, extensions: [  	
                [$class: 'PruneStaleBranch'],
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
    else	
    {   	
        try {	
        echo 'checkout from scm options'	
        checkout scm	
            } catch (e) {	
                print (e)	
                return "error"	
         }	
    }	
}
