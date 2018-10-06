
def call(String remote)
{
    if(isUnix())
    {
        sh """
            ${CIS_TOOLS}/receiveFiles.sh \"${remote}\" ${local}
        """
    }
    else
    {
        bat """
            %CIS_TOOLS%\\receiveFiles.bat ${remote} /mnt/c/ReferenceImages/${remote}
        """
        bat """
            copy "%REF_IMAGES%\${remote}" "./Work/Baselines"
        """
    }
}
