def call() {
  node("ANDREY_A") {
    stage('PreBuild') {
      echo "Prebuld"
      echo "${BRANCH_NAME}"
      outputEnvironmentInfo("Windows")
      
      checkout([$class: 'GitSCM',
                userRemoteConfigs: [[url: 'https://github.com/luxteam/branch_detect_test.git']]])
      
      bat"""
      echo %CD%
      set
      """
    }
    stage('Build') {
      echo "Build"
      def scmVars = checkout scm
      scmVars.each { key, value
        echo "${key} : ${value}"
        println "${key} : ${value}"
      }
    }
    stage('Deploy') {
      echo "Deploy"
    }
  }
}
