package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.mail.Mailbox;
import com.sun.mail.imap.IMAPMessage;
import org.apache.log4j.Logger;

import javax.mail.*;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.regex.Pattern;

class MailContentProcessor {

    private static final Logger LOG = Logger.getLogger(MailContentProcessor.class);

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

        ContentType contentType = getContentType(message);

        if(contentType.type.startsWith("multipart/")) {
            Object content = message.getContent();
            Multipart multipart = (Multipart)content;

            switch(contentType.type) {
                case "multipart/alternative":
                    return sendMultipartAlternative(multipart, response);
                default:
                    return message.getInputStream();
            }
        } else {
            return message.getInputStream();
        }
    }


    private InputStream sendMultipartAlternative(Multipart multipart, HttpServletResponse response)
            throws MessagingException, IOException {
        int textPlain = 0;
        String textCharset = null;
        int count = multipart.getCount();
        for(int i = 0; i < count; i++) {
            BodyPart part = multipart.getBodyPart(i);
            ContentType contentType = getContentType(part);
            // TODO not allways text.html is going to be at the root level, need to also check for multipart/related
            if(contentType.type.equals("text/html")) {
                response.addHeader("Content-type", "text/html" + (contentType.charset != null? "; charset=\"" + contentType.charset + '"': ""));
                return multipart.getBodyPart(i).getInputStream();
            } else if(contentType.type.equals("text/plain")) {
                textPlain = i;
                textCharset = contentType.charset;
            }
        }
        response.addHeader("Content-type", "text/plain" + (textCharset != null? "; charset=\"" + textCharset + '"': ""));
        return multipart.getBodyPart(textPlain).getInputStream();
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




    private ContentType getContentType(Part part) throws MessagingException {
        String[] rawValues = part.getHeader("Content-type");
        if(rawValues == null || rawValues.length == 0) return new ContentType("text/plain", "us-ascii");
        String rawValue = rawValues[0];
        int semiColonPos = rawValue.indexOf(';');
        String type = rawValue.substring(0, semiColonPos != -1? semiColonPos: rawValue.length()).trim().toLowerCase();
        if(semiColonPos == -1) return new ContentType(type, type.startsWith("text/")? "us-ascii": null);
        String strParameters = rawValue.substring(semiColonPos + 1);


        // TODO rewrite this part
        String[] parameters = strParameters.split("[ \\t]*;[ \\t]*");
        for(String parameter: parameters) {
            parameter = parameter.trim();
            if(!parameter.startsWith("charset")) continue;
            parameter = parameter.substring(parameter.indexOf('=') + 1);
            parameter = parameter.startsWith("\"")? parameter.substring(1,parameter.length() - 1): parameter;
            return new ContentType(type, parameter);
        }
        return new ContentType(type, type.startsWith("text/")? "us-ascii": null);
        // ----------------------

    }

    private class ContentType {
        final String type;
        final String charset;

        public ContentType(String type, String charset) {
            this.type = type;
            this.charset = charset;
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
