package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.mail.Mailbox;
import com.sun.mail.imap.IMAPBodyPart;
import com.sun.mail.imap.IMAPMessage;
import org.apache.log4j.Logger;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

class MailContentProcessor {

    private static final Logger LOG = Logger.getLogger(MailContentProcessor.class);

    private Mailbox mailbox;

    MailContentProcessor(Mailbox mailbox) {
        this.mailbox = mailbox;
    }

    void transferMainContent(String mailMessageId, HttpServletResponse response) {
        processMessage(mailMessageId, response, message -> {
            InputStream input = getMainContentAndSetHeaders(message, response);
            OutputStream output = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        });
    }

    private InputStream getMainContentAndSetHeaders(IMAPMessage message, HttpServletResponse response)
            throws MessagingException, IOException {
        return partsInfo(message);
//        Object content = message.getContent();
//        if(content instanceof MimeMultipart) {
//            MimeMultipart multipart = (MimeMultipart)content;
//
//            return partsInfo(multipart);
////            String contentType = multipart.getContentType();
////            int pos = contentType.indexOf(';');
////            contentType = contentType.substring(0, pos != -1? pos: contentType.length());
////
////            switch(contentType) {
////                case "multipart/alternative":
////                    return sendMultipartAlternative(multipart, response);
////                default:
////                    return message.getInputStream();
////            }
//        } else {
//            return message.getInputStream();
//        }
////          return message.getInputStream();
    }

    private InputStream sendMultipartAlternative(MimeMultipart multipart, HttpServletResponse response)
            throws MessagingException, IOException {
        return multipart.getBodyPart(1).getInputStream();
    }





    private InputStream partsInfo(Part part) throws IOException, MessagingException {
        StringBuilder builder = new StringBuilder();
        partsInfo("", part, builder);
        return new ByteArrayInputStream(builder.toString().getBytes());
    }

    private void partsInfo(String identation, Part part, StringBuilder builder) throws MessagingException, IOException {
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




    private void processMessage(String mailMessageId, HttpServletResponse response, Mailbox.MessageProcess process) {
        try {
            if(!mailbox.processMessage(mailMessageId, process)) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (MessagingException | IOException e) {
            LOG.error("Error getting content: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
