def call(String path, String prefix, String new_version, String delimeter = '.')
{
    python3(
      "${CIS_TOOLS}/version_write.py --file \"${path}\" --prefix \"${prefix}\" --version \"${new_version}\" --delimeter ${delimeter}"
        )
}
