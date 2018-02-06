def call(String path, String prefix)
{
    String version_read_script = 
"""
	file = ${path}
	prefix = ${prefix}

	old_version = []

	with open(file, encoding="utf8") as f:
		for line in f:
			if line.find(prefix) != -1:
				prefix_line = line

	try:

		old_version = re.findall(r'\\d+.\\d+.\\d+.\\d+', prefix_line)

		if len(old_version) == 0:
			old_version = re.findall(r'\\d+.\\d+.\\d+', prefix_line)
			if len(old_version) == 0:
				old_version = re.findall(r'\\d+.\\d+', prefix_line)
				if len(old_version) == 0: 
					old_version = re.findall(r'\\d+', prefix_line)

		if len(old_version) == 0:
			print('Unsupported version. No numbers in prefix line.')
		else:	
			print(old_version[0])
				
	except UnboundLocalError:
		print('Error. No search string.')
		exit(0)
        
"""
	ver = python3(version_read_script).split('\r\n')[4].trim()
    
    
    return ver
}
