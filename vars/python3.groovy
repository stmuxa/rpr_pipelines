def call(String command)
{
    String ret
    if(isUnix())
    {
        sh """
            python3 -c \"${command}\"
        """
    ret = sh(
                script: """
                python3 -c \"${command}\"
                """,
                returnStdout: true
            )
    }
    else
    {
        bat(
                script: """
                python -c \"${command}\"
                """,
                returnStdout: true
            )
    }
    return ret
}
