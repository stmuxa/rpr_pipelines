def call(String version, Integer index = 3)
{
    String new_version =python3(
        "${CIS_TOOLS}/version_inc.py --version \"${version}\" --index ${index}"
                ).split('\r\n')[2].trim()
    return new_version
}
