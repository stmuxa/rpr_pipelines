def call() {
  node("ANDREY_A") {
    stage('PreBuild') {
      echo "Prebuld"
      echo "=============="
      if("${BRANCH_NAME}" == "master"){
        def commitHash = checkout(scm).GIT_COMMIT
        echo "${BRANCH_NAME} is master branch. build it by sha: "${commitHash}"
      } else {
        /*def commitHash = checkout(scm).GIT_COMMIT
        def commitMessage = checkout(scm).GIT_COMMIT
        echo "${BRANCH_NAME} */
      }
      /*checkout([$class: 'GitSCM',
                userRemoteConfigs: [[url: 'https://github.com/luxteam/branch_detect_test.git']]])
      */
    }
    stage('Build') {
      
      //echo "Build"
      /*def scmVars = checkout scm
      echo scmVars.getClass()
      */
      //def commitHash = checkout(scm).GIT_COMMIT
      //echo "${commitHash}"
      /*scmVars.each { key, value
        echo "${key} : ${value}"
        println "${key} : ${value}"
      }*/
    }
    stage('Deploy') {
      echo "Deploy"
    }
  }
}
