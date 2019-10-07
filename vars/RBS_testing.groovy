import RBS


def call() {
	rbs = new RBS(this, "Blender28", env.JOB_NAME, env)
	println(rbs.buildName)	
}
