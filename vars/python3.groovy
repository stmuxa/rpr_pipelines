def call(String command)
{
    if(isUnix())
    {
        sh "python3 ${command}"
    }
    else
    {
        bat "python3 ${command}"
    }
}
