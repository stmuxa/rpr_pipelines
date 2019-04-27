def executeAnalysis(pcType, osName, Map options) {
    currentBuild.result = 'SUCCESS'
    
    echo "${options}"
    
    timeout(time: 1, unit: 'HOURS') {
    switch(osName) {
	case 'Windows10':
	    try {

		print("Clean up work folder")
		bat '''
			@echo off
			del /q *
			for /d %%x in (*) do @rd /s /q "%%x"
		''' 
			    	
		python3("..\\..\\scripts\\send_status.py --django_ip \"${options.django_url}/\" --status \"Downloading file\" --id ${id}")
		bat """ 
		    wget --no-check-certificate "${options.Scene}"
		"""

		if (options['File'].endsWith('.zip') || options['File'].endsWith('.7z')) {
			bat """
				"..\\..\\cis_tools\\7-Zip\\7z.exe" x \"${options.File}\"
		        """
		}
		
		echo "Launching analysis"
		python3("..\\..\\scripts\\send_status.py --django_ip \"${options.django_url}/\" --status \"Analysing file\" --id ${id}")
		bat """
		    mkdir "Output"
		    ..\\..\\scripts\\procmon.exe /AcceptEula /Quiet /Minimized /BackingFile "Output\\log.pml"
		"""
		python3("..\\..\\scripts\\launch_analysis.py")
		bat """
		    ..\\..\\scripts\\procmon.exe /Terminate
		"""
		bat """
		    ..\\..\\scripts\\procmon.exe /OpenLog "Output\\log.pml" /SaveAs "Output\\log.csv"
		"""
		echo "Preparing results"
		python3("..\\..\\cis_tools\\${options.cis_tools}\\send_status.py --django_ip \"${options.django_url}/\" --status \"Completed\" --id ${id}")
 
	    } catch(e) {
			currentBuild.result = 'FAILURE'
			print e
			echo "Error while analyse"
	    } finally {
		archiveArtifacts 'Output/*'
		String post = python3("..\\..\\cis_tools\\${options.cis_tools}\\send_post.py --django_ip \"${options.django_url}/\" --build_number ${currentBuild.number} --jenkins_job \"${options.jenkins_job}\" --status ${currentBuild.result} --id ${id}")
		print post
	    }
	  break;

	}
}
}

def main(String pcType, String os, Map options) {

	timestamps {
	    String PRJ_PATH="${options.PRJ_ROOT}/${options.PRJ_NAME}"
	    String JOB_PATH="${PRJ_PATH}/${JOB_NAME}/Build-${BUILD_ID}".replace('%2F', '_')
	    options['PRJ_PATH']="${PRJ_PATH}"
	    options['JOB_PATH']="${JOB_PATH}"

	    boolean PRODUCTION = true

	    if (PRODUCTION) {
		options['django_url'] = "https://analyse.cis.luxoft.com/jenkins_post_form/"
		options['jenkins_job'] = "AnalysisMalwareJob"
	    } else {
		options['django_url'] = "https://analyse.cis.luxoft.com/jenkins_post_form/"
		options['jenkins_job'] = "AnalysisMalwareJob"
	    }


	    try {

		    Map newOptions = options.clone()

		    echo "Scheduling Analysis ${pcType}:${os}"
			node("${pcType} && ${os}")
			{
			    stage("Analysis-${pcType}-${os}")
			    {
				timeout(time: 1, unit: 'HOURS')
				{
				    ws("WS/${newOptions.PRJ_NAME}_Analysis") {
					executeAnalysis(pcType, os, newOptions)
				    }
				}
			    }
			}
			}   catch (e) {
				println(e.toString());
				println(e.getMessage());
				println(e.getStackTrace());
				currentBuild.result = "FAILED"
				throw e
	    }

	}
}    
       
    
    
def call(
    String File = '',
    String pcType = '',
    String OS = '',
    String id = '',
    String run_time = ''
    ) {
	String PRJ_ROOT='Analysis_File'
	String PRJ_NAME='Analysis_File'  
	main(pcType, OS, [
	    enableNotifications:false,
	    PRJ_NAME:PRJ_NAME,
	    PRJ_ROOT:PRJ_ROOT,
	    File:File,
	    id:id,
            run_time:run_time
	   ])
    }
