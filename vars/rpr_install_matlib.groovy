def call(String osName) {
    switch(osName) {
        case 'Windows':
        // TODO: implement install/uninstall logs saving
            try {
                receiveFiles("/bin_storage/RadeonProMaterialLibrary.msi", "/mnt/c/TestResources/")

                try {
                    // TODO: call uninstall only if installed matlib has been detected
                    powershell"""
                    \$uninstall = Get-WmiObject -Class Win32_Product -Filter "Name = 'Radeon ProRender Material Library'"
                    if (\$uninstall) {
                    Write "Uninstalling..."
                    \$uninstall = \$uninstall.IdentifyingNumber
                    start-process "msiexec.exe" -arg "/X \$uninstall /qn /quiet /L+ie ${STAGE_NAME}.matlib.uninstall.log /norestart" -Wait
                    }else{
                    Write "Plugin not found"}
                    """
                } catch(e) {
                    println("Error while deinstall plugin")
                    println(e.toString())
                }

                bat """
                msiexec /i "C:\\TestResources\\RadeonProMaterialLibrary.msi" /quiet /L+ie ${STAGE_NAME}.matlib.install.log /norestart
                """
            } catch(e) {
                println(e.getMessage())
                println(e.toString())
            }
            break
        case 'OSX':
            // TODO: implement matlib installation
            echo "skip"
            break
        default:
            echo "skip"
            break
    }
}