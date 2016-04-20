package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.mail.ContentType;
import au.id.villar.email.webClient.mail.HtmlEscaperReader;
import au.id.villar.email.webClient.mail.PartInfo;
import au.id.villar.email.webClient.mail.Mailbox;
import org.apache.log4j.Logger;

import javax.mail.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
        boolean isRequestForMainContent = tokens.size() == 1;

        processMessage(mailMessageId, response, message -> {

            Part part = message;
            for(int i = 1; i < tokens.size(); i++) {
                ContentType contentType = new ContentType(part);
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

            InputStream input = getPart(part, response, isRequestForMainContent? mailReference: null);
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

    private static InputStream getPart(Part part, HttpServletResponse response, String path)
            throws MessagingException, IOException {

        ContentType contentType = new ContentType(part);

        if(contentType.type.startsWith("multipart/")) {
            return getMultipart(contentType, (Multipart)part.getContent(), response, path);
        } else {
            return getSinglePart(contentType, part, response, path);
        }
    }

    private static InputStream getSinglePart(ContentType contentType, Part part, HttpServletResponse response, String path)
            throws IOException, MessagingException {


        Part parent = part instanceof BodyPart? ((BodyPart)part).getParent().getParent(): null;
        if(parent != null) {
            int length = path.lastIndexOf(',');
            if(length == -1) length = path.length();
            System.out.println("\n\n\n---------------------\n" + new PartInfo(parent, path.substring(0, length), PartInfo.Level.DEEP).formattedInfo() + "\n---------------------\n\n\n");
            System.out.println("\n\n\n---------------------\n");
            try(Reader reader = new InputStreamReader(parent.getInputStream(), "us-ascii")) {
                int ch;
                while((ch = reader.read()) != -1) System.out.print((char)ch);
            }
            System.out.println("\n---------------------\n\n\n");
        }



        // TODO content-disposition, file name, etc ... HTML and CSS escaping
        setContentType(contentType, response);
        return contentType.type.equals("text/html") && path != null? new HtmlEscaperReader(Charset.forName(contentType.charset), part.getInputStream(), null): part.getInputStream();
    }

    private static InputStream getMultipart(ContentType contentType, Multipart multipart, HttpServletResponse response, String path)
            throws IOException, MessagingException {
        switch(contentType.type) {
            case "multipart/alternative":
                return getMultipartAlternative(multipart, response, path);
            default:
                return getMultipartMixed(multipart, response, path);
        }

    }

    private static InputStream getMultipartAlternative(Multipart multipart, HttpServletResponse response, String path)
            throws MessagingException, IOException {
        int textPlain = 0;
        ContentType textPlainContextType = null;
        int count = multipart.getCount();
        for(int i = 0; i < count; i++) {
            BodyPart part = multipart.getBodyPart(i);
            ContentType contentType = new ContentType(part);
            switch (contentType.type) {
                case "text/html":
                    if(path != null) path += "," + i;
                    return getSinglePart(contentType, part, response, path);
                case "text/plain":
                    textPlain = i;
                    textPlainContextType = contentType;
                    break;
                default:
                    if(contentType.type.startsWith("multipart/")) {
                        if(path != null) path += "," + i;
                        return getMultipart(contentType, (Multipart)part.getContent(), response, path);
                    }
            }
        }
        if(path != null) path += "," + textPlain;
        return getSinglePart(textPlainContextType, multipart.getBodyPart(textPlain), response, path);
    }

    private static InputStream getMultipartMixed(Multipart multipart, HttpServletResponse response, String path)
            throws MessagingException, IOException {
        // TODO verify that the main part will always be #0
        if(path != null) path += ",0";
        return getPart(multipart.getBodyPart(0), response, path);
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

}
