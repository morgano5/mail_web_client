package au.id.villar.email.webClient.mail;

import javax.mail.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;

public class MailProcessor {


    public static class PartInfo {
        private String contentId;
        private ContentType mimeType;
        private boolean attachemment;
        private String filename;
        private List<PartInfo> parts;
    }


    public static String partsInfo(Part part) throws IOException, MessagingException {
        StringBuilder builder = new StringBuilder();
        partsInfo("", part, builder);
        return builder.toString();
    }

    private static void partsInfo(String identation, Part part, StringBuilder builder) throws MessagingException, IOException {
        boolean isMultipart = false;
        Enumeration enumeration = part.getAllHeaders();
        while(enumeration.hasMoreElements()) {
            Header header = (Header)enumeration.nextElement();
            if(header.getName().equalsIgnoreCase("Content-type") && header.getValue().contains("multipart")) isMultipart = true;
            builder.append(identation).append(" >> ").append(header.getName()).append(':').append(header.getValue()).append('\n');
        }
        if(isMultipart) {
            Multipart multipart = (Multipart)part.getContent();
            int count = multipart.getCount();
            identation = "    " + identation;
            for(int x = 0; x < count; x++) {
                BodyPart bodyPart = multipart.getBodyPart(x);
                builder.append(identation).append(" PART #").append(x).append('\n');
                partsInfo(identation, bodyPart, builder);
                builder.append("\n\n");
            }
        }

    }




}
