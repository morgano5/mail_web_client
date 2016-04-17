import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlParser {

    private static final Pattern TAG_PATTERN = Pattern.compile("<([A-Za-z0-9:_-][A-Za-z0-9:._-]*)[ \\t\\r\\n]?(?:[ \\t\\r\\n]*(?:[A-Za-z0-9:_-][A-Za-z0-9:._-]*)(?:[ \\t\\r\\n]*=[ \\t\\r\\n]*(?:(?:'[^']*')|(?:\"[^\"]*\")))?)*[ \\t\\r\\n]*/?>");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("[ \\t\\r\\n]*([A-Za-z0-9:_-][A-Za-z0-9:._-]*)(?:[ \\t\\r\\n]*=[ \\t\\r\\n]*((?:'[^']*')|(?:\"[^\"]*\")))?");


    public static void main(String[] args) {

        String[] tests = {
                "qewrqewrqrew<test1>wwwewer",
                "werwerwerw<test2 >qwewerwer",
                "werwerwerw<test3      >qwewerwer",
                "werwerwerw<test4    uno  >qwewerwer",
                "werwerwerw<test4    uno=''  >qwewerwer",
                "werwerwerw<test4    uno='1'  >qwewerwer",
                "qeqwe<test2 uno='11'   dos = \"22\"   >werwqqwe",
                "qeqwe<test2 uno='1'dos=\"2\">werwqqwe",
                "qeqwe<test2 uno='1'   dos = \"2\"   />werwqqwe",
                "qeqwe<test2 uno='1'dos=\"2\"/>werwqqwe"};

        for(String test: tests) {
            Matcher matcher = TAG_PATTERN.matcher(test);
            if(!matcher.find()) {
                System.out.println("ERROR -- doesn't match: " + test);
                return;
            }
            System.out.println("MATCHES >> " + matcher.group());
            String tag = matcher.group(1);
            System.out.println(">> tag: " + tag);
            String attrs = matcher.group().substring(tag.length() + 1);
            matcher = ATTRIBUTE_PATTERN.matcher(attrs);
            while(matcher.find()) {
                System.out.println(">> attr: " + matcher.group(1) + ", value: " + matcher.group(2));
            }
            System.out.println();
        }
    }



}
