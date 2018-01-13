
def call(String osName, String remote, String local)
{
    if(osName == 'Windows')
    {
        bat """
            %CIS_TOOLS%\\receiveFiles.bat ${remote} ${local}
        """
    }
    else
    {
        sh """
            ${CIS_TOOLS}/receiveFiles.sh \"${remote}\" ${local}
        """
    }
}
