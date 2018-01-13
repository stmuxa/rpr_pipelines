def call(String projectBranch = '', 
         String platforms = 'Windows:AMD_RXVEGA,AMD_WX9100,AMD_WX7100;Ubuntu;OSX:Intel_Iris', 
         Boolean updateRefs = false, Boolean enableNotifications = true) {
    
    rpr_baikal_script(projectBranch, platforms, updateRefs, enableNotifications)
}
