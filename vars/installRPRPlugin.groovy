def call(String osName, Map options, String tool, String logs, boolean clear=true, String matlib='')
{
    switch(osName)
    {
        case 'Windows':
            echo 'uninstall plugin'
            uninstallMSI("Radeon%${tool}%", logs)
            echo 'install plugin'
            installWin(options.pluginWinSha, tool, logs, clear)
            if (matlib){
                echo 'reinstall matlib'
                uninstallMSI("Radeon%Material%", logs)
                installMSI(matlib, logs)
            }
            break
        case 'OSX':
            // TODO: make implicit plugin deletion
            // TODO: implement matlib install
            echo 'install plugin'
            installOSX(options.pluginOSXSha, tool, logs, clear)
            break
        default:
            // TODO: make implicit plugin deletion
            echo 'install plugin'
            installLinux(options.pluginUbuntuSha, tool, logs, clear, osName, '2.82')
            if (matlib){
                echo 'reinstall matlib'
                installMatLibLinux(matlib, logs)
            }
    }
}


def installWin(String pluginSha, String tool, String logs, boolean clear)
{
    if (clear){
        clearBinariesWin()
        checkExistWin(pluginSha, tool, logs)
    }
    installMSI("${pluginSha}.msi")

    // duct tape for plugin registration
    // FIXME: blender version hardcode
    if(tool == 'Blender'){
        try
        {
            bat"""
            echo "----------DUCT TAPE. Try adding addon from blender" >> ${logs}.install.log
            """

            bat """
            echo import bpy >> registerRPRinBlender.py
            echo import os >> registerRPRinBlender.py
            echo addon_path = "C:\\Program Files\\AMD\\RadeonProRenderPlugins\\Blender\\\\addon.zip" >> registerRPRinBlender.py
            echo bpy.ops.preferences.addon_install(filepath=addon_path) >> registerRPRinBlender.py
            echo bpy.ops.preferences.addon_enable(module="rprblender") >> registerRPRinBlender.py
            echo bpy.ops.wm.save_userpref() >> registerRPRinBlender.py

            "C:\\Program Files\\Blender Foundation\\Blender 2.82\\blender.exe" -b -P registerRPRinBlender.py >> ${logs}.install.log 2>&1
            """
        }
        catch(e)
        {
            echo "Error during rpr register"
            println(e.toString())
            println(e.getMessage())
        }
    }
}


def checkExistWin(String pluginSha, String tool, String logs){
    if(!(fileExists("${CIS_TOOLS}/../PluginsBinaries/${pluginSha}.msi")))
    {
        bat """
        IF NOT EXIST "${CIS_TOOLS}\\..\\PluginsBinaries" mkdir "${CIS_TOOLS}\\..\\PluginsBinaries"
        rename RadeonProRender${tool}.msi ${pluginSha}.msi
        copy ${pluginSha}.msi "${CIS_TOOLS}\\..\\PluginsBinaries\\${pluginSha}.msi"
        """
    }
    else
    {
        bat """
        copy "${CIS_TOOLS}\\..\\PluginsBinaries\\${pluginSha}.msi" ${pluginSha}.msi
        """
    }
}


def installMatLibLinux(String msiName, String logs)
{
    receiveFiles("bin_storage/RadeonProRenderMaterialLibraryInstaller_2.0.run", "${CIS_TOOLS}/../TestResources/")

    sh """
    #!/bin/bash
    ${CIS_TOOLS}/../TestResources/RadeonProRenderMaterialLibraryInstaller_2.0.run --nox11 -- --just-do-it >> ${logs}.matlib.install.log 2>&1
    """
}


def installOSX(String pluginSha, String tool, String logs, boolean clear)
{
    if (clear){
        clearBinariesUnix()
        checkExistOSX(pluginSha, tool)
    }

    sh"""
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
        echo "Error while deinstall plugin"
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