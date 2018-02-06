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
            python3 -c \"${command}\"
        """
    }
}
