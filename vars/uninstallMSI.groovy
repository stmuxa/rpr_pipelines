def call(String name, String logs="") //for example: "Blender%' and Version ='2.80.0"
{
    logs = logs ?: "${STAGE_NAME}"
    try{
        powershell"""
            \$uninstall = Get-WmiObject -Class Win32_Product -Filter "Name LIKE '${name}'"
            if (\$uninstall) {
                Write "Uninstalling..."
                \$uninstall = \$uninstall.IdentifyingNumber
                start-process "msiexec.exe" -arg "/X \$uninstall /qn /quiet /L+ie ${logs}.uninstall.log /norestart" -Wait
            }else{
                Write "Plugin not found"
            }
        """
    }
    catch(e)
    {
        println("Error while uninstall plugin ${name}")
        println(e.toString())
        println(e.getMessage())
    }
}
