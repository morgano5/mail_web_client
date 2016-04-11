package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.mail.Mailbox;
import com.sun.mail.imap.IMAPBodyPart;
import com.sun.mail.imap.IMAPMessage;
import org.apache.log4j.Logger;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMultipart;
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
        Object content = message.getContent();
        if(content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart)content;

            return partsInfo(multipart);
//            String contentType = multipart.getContentType();
//            int pos = contentType.indexOf(';');
//            contentType = contentType.substring(0, pos != -1? pos: contentType.length());
//
//            switch(contentType) {
//                case "multipart/alternative":
//                    return sendMultipartAlternative(multipart, response);
//                default:
//                    return message.getInputStream();
//            }
        } else {
            return message.getInputStream();
        }
    }

    private InputStream sendMultipartAlternative(MimeMultipart multipart, HttpServletResponse response)
            throws MessagingException, IOException {
        return multipart.getBodyPart(1).getInputStream();
    }





    private InputStream partsInfo(MimeMultipart multipart) throws MessagingException, IOException {
        StringBuilder builder = new StringBuilder();
        partsInfo("", multipart, builder);
        return new ByteArrayInputStream(builder.toString().getBytes());
    }


    private void partsInfo(String identation, MimeMultipart multipart, StringBuilder builder) throws MessagingException, IOException {
        int count = multipart.getCount();
        for(int x = 0; x < count; x++) {
            IMAPBodyPart part = (IMAPBodyPart)multipart.getBodyPart(x);
            Enumeration enumeration = part.getAllHeaders();
            builder.append(identation).append(" PART #").append(x).append('\n');
            while(enumeration.hasMoreElements()) {
                Header header = (Header)enumeration.nextElement();
                builder.append(identation).append(" >> ").append(header.getName()).append(':').append(header.getValue()).append('\n');
            }
            if(part.getContentType().contains("multipart")) {
                partsInfo(identation + "    ", (MimeMultipart)part.getContent(), builder);
            }
            builder.append("\n\n");
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
