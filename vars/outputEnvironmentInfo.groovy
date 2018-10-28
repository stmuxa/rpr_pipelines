
def call(String osName, String logName="")
{
    // logName null - use STAGE_NAME
    logName =  logName ?: "${STAGE_NAME}"
  
    if(osName == 'Windows')
    {
         bat "HOSTNAME  >  ${logName}.log"
         bat "set       >> ${logName}.log"
    }
    else
    {
         sh "uname -a   >  ${logName}.log"
         sh "env        >> ${logName}.log"
    }
}
