def call() {
  node("ANDREY_A") {
    /*stage('PreBuild') {
      echo "Prebuld"
      echo "=============="

      checkout([$class: 'GitSCM',
                userRemoteConfigs: [[url: 'https://github.com/luxteam/branch_detect_test.git']]])
      
      AUTHOR_NAME = bat ( script: "git show -s --format='%%an' HEAD ",
                          returnStdout: true
                          ).split('\r\n')[2].trim()

      build = false
      
      if("${BRANCH_NAME}" == "master" && "${AUTHOR_NAME}" != "radeonprorender"){
        def commitHash = checkout(scm).GIT_COMMIT
        echo "${BRANCH_NAME} is master branch. build it by sha: ${commitHash}"

        echo "Commit author: ${AUTHOR_NAME}"
        bat """
        del auto.code
        echo "${commitHash}" > auto.code
        git add auto.code
        git commit -m "buildmaster: version update to sha"
        git push origin HEAD:master
        """
        bat """
        del auto.code
        echo "trym" > auto.code
        git add auto.code
        git commit -m "buildmaster: version update to trym"
        git push origin HEAD:master
        """ 
        build = true
        
        commitHashN = bat ( script: "git log --format=%%H -1 ",
                           returnStdout: true).split('\r\n')[2].trim()
        echo "++++++++++++++++++++++"
        echo "${BRANCH_NAME} is master branch. build it by sha: ${commitHashN}"

        //TODO: make push    
      } else {
        //def commitHash = checkout(scm).GIT_COMMIT
        checkout(scm).each { name, value -> println "Name: $name -> Value $value" }
        echo "${BRANCH_NAME} isn't master branch. Parsing commit message..."
        
        commitMessage = bat ( script: "git log --format=%%B -n 1",
                              returnStdout: true )
        echo "Message: ${commitMessage}"
        
        if (commitMessage.contains("CIS:BUILD")){
          build = true
        }
      }
    }*/
    stage('Build') {
      echo "Build"
      String repoName = 'https://github.com/luxteam/branch_detect_test.git'
      
      echo "=============="
      if(true) {
        ws("WS/Test"){
          echo "Building...."
          def branchName = "master"
          branchName = "bacfe4b00542d031f9e52e6f9c4bbfdd0adc2c70"
          //echo "checkout from user branch: ${BRANCH_NAME}; repo: ${repoName}, commitId: 9cd800b6933f052a4d005984997cac43c9cbcb31"
          checkout([$class: 'GitSCM', branches: [[name: "${branchName}"]], doGenerateSubmoduleConfigurations: false, extensions: [
              [$class: 'CleanBeforeCheckout'],
              [$class: 'CleanCheckout'],
           //   [$class: 'WipeWorkspace'],
              [$class: 'CheckoutOption', timeout: 30],
              [$class: 'CloneOption', timeout: 30, noTags: false],
              [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: '', trackingSubmodules: false]
              ], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'radeonprorender', url: "${repoName}"]]])
        }
      }
      /*commitHashN = bat ( script: "git log --format=%%H -1 ",
                   returnStdout: true).split('\r\n')[2].trim()
      echo "++++++++++++++++++++++"
      echo "${BRANCH_NAME} is master branch. build it by sha: ${commitHashN}"
      //env.getEnvironment().each { name, value -> println "Name: $name -> Value $value" }
      */
    }
    stage('Deploy') {
      echo "Deploy"
    }
  }
}
