
def call(String fileName, String linkText, String link)
{
    f = new File(fileName)
    String str = "<a href=\"${link}\">${linkText}</a><br>"    
    f.append(str)
}
