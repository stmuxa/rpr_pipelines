
def call(String fileName, String linkText, String link)
{
    String str = "<a href=\"${link}\">${linkText}</a><br>"    
 /*   
    new File('/Users/me/Downloads', 'myImage.gif').withOutputStream { os ->
        os << str
    }
   */ 
    f = new File(fileName)
    f.append(str)

}
