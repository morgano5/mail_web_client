package au.id.villar.email.webClient.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private static class CaseInsensitiveCharSequence implements CharSequence {
        private final String name;

        public CaseInsensitiveCharSequence(CharSequence name) {
            this.name = name.toString().toLowerCase();
        }

        @Override
        public int length() {
            return name.length();
        }

        @Override
        public char charAt(int index) {
            return name.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new CaseInsensitiveCharSequence(name.subSequence(start, end));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !CharSequence.class.isAssignableFrom(o.getClass())) return false;
            CharSequence charSequence = (CharSequence)o;
            if (charSequence.length() != this.length()) return false;
            for(int i = 0; i < charSequence.length(); i++) {
                char ch = Character.toLowerCase(charSequence.charAt(i));
                if(ch != name.charAt(i)) return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    private static boolean isAllowedTag(String tagName) {

    }

    private final ByteArrayInputStream message;

    public HtmlEscaperReader(Charset charset, InputStream rawStream, Map<String, String> hrefMappings) {
        StringBuilder builder = loadMessage(charset, rawStream);
        filterContent(builder);
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

    private void filterContent(StringBuilder builder) {

        // TODO mapping HREFs here
        Matcher tagMatcher = TAG_PATTERN.matcher(builder);
        int start = 0;
        while(tagMatcher.find(start)) {

            System.out.println(">> TAG: " + tagMatcher.group() + "    --    CLOSING: " + (tagMatcher.group(1).length() == 1) + ", NAME: " + tagMatcher.group(2) + ", EMPTY: " + (tagMatcher.group(3).length() == 1));
            start = tagMatcher.end();
        }
        // ------------------------

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
