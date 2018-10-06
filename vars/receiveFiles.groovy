
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
            %CIS_TOOLS%\\receiveFiles.bat ${remote} "${local}"
        """
    }
}
