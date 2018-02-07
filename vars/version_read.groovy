def call(String path, String ver_prefix, String delimeter = '.')
{
    String currentversion=python3(
        "${CIS_TOOLS}/version_read.py --file \"${path}\" --prefix \"${ver_prefix}\" --delimeter ${delimeter}"
    ).split('\r\n')[2].trim()
    return currentversion;
}
