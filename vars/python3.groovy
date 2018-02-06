def call(String command)
{
    echo command
    String ret
    if(isUnix())
    {

        ret = sh(
                    script: """
                    python3 ${command}
                    """,
                    returnStdout: true
                )
    }
    else
    {
        withEnv(["PATH=c:\\python35\\;c:\\python35\\scripts\\;${PATH}"]) {
            ret = bat(
                script: """
                python ${command}
                """,
                returnStdout: true
            )
        }
    }
    return ret
}
