def call() {
  node("ANDREY_A") {
    stage('PreBuild') {
      echo "Prebuld"
      echo "${BRANCH_NAME}"
    }
    stage('Build') {
      echo "Build"
    }
    stage('Deploy') {
      echo "Deploy"
    }
  }
}
