package au.id.villar.email.webClient.mail;

import javax.mail.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@Deprecated
public class MailPart {

    private enum Level { PART_ONLY, PART_AND_CHILDREN, DEEP }

    private static final String DEFAULT_CONTENT_TYPE = "text/plain; charset=\"us-ascii\"";

    public final String path;
    public final String contentId;
    public final String contentType;
    public final String charset;
    public final boolean isMultipart;
    public final boolean attachment;
    public final String filename;
    public final Map<String, String> others;
    public final List<MailPart> parts;

    private MailPart(Part part, String path, Level level) throws IOException, MessagingException {

        this.path = path != null? path: "";

        String contentId = null;
        String contentType = DEFAULT_CONTENT_TYPE;
        String contentDisposition = "";
        Map<String, String> others = new HashMap<>();
        List<MailPart> parts = new ArrayList<>();

        Enumeration enumeration = part.getAllHeaders();
        while(enumeration.hasMoreElements()) {
            Header header = (Header)enumeration.nextElement();
            String headerName = header.getName().toLowerCase();

            switch(headerName) {
                case "content-id": contentId = header.getValue(); break;
                case "content-type": contentType = header.getValue(); break;
                case "content-disposition": contentDisposition = header.getValue(); break;
                default: others.put(header.getName(), header.getValue());
            }

        }

        this.contentType = getMainValue(contentType).toLowerCase();
        this.isMultipart = Utils.isMultipart(contentType);

        String charset = getParameters(contentType).get("charset");

        this.charset = charset != null ? charset : (this.contentType.startsWith("text/") ? "us-ascii" : null);

        if(this.isMultipart && (level == Level.PART_AND_CHILDREN || level == Level.DEEP)) {
            Multipart multipart = (Multipart)part.getContent();
            int count = multipart.getCount();
            for(int x = 0; x < count; x++) {
                BodyPart bodyPart = multipart.getBodyPart(x);
                parts.add(new MailPart(bodyPart, path + MailMessage.SEPARATOR + x,
                        (level == Level.PART_AND_CHILDREN? Level.PART_ONLY: Level.DEEP)));
            }
        }

        if(contentId != null && contentId.startsWith("<") && contentId.endsWith(">")) {
            contentId = contentId.substring(1, contentId.length() - 1);
        }
        this.contentId = contentId;
        this.attachment = contentDisposition != null && getMainValue(contentDisposition).equals("attachment");
        this.filename = getParameters(contentDisposition).get("filename");
        this.others = Collections.unmodifiableMap(others);
        this.parts = Collections.unmodifiableList(parts);
    }

    public String contentTypeHeaderValue() {
        return contentType + (charset != null ? "; charset=\"" + charset + '"' : "");
    }

    public String contentDispositionHeaderValue() {
        return attachment? "attachment; filename=\"" + (filename != null? filename: "file") + "\"": "inline";
    }

    String formattedInfo() {
        return formattedInfo(0);
    }


    public static boolean attachmentsPresent(Part part) throws IOException, MessagingException {

        if(!Utils.isMultipart(part.getContentType())) {
            String contentDisposition = part.getDisposition();
            return contentDisposition != null && contentDisposition.equalsIgnoreCase(Part.ATTACHMENT);
        }

        Multipart multipart = (Multipart)part.getContent();
        int count = multipart.getCount();
        for(int x = 0; x < count; x++) {
            BodyPart bodyPart = multipart.getBodyPart(x);
            if(attachmentsPresent(bodyPart)) return true;
        }

        return false;
    }

    public static MailPart getSinglePartInfo(Part part, String path) throws IOException, MessagingException {
        return new MailPart(part, path, MailPart.Level.PART_ONLY);
    }

    public static Map<String, String> hrefMappings(Part part, String path)
            throws IOException, MessagingException {

        if(path == null) {
            return Collections.emptyMap();
        }

        Part parent = part instanceof BodyPart ? ((BodyPart)part).getParent().getParent(): null;
        if(parent == null) {
            return Collections.emptyMap();
        }

        int length = path.lastIndexOf(MailMessage.SEPARATOR);
        if(length == -1) length = path.length();
        List<MailPart> relatedParts =
                new MailPart(parent, path.substring(0, length), MailPart.Level.PART_AND_CHILDREN).parts;

        Map<String, String> hrefMappings = new HashMap<>(relatedParts.size());
        for(MailPart mailPart : relatedParts) {
            if(mailPart.attachment || mailPart.contentId == null) continue;
            hrefMappings.put(mailPart.contentId, mailPart.path);
        }

        return hrefMappings;
    }

