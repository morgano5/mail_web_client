package au.id.villar.email.webClient.mail;

import javax.mail.*;
import java.io.IOException;
import java.util.*;

public class PartInfo {

    public final String path;
    public final String contentId;
    public final ContentType contentType;
    public final boolean attachment;
    public final String filename;
    public final Map<String, String> others;
    public final List<PartInfo> parts;

    public PartInfo(Part part, String path) throws IOException, MessagingException {

        this.path = path;
        String contentId = null;
        ContentType contentType = ContentType.DEFAULT;
        String contentDisposition = "";
        Map<String, String> others = new HashMap<>();
        List<PartInfo> parts = new ArrayList<>();

        Enumeration enumeration = part.getAllHeaders();
        while(enumeration.hasMoreElements()) {
            Header header = (Header)enumeration.nextElement();
            String headerName = header.getName().toLowerCase();

            switch(headerName) {
                case "content-id": contentId = header.getValue(); break;
                case "content-type": contentType = new ContentType(header.getValue()); break;
                case "content-disposition": contentDisposition = header.getValue(); break;
                default: others.put(header.getName(), header.getValue());
            }

        }
        if(contentType.type.startsWith("multipart/")) {
            Multipart multipart = (Multipart)part.getContent();
            int count = multipart.getCount();
            for(int x = 0; x < count; x++) {
                BodyPart bodyPart = multipart.getBodyPart(x);
                parts.add(new PartInfo(bodyPart, path + ',' + x));
            }
        }

        Map<String, String> parameters = Utils.getParameters(contentDisposition);

        if(contentId != null && contentId.startsWith("<") && contentId.endsWith(">")) {
            contentId = contentId.substring(1, contentId.length() - 1);
        }
        this.contentId = contentId;
        this.contentType = contentType;
        this.attachment = contentDisposition != null && Utils.getMainValue(contentDisposition).equals("attachment");
        this.filename = parameters.get("filename");
        this.others = Collections.unmodifiableMap(others);
        this.parts = Collections.unmodifiableList(parts);
    }

    public String formattedInfo() {
        return formattedInfo(0);
    }

    private String formattedInfo(int identation) {
        String prefix = identation(identation);
        StringBuilder builder = new StringBuilder();
        builder
                .append('|').append(prefix).append("path: ").append(path).append('\n')
                .append('|').append(prefix).append("contentId: ").append(contentId).append('\n')
                .append('|').append(prefix).append("contentType: ").append(contentType.type).append('\n');

        if(contentType.charset != null)
            builder.append('|').append(prefix).append("charset: ").append(contentType.charset).append('\n');

        builder
                .append('|').append(prefix).append("disposition: ").append(attachment? "ATTACHMENT": "INLINE")
                .append('\n');
        if(filename != null)
            builder.append('|').append(prefix).append("filename: ").append(filename).append('\n');

        for(Map.Entry<String, String> other: others.entrySet())
            builder.append('|').append(prefix).append("[Other] ").append(other.getKey()).append(": ")
                    .append(other.getValue()).append('\n');

        for(PartInfo part: parts)
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
