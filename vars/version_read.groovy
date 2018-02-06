def call(String path, String ver_prefix)
{
    String currentversion=python3(
        "${CIS_TOOLS}/version_read.py --file version.h --prefix \"#define VERSION_STR\""
    ).split('\r\n')[2].trim()
    return currentversion;
}
