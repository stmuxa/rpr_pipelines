
def call(String branchName, String repoName)
{
    try_git = 0
    while(try_git < 10)
    {
        sleep(5 * try_git)
        try_git += 1
        echo "TRY: ${try_git}"
        try
        {
            checkOutBranchOrScm(branchName, repoName)
        }
        catch(e)
        {
            println(e.toString())
            println(e.getMessage())
        }
        else
        {
            return
        }
    }

    currentBuild.result = 'FAILED'
    error('Failed to connect github')
}
