def call(String msiName, String logs="")
{
    logs = logs ?: "${STAGE_NAME}"
    if (fileExists(msiName)){
        bat """
            msiexec /i "${msiName}" /quiet /qn /L+ie ${logs}.install.log /norestart
        """
    }else{
        echo "Missing msi ${msiName}"
    }
}