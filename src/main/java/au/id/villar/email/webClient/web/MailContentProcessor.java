package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.mail.HtmlEscaperReader;
import au.id.villar.email.webClient.mail.PartInfo;
import au.id.villar.email.webClient.mail.Mailbox;
import au.id.villar.email.webClient.mail.Utils;
import org.apache.log4j.Logger;

import javax.mail.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

class MailContentProcessor {

    private static final Logger LOG = Logger.getLogger(MailContentProcessor.class);

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
        String path = isRequestForMainContent? mailReference: null;

        processMessage(mailMessageId, response, message -> {

            Part part = message;
            for(int i = 1; i < tokens.size(); i++) {
                PartInfo partInfo = Utils.getSinglePartInfo(part, path);
                if(!partInfo.isMultipart) {
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

            InputStream input = getPart(part, response, path);
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

        PartInfo partInfo = Utils.getSinglePartInfo(part, path);

        if(partInfo.isMultipart) {
            return getMultipart(partInfo, (Multipart)part.getContent(), response, path);
        } else {
            return getSinglePart(partInfo, part, response, path);
        }
    }

    private static InputStream getSinglePart(PartInfo partInfo, Part part, HttpServletResponse response, String path)
            throws IOException, MessagingException {

        response.addHeader("Content-Type", partInfo.contentTypeHeaderValue());
        response.addHeader("Content-Disposition", partInfo.contentDispositionHeaderValue());

        return partInfo.contentType.equals("text/html") && !partInfo.attachment?
                new HtmlEscaperReader(Charset.forName(partInfo.charset), part.getInputStream(), Utils.hrefMappings(part, path)):
                part.getInputStream();
    }

    private static InputStream getMultipart(PartInfo partInfo, Multipart multipart, HttpServletResponse response, String path)
            throws IOException, MessagingException {
        switch(partInfo.contentType) {
            case "multipart/alternative":
                return getMultipartAlternative(multipart, response, path);
            default:
                return getMultipartMixed(multipart, response, path);
        }

    }

    private static InputStream getMultipartAlternative(Multipart multipart, HttpServletResponse response, String path)
            throws MessagingException, IOException {
        int textPlain = 0;
        PartInfo textPlainPartInfo = null;
        int count = multipart.getCount();
        for(int i = 0; i < count; i++) {
            BodyPart part = multipart.getBodyPart(i);
            PartInfo partInfo = Utils.getSinglePartInfo(part, path);
            switch (partInfo.contentType) {
                case "text/html":
                    if(path != null) path += "," + i;
                    return getSinglePart(partInfo, part, response, path);
                case "text/plain":
                    textPlain = i;
                    textPlainPartInfo = partInfo;
                    break;
                default:
                    if(partInfo.isMultipart) {
                        if(path != null) path += "," + i;
                        return getMultipart(partInfo, (Multipart)part.getContent(), response, path);
                    }
            }
        }
        if(path != null) path += "," + textPlain;
        return getSinglePart(textPlainPartInfo, multipart.getBodyPart(textPlain), response, path);
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
