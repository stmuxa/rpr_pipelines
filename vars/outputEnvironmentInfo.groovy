
def call(String osName)
{
    if(osName == 'Windows')
    {
         bat "set > ${STAGE_NAME}.log"
    }
    else
    {
         sh "env > ${STAGE_NAME}.log"
    }
}
