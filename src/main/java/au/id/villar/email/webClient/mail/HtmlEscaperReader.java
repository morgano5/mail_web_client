package au.id.villar.email.webClient.mail;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import javax.mail.BodyPart;
import javax.mail.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO optimize this entire class (loading an entire message in memory looks quite inefficient)
public class HtmlEscaperReader extends InputStream {

    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)([A-Za-z0-9:_-][A-Za-z0-9:._-]*)[ \\t\\r\\n]?(?:[ \\t\\r\\n]*(?:[A-Za-z0-9:_-][A-Za-z0-9:._-]*)(?:[ \\t\\r\\n]*=[ \\t\\r\\n]*(?:(?:'[^']*')|(?:\"[^\"]*\")))?)*[ \\t\\r\\n]*(/?)>");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("[ \\t\\r\\n]*([A-Za-z0-9:_-][A-Za-z0-9:._-]*)(?:[ \\t\\r\\n]*=[ \\t\\r\\n]*((?:'[^']*')|(?:\"[^\"]*\")))?");

    // TAG fate: allowed, forbidden (with warning), edit hrefs
    // STYLE fate: allowed, edit hrefs
    //private static final Map<String, Set<String>> ALLOWED_TAGS;// = Collections.unmodifiableMap()


//    private static final Set<CaseInsensitiveCharSequence> FORBIDDEN

    static {
        String[][] allowedTagsAttrs = {
                {"html"},
                {"head"},
                {"title"},
                {"body"},
                {"table", "cellspacing", "cellpadding", "border", "width"}
        };

        String[][] allowedGlobalAttrs = {
                {"id"},
        };

        String[][] forbidden = {
                {"script"}
        };

        String[][] hyperlinks = {
                {"img", "src"},
                {"a", "href"}
        };

//        ALLOWED_TAGS;// = Collections.unmodifiableMap()
    }

    private static enum TagAtributeAction { ALLOW, FORBID, FURTHER }

    //    private static boolean isAllowedTag(String tagName) {
//
//    }

    private final ByteArrayInputStream message;

    public HtmlEscaperReader(Charset charset, InputStream rawStream, Map<String, String> hrefMappings) {
        StringBuilder builder = loadMessage(charset, rawStream);
        filterContent(builder, hrefMappings);
        message = new ByteArrayInputStream(builder.toString().getBytes(charset));
    }

    @Override
    public int read() throws IOException {
        return message.read();
    }

    @Override
    public void close() throws IOException {
        message.close();
    }


    private StringBuilder loadMessage(Charset charset, InputStream rawStream) {
        InputStreamReader reader = new InputStreamReader(rawStream, charset);
        try (StringWriter writer = new StringWriter()) {
            char[] buffer = new char[2048];
            int read;
            while((read = reader.read(buffer)) != -1) writer.write(buffer, 0, read);
            return new StringBuilder(writer.toString());
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void filterContent(StringBuilder builder, Map<String, String> hrefMappings) {











        // TODO HTML and CSS escaping

//        for(Map.Entry<String, String> entry: hrefMappings.entrySet())
//            System.out.println(">>>>>> URL MAPPING: " + entry.getKey() + "   --->   " + entry.getValue());


//        Part parent = part instanceof BodyPart ? ((BodyPart)part).getParent().getParent(): null;
//        if(parent != null) {
//            int length = path.lastIndexOf(',');
//            if(length == -1) length = path.length();
//            System.out.println("\n\n\n---------------------\n" + Utils.formattedInfo(parent, path.substring(0, length)) + "\n---------------------\n\n\n");
//            System.out.println("\n\n\n---------------------\n");
//            try(Reader reader = new InputStreamReader(parent.getInputStream(), "us-ascii")) {
//                int ch;
//                while((ch = reader.read()) != -1) System.out.print((char)ch);
//            }
//            System.out.println("\n---------------------\n\n\n");
//        }










//        // TODO mapping HREFs here
//        Matcher tagMatcher = TAG_PATTERN.matcher(builder);
//        int start = 0;
//        while(tagMatcher.find(start)) {
//
//            System.out.println(">> TAG: " + tagMatcher.group() + "    --    CLOSING: " + (tagMatcher.group(1).length() == 1) + ", NAME: " + tagMatcher.group(2) + ", EMPTY: " + (tagMatcher.group(3).length() == 1));
//            start = tagMatcher.end();
//        }
//        // ------------------------


        // TODO provisional code, needs to be replaced with something more robust
        String unsafe = builder.toString();
        String lessUnsafe = Jsoup.clean(unsafe, Whitelist.basic());
        builder.replace(0, builder.length(), lessUnsafe);


    }




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
