def call(String path, String ver_prefix, String new_version, String delimeter = '.')
{
    String new_prefix = ver_prefix.replace('"', '""')

    python3(
      "${CIS_TOOLS}/version_write.py --file \"${path}\" --prefix \"${new_prefix}\" --version \"${new_version}\" --delimeter \"${delimeter}\""
        )
}
