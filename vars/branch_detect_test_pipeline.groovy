def call() {
  node("ANDREY_A") {
    stage('PreBuild') {
      echo "Prebuld"
      echo "=============="
      if("${BRANCH_NAME}" == "master" ){
        def commitHash = checkout(scm).GIT_COMMIT
        echo "${BRANCH_NAME} is master branch. build it by sha: ${commitHash}"
        
        AUTHOR_NAME = bat (
                            script: "git show -s --format='%%an' HEAD ",
                            returnStdout: true
                            ).split('\r\n')[2].trim()
        echo "Commit author: ${AUTHOR_NAME}"
        
        if("${AUTHOR_NAME}" != "radeonprorender") {
           echo "Incrementing..."
           //TODO: increment
         } else {
           echo "Last author ${AUTHOR_NAME} - not incremen"
         }    
        
      } else {
        def commitHash = checkout(scm).GIT_COMMIT
        //def commitMessage = checkout(scm).GIT_COMMIT
        echo "${BRANCH_NAME} isn't master branch. Parsing commit message..."
      }
      /*checkout([$class: 'GitSCM',
                userRemoteConfigs: [[url: 'https://github.com/luxteam/branch_detect_test.git']]])
      */
    }
    stage('Build') {
      echo "Build"
      echo "=============="
      env.getEnvironment().each { name, value -> println "Name: $name -> Value $value" }
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
