def call() {
  node("ANDREY_A") {
    stage('PreBuild') {
      echo "Prebuld"
      echo "=============="
      if("${BRANCH_NAME}" == "master" && "${AUTHOR_NAME}" != "radeonprorender"){
        def commitHash = checkout(scm).GIT_COMMIT
        echo "${BRANCH_NAME} is master branch. build it by sha: ${commitHash}"
        
        AUTHOR_NAME = bat ( 
                            script: "git show -s --format='%%an' HEAD ",
                            returnStdout: true
                            ).split('\r\n')[2].trim()
        echo "Commit author: ${AUTHOR_NAME}"
        echo "Incrementing..."
        //TODO: make push    
      } else {
        //def commitHash = checkout(scm).GIT_COMMIT
        checkout(scm).each { name, value -> println "Name: $name -> Value $value" }
        echo "${BRANCH_NAME} isn't master branch. Parsing commit message..."
      }
      /*checkout([$class: 'GitSCM',
                userRemoteConfigs: [[url: 'https://github.com/luxteam/branch_detect_test.git']]])
      */
    }
    stage('Build') {
      echo "Build"
      echo "=============="
      //env.getEnvironment().each { name, value -> println "Name: $name -> Value $value" }

    }
    stage('Deploy') {
      echo "Deploy"
    }
  }
}
