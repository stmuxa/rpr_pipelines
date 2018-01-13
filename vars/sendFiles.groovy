
def call(String osName, String local, String remote)
{
    if(osName == 'Windows')
    {
        bat """
            %CIS_TOOLS%\\sendFiles.bat ${local} ${remote}
        """
    }
    else
    {
        sh """
            ${CIS_TOOLS}/sendFiles.sh \"${local}\" ${remote}
        """
    }
}
