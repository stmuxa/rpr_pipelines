def call(String projectBranch = "") {
  node("ANDREY_A") {
    stage('PreBuild') {
      ws("WS/Branch_Prebuild") {
        echo "Prebuld"
        echo "=============="
        for(e in env){
          echo e + " is " + ${e}
        }
        echo "${BRANCH_NAME}"
        build = false
        checkOutBranchOrScm(projectBranch, 'https://github.com/luxteam/branch_detect_test.git')
        
        AUTHOR_NAME = bat (
                script: "git show -s --format=%%an HEAD ",
                returnStdout: true
                ).split('\r\n')[2].trim()

        echo "The last commit was written by ${AUTHOR_NAME}."
        if("${BRANCH_NAME}" == "master" && "${AUTHOR_NAME}" != "radeonprorender"){
          echo "master from ${AUTHOR_NAME}"
        } else {
          //def commitHash = checkout(scm).GIT_COMMIT
          //checkout(scm).each { name, value -> println "Name: $name -> Value $value" }
          echo "${BRANCH_NAME} isn't master branch. Parsing commit message..."
           
          if (BRANCH_NAME.matches("^PR-(\\d*)")) {
            echo "detected as PR"
          }
          
          commitMessage = bat ( script: "git log --format=%%B -n 1", returnStdout: true )

          commitSecond = bat ( script: "git log --format=%%B -n 1", returnStdout: true ).split('\r\n')[2].trim()

          echo "trim: ${commitSecond}"
          echo "Commit message: ${commitMessage}"
          println commitMessage.getClass()

          if (commitMessage.matches("(.*)CIS:BUILD(.*)")){
            build = true
            echo "found CIS:BUILD"
          }

          if (commitSecond.contains("CIS:BUILD")){
            build = true
            echo "found CIS:BUILD"
          }
        }
       }
     }
    stage('Build') {
      echo "Build"
      echo "=============="
      if(build) {
        ws("WS/Test"){
          echo "true"
        }
      }
    }
    stage('Deploy') {
      echo "Deploy"
      /*checkout([$class: 'GitSCM',
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
        echo "${BRANCH_NAME} is master branch. build it by sha: ${commitHashN}"*/ 
    }
  }
}
