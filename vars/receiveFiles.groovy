
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
            set remote=%${remote}:/=\%
            copy "%REF_IMAGES%\\${remote}" ".\Work\Baseline"
        """
    }
}
