def call(String command)
{
    if(isUnix())
    {
        sh """
            python3 -c \"${command}\"
        """
            
    }
    else
    {
        bat """
            set PATH=c:\\python35\\;c:\\python35\\scripts\\;%PATH%
            python -c \"${command}\"
        """
    }
}