    public static List<MailPart> getAttachments(Part part, String path) throws IOException, MessagingException {
        MailPart info = new MailPart(part, path, Level.DEEP);
        List<MailPart> attachments = new ArrayList<>();
        Queue<MailPart> queue = new LinkedList<>();
        queue.offer(info);
        while(!queue.isEmpty()) {
            MailPart current = queue.poll();
            current.parts.forEach(queue::offer);
            if(current.attachment) attachments.add(current);
        }
        return attachments;
    }

    public static String formattedInfo(Part part, String path) throws IOException, MessagingException {
        return new MailPart(part, path, MailPart.Level.DEEP).formattedInfo();
    }













    private static final Pattern HEADER_FIELD_PARAMETER_PATTERN = Pattern.compile("[ \t]*((?:[\\x20-\\x7E\t]|\\r?\\n[ \t])+)|(\\r?\\n\\r?\\n)");

/*

headerValue = value(; parameter)*

parameter = attribute '=' value

attribute =




obs-NO-WS-CTL   =   %d1-8 /            ; US-ASCII control
                       %d11 /             ;  characters that do not
                       %d12 /             ;  include the carriage
                       %d14-31 /          ;  return, line feed, and
                       %d127              ;  white space characters

   obs-ctext       =   obs-NO-WS-CTL







   ctext           =   %d33-39 /          ; Printable US-ASCII
                       %d42-91 /          ;  characters not including
                       %d93-126 /         ;  "(", ")", or "\"
                       obs-ctext

   ccontent        =   ctext / quoted-pair / comment

   comment         =   "(" *([FWS] ccontent) [FWS] ")"

   CFWS            =   (1*([FWS] comment) [FWS]) / FWS

   VCHAR           =  %x21-7E              ; visible (printing) characters

   quoted-pair     =   ("\" (VCHAR / WSP)) / obs-qp

   WSP             =    SP / HTAB

   FWS             =   ([*WSP CRLF] 1*WSP) /  obs-FWS


   qtext           =   %d33 /             ; Printable US-ASCII
                       %d35-91 /          ;  characters not including
                       %d93-126 /         ;  "\" or the quote character
                       obs-qtext

   qcontent        =   qtext / quoted-pair

   quoted-string   =   [CFWS]
                       DQUOTE *([FWS] qcontent) [FWS] DQUOTE
                       [CFWS]

*/

    private static String getMainValue(String headerValue) {
        int semiColonPos = headerValue.indexOf(';');
        return headerValue.substring(0, semiColonPos != -1? semiColonPos: headerValue.length()).trim().toLowerCase();
    }

    private static Map<String, String> getParameters(String headerValue) {
        int semiColonPos = headerValue.indexOf(';');

        if(semiColonPos == -1) {
            return Collections.emptyMap();
        }


        Map<String, String> parameterMap = new HashMap<>();
        String strParameters = headerValue.substring(semiColonPos + 1);

        // TODO rewrite this part
        String[] parameters = strParameters.split("[ \\t]*;[ \\t]*");
        for (String parameter : parameters) {
            parameter = parameter.trim();
            int equalsPos = parameter.indexOf('=');
            String name = parameter.substring(0, equalsPos).trim();
            parameter = parameter.substring(equalsPos + 1).trim();
            parameter = parameter.startsWith("\"") ? parameter.substring(1, parameter.length() - 1) : parameter;
            parameterMap.put(name, parameter);
        }
        // ----------------------

        return parameterMap;
    }


















    private String formattedInfo(int identation) {
        String prefix = identation(identation);
        StringBuilder builder = new StringBuilder();
        builder
                .append('|').append(prefix).append("path: ").append(path).append('\n')
                .append('|').append(prefix).append("contentId: ").append(contentId).append('\n')
                .append('|').append(prefix).append("contentType: ").append(contentType).append('\n');

        if(charset != null)
            builder.append('|').append(prefix).append("charset: ").append(charset).append('\n');

        builder
                .append('|').append(prefix).append("disposition: ").append(attachment? "ATTACHMENT": "INLINE")
                .append('\n');
        if(filename != null)
            builder.append('|').append(prefix).append("filename: ").append(filename).append('\n');

        for(Map.Entry<String, String> other: others.entrySet())
            builder.append('|').append(prefix).append("[Other] ").append(other.getKey()).append(": ")
                    .append(other.getValue()).append('\n');

        for(MailPart part: parts)
            builder.append(part.formattedInfo(identation + 1));

        builder.append('\n');

        return builder.toString();
    }

    private String identation(int identation) {
        char[] chs = new char[identation * 4];
        Arrays.fill(chs, ' ');
        return new String(chs);
    }

}
