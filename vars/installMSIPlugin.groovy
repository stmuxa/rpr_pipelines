def call(String osName, Map options, String tool, String logs, boolean clear=true, String matlib='')
{
    switch(osName)
    {
        case 'Windows':
            if (checkExistenceOfPlugin(options.pluginWinSha, tool, logs, clear)) {
                  println '[INFO] Current plugin is already installed.'
                  return false
            } else {
                println '[INFO] Uninstalling plugin'
                uninstallMSI("Radeon%${tool}%", logs)
                println '[INFO] Installing plugin'
                installMSI("${options.pluginWinSha}.msi")
                if (matlib){
                    echo '[INFO] Reinstalling Material Library'
                    uninstallMSI("Radeon%Material%", logs)
                    installMSI(matlib, logs)
                }
                return true
            }

        case 'OSX':
            // TODO: make implicit plugin deletion
            // TODO: implement matlib install
            println '[INFO] Installing plugin'
            installOSX(options.pluginOSXSha, tool, logs, clear)
            return true

        default:
            // TODO: make implicit plugin deletion
            println '[INFO] Installing plugin'
            installLinux(options.pluginUbuntuSha, tool, logs, clear, osName, '2.82')
            if (matlib){
                println '[INFO] Reinstalling Material Library'
                installMatLibLinux(matlib, logs)
            }
            return true
    }
}
    

def checkExistenceOfPlugin(String pluginSha, String tool, String logs, boolean clear) 
{
    if (clear) {
        clearBinariesWin()
        checkExistWin(pluginSha, tool, logs)
    }

    println "[INFO] Checking existence of the RPR plugin on test PC."
    println "[INFO] MSI name: ${pluginSha}.msi"
    
    // Finding installed plugin on test PC
    String installedProductCode =  powershell(
            script: """
            (Get-WmiObject -Class Win32_Product -Filter \"Name LIKE 'Radeon%${tool}%'\").IdentifyingNumber
            """, returnStdout: true).trim()

    println "[INFO] Installed MSI product code: ${installedProductCode}"

    // Reading built msi file
    bat """

        echo import msilib >> getMsiProductCode.py
        echo db = msilib.OpenDatabase(r'${pluginSha}.msi', msilib.MSIDBOPEN_READONLY) >> getMsiProductCode.py
        echo view = db.OpenView("SELECT Value FROM Property WHERE Property='ProductCode'") >> getMsiProductCode.py
        echo view.Execute(None) >> getMsiProductCode.py
        echo print(view.Fetch().GetString(1)) >> getMsiProductCode.py

    """

    String msiProductCode = python3("getMsiProductCode.py").split('\r\n')[2].trim()

    println "[INFO] Built MSI product code: ${msiProductCode}"

    return installedProductCode==msiProductCode
}


def checkExistWin(String pluginSha, String tool, String logs){
    if(!(fileExists("${CIS_TOOLS}/../PluginsBinaries/${pluginSha}.msi")))
    {
        println "[INFO] The plugin does not exist in the storage."
        bat """
            IF NOT EXIST "${CIS_TOOLS}\\..\\PluginsBinaries" mkdir "${CIS_TOOLS}\\..\\PluginsBinaries"
            rename RadeonProRender${tool}.msi ${pluginSha}.msi
            copy ${pluginSha}.msi "${CIS_TOOLS}\\..\\PluginsBinaries\\${pluginSha}.msi"
        """
    }
    else
    {
        println "[INFO] The plugin exists in the storage."
        bat """
            copy "${CIS_TOOLS}\\..\\PluginsBinaries\\${pluginSha}.msi" ${pluginSha}.msi
        """
    }
}


def installOSX(String pluginSha, String tool, String logs, boolean clear)
{
    if (clear){
        clearBinariesUnix()
        checkExistOSX(pluginSha, tool)
    }

    sh """
        $CIS_TOOLS/install${tool}Plugin.sh ${CIS_TOOLS}/../PluginsBinaries/${pluginSha}.dmg >> ${logs}.install.log 2>&1
    """
}


def checkExistOSX(String pluginSha, String tool){
    if(!(fileExists("${CIS_TOOLS}/../PluginsBinaries/${pluginSha}.dmg")))
    {
        sh """
            mkdir -p "${CIS_TOOLS}/../PluginsBinaries"
            mv RadeonProRender${tool}.dmg "${CIS_TOOLS}/../PluginsBinaries/${pluginSha}.dmg"
        """
    }
}


def installLinux(String pluginSha, String tool, String logs, boolean clear, String osName, String blenderVersion)
{
    // remove installed plugin
    try
    {
        sh"""
            /home/user/.local/share/rprblender/uninstall.py /home/user/Desktop/Blender${blenderVersion}/ >> ${logs}.uninstall.log 2>&1
        """
    }

    catch(e)
    {
        echo "[ERROR] Failed to deinstall plugin"
        println(e.toString())
        println(e.getMessage())
    }

    if (clear){
        clearBinariesUnix()
        checkExistUnix(pluginSha, osName)
    }

    // install plugin
    sh """
        #!/bin/bash
        printf "y\nq\n\ny\ny\n" > input.txt
        exec 0<input.txt
        exec &>${logs}.install.log
        ${CIS_TOOLS}/../PluginsBinaries/${pluginSha}.run --nox11 --noprogress ~/Desktop/Blender${blenderVersion} >> ${logs}.install.log 2>&1
    """
}


def checkExistUnix(String pluginSha, String osName)
{
    if(!(fileExists("${CIS_TOOLS}/../PluginsBinaries/${pluginSha}.run")))
    {
        sh """
            mkdir -p "${CIS_TOOLS}/../PluginsBinaries"
            chmod +x RadeonProRenderBlender.run
            mv RadeonProRenderBlender.run "${CIS_TOOLS}/../PluginsBinaries/${pluginSha}.run"
        """
    }
}