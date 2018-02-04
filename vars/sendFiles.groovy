
def call(String local, String remote)
{
    if(isUnix())
    {
        sh """
            ${CIS_TOOLS}/sendFiles.sh \"${local}\" ${remote}
        """
    }
    else
    {
        bat """
            %CIS_TOOLS%\\sendFiles.bat ${local} ${remote}
        """
    }
}
