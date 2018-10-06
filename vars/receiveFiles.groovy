
def call(String remote, String local)
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
            %CIS_TOOLS%\\receiveFiles.bat ${remote} /mnt/c/ReferenceImages/${local}
        """
        bat """
            xcopy /E /S /Y "%REF_IMAGES%\\${remote}" "Work\\Baseline"
        """
    }
}
