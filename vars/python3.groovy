def call(String command)
{
    String ret
    if(isUnix())
    {

        ret = sh(
                    script: """
                    python3 -c \"${command}\"
                    """,
                    returnStdout: true
                )
    }
    else
    {
        withEnv(["PATH=c:\\python35\\;c:\\python35\\scripts\\;${PATH}"]) {
            ret = bat(
                script: """
                python -c \"${command}\"
                """,
                returnStdout: true
            )
        }
    }
    return ret
}
