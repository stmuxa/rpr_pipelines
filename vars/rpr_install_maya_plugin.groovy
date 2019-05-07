def call(String osName, Map options, Boolean buildRenderCache) {
    switch(osName) {
        case 'Windows':
            // uninstall plugin
            try {
                powershell"""
                \$uninstall = Get-WmiObject -Class Win32_Product -Filter "Name = 'Radeon ProRender for Autodesk Maya®'"
                if (\$uninstall) {
                Write "Uninstalling..."
                \$uninstall = \$uninstall.IdentifyingNumber
                start-process "msiexec.exe" -arg "/X \$uninstall /qn /quiet /L+ie ${STAGE_NAME}.uninstall.log /norestart" -Wait
                }else{
                Write "Plugin not found"}
                """
            } catch(e) {
                println("Error while deinstall plugin")
                println(e.toString())
                println(e.getMessage())
            }

            // install new plugin
            // script use local binary storage for enhancement split execution performance
            dir('temp/install_plugin') {
                // delete files from binary storage if they are old, or if folder size more than 10Gb
                bat """
                IF EXIST "${CIS_TOOLS}\\..\\PluginsBinaries" (
                    forfiles /p "${CIS_TOOLS}\\..\\PluginsBinaries" /s /d -2 /c "cmd /c del @file"
                    powershell -c "\$folderSize = (Get-ChildItem -Recurse \"${CIS_TOOLS}\\..\\PluginsBinaries\" | Measure-Object -Property Length -Sum).Sum / 1GB; if (\$folderSize -ge 10) {Remove-Item -Recurse -Force \"${CIS_TOOLS}\\..\\PluginsBinaries\";};"
                )
                """
                // copy current installer to local storage, if required
                if(!(fileExists("${CIS_TOOLS}/../PluginsBinaries/${options.pluginWinSha}.msi"))) {
                    unstash 'appWindows'
                    bat """
                    IF NOT EXIST "${CIS_TOOLS}\\..\\PluginsBinaries" mkdir "${CIS_TOOLS}\\..\\PluginsBinaries"
                    rename RadeonProRenderForMaya.msi ${options.pluginWinSha}.msi
                    copy ${options.pluginWinSha}.msi "${CIS_TOOLS}\\..\\PluginsBinaries\\${options.pluginWinSha}.msi"
                    """
                } else {
                    bat """
                    copy "${CIS_TOOLS}\\..\\PluginsBinaries\\${options.pluginWinSha}.msi" ${options.pluginWinSha}.msi
                    """
                }
                // install plugin
                bat """
                msiexec /i "${options.pluginWinSha}.msi" /quiet /qn PIDKEY=${env.RPR_PLUGIN_KEY} /L+ie ../../${STAGE_NAME}.install.log /norestart
                """
            }
            break
        case 'OSX':
            // TODO: implement plugin installation for OSX
            echo "skip"
            break
        default:
            echo "skip"
    }

    /*
    build render cache - render default scene by RPR
    this step required for normalization of render time for first test case
    */
    if(buildRenderCache)
    {
        switch(osName)
        {
        case 'Windows':
            dir("scripts")
            {
                bat "build_rpr_cache.bat"
            }
            break;
        case 'OSX':
            // TODO: implement render cache building for OSX
            echo "pass"
            break;
        default:
            echo "pass"
        }
    }
}