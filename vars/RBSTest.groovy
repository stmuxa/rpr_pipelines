import RBS

def call() {
    node("Windows") {
        rbs = new RBS(this, "Blender28", env.JOB_NAME, env)
    	println(rbs.buildName)
        Map options = [
            JOB_STARTED_TIME: rbs.getTime(),
            testsList: ["Smoke"],
            gpusCount: 2
        ]
        rbs.startBuild(options)
        println(options.JOB_STARTED_TIME)  
        println(rbs.buildID)
    }
}
