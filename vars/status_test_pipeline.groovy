def call() {
    stage("build") {
        echo "build"
    }

    stage("test") {
        echo "test"
    }

    stage("deploy") {
        echo "deploy"
    }
}
