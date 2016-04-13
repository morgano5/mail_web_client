package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.mail.Mailbox;
import org.apache.log4j.Logger;

import javax.mail.*;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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

    void transferMainContent(String mailReference, HttpServletResponse response) {

        List<Object> tokens = getTokens(mailReference);
        if(tokens == null) {
            setNotFound(response);
            return;
        }
        String mailMessageId = (String)tokens.get(0);

        processMessage(mailMessageId, response, message -> {

            Part part = message;
            for(int i = 1; i < tokens.size(); i++) {
                ContentType contentType = getContentType(part);
                if(!contentType.type.startsWith("multipart")) {
                    setNotFound(response);
                    return;
                }
                int partNumber = (Integer)tokens.get(i);
                Multipart multipart = (Multipart)part.getContent();
                if(multipart.getCount() - 1 < partNumber) {
                    setNotFound(response);
                    return;
                }
                part = multipart.getBodyPart(partNumber);
            }

            InputStream input = getPart(part, response);
            OutputStream output = response.getOutputStream();
            transfer(input, output);
        });
    }

    private void processMessage(String mailMessageId, HttpServletResponse response, Mailbox.MessageProcess process) {
        try {
            if(!mailbox.processMessage(mailMessageId, process)) response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (MessagingException | IOException e) {
            LOG.error("Error getting content: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static InputStream getPart(Part part, HttpServletResponse response)
            throws MessagingException, IOException {

        ContentType contentType = getContentType(part);

        if(contentType.type.startsWith("multipart/")) {
            return getMultipart(contentType, (Multipart)part.getContent(), response);
        } else {
            setContentType(contentType, response);
            return part.getInputStream();
        }
    }

    private static InputStream getMultipart(ContentType contentType, Multipart multipart, HttpServletResponse response)
            throws IOException, MessagingException {
        switch(contentType.type) {
            case "multipart/alternative":
                return getMultipartAlternative(multipart, response);
            case "multipart/related":
                return getMultipartRelated(multipart, response);
            default:
                return getMultipartMixed(multipart, response);
        }

    }

    private static InputStream getMultipartAlternative(Multipart multipart, HttpServletResponse response)
            throws MessagingException, IOException {
        int textPlain = 0;
        ContentType textPlainContextType = null;
        int count = multipart.getCount();
        for(int i = 0; i < count; i++) {
            BodyPart part = multipart.getBodyPart(i);
            ContentType contentType = getContentType(part);
            switch (contentType.type) {
                case "text/html":
                    setContentType(contentType, response);
                    return multipart.getBodyPart(i).getInputStream();
                case "text/plain":
                    textPlain = i;
                    textPlainContextType = contentType;
                    break;
                default:
                    if(contentType.type.startsWith("multipart/")) {
                        return getMultipart(contentType, (Multipart)part.getContent(), response);
                    }
            }
        }
        setContentType(textPlainContextType, response);
        return multipart.getBodyPart(textPlain).getInputStream();
    }

    private static InputStream getMultipartRelated(Multipart multipart, HttpServletResponse response)
            throws MessagingException, IOException {
        return getPart(multipart.getBodyPart(0), response);
    }

    private static InputStream getMultipartMixed(Multipart multipart, HttpServletResponse response)
            throws MessagingException, IOException {
        return getPart(multipart.getBodyPart(0), response);
    }







    private static InputStream partsInfo(Part part) throws IOException, MessagingException {
        StringBuilder builder = new StringBuilder();
        partsInfo("", part, builder);
        return new ByteArrayInputStream(builder.toString().getBytes());
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







    private static ContentType getContentType(Part part) throws MessagingException {
        String[] rawValues = part.getHeader("Content-type");
        if(rawValues == null || rawValues.length == 0) return new ContentType("text/plain", "us-ascii");
        String rawValue = rawValues[0];
        int semiColonPos = rawValue.indexOf(';');
        String type = rawValue.substring(0, semiColonPos != -1? semiColonPos: rawValue.length()).trim().toLowerCase();
        if(semiColonPos == -1) return new ContentType(type, null);
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
        return new ContentType(type, null);
        // ----------------------

    }

    private static void setNotFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    private static void setContentType(ContentType contentType, HttpServletResponse response) {
        response.addHeader("Content-type", contentType.toHeaderValue());
    }

    private static void transfer(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    private static List<Object> getTokens(String mailReference) {
        int charPos = mailReference.indexOf(',');
        if(charPos == -1) return null;
        charPos = mailReference.indexOf(',', charPos + 1);
        List<Object> results = new ArrayList<>();
        if(charPos == -1) {
            results.add(mailReference);
            return results;
        }
        results.add(mailReference.substring(0, charPos));
        int index = 0;
        while(++charPos < mailReference.length()) {
            char ch = mailReference.charAt(charPos);
            if(ch >= '0' && ch <= '9') {
                index = index * 10 + ch - '0';
            } else {
                results.add(index);
                index = 0;
            }
        }
        results.add(index);
        return results;
    }

    private static class ContentType {
        final String type;
        final String charset;

        ContentType(String type, String charset) {
            this.type = type != null? type.toLowerCase(): "text/plain";
            this.charset = charset != null? charset: (this.type.startsWith("text/")? "us-ascii": null);
        }

        String toHeaderValue() {
            return type + (charset != null ? "; charset=\"" + charset + '"' : "");
        }
    }

}
