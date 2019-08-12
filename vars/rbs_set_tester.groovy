def call(Map options) {
	try {
		String tests = ""
		String testsPackage = ""
		
		if (options.tests != "") {
			tests = """--tests ${options.tests}"""
		}

		if (options.testsPackage != "") {
			testsPackage = """--tests_package ${options.testsPackage}"""
		}

		// high priority custom tests - not testsPackage
        python3("""jobs_launcher/rbs.py --tool ${options.TESTER_TAG} --branch ${getBranchTag(env.JOB_NAME)} --build ${env.BUILD_NUMBER} ${tests} ${testsPackage} --login ${env.RBS_LOGIN} --password ${env.RBS_PASSWORD}""")
    }
    catch (e) {
        println(e)
    }
}