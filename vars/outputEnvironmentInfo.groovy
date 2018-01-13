
def call(String osName)
{
    if(osName == 'Windows')
    {
         bat "HOSTNAME  >  ${STAGE_NAME}.log"
         bat "set       >> ${STAGE_NAME}.log"
    }
    else
    {
         sh "uname -a   >  ${STAGE_NAME}.log"
         sh "env        >> ${STAGE_NAME}.log"
    }
}
