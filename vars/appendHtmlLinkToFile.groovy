
def call(String fileName, String link)
{
    f = new File(fileName)
    String str = "<a href=\"${link}\">${link}</a><br>"    
    f.append(str)
}
