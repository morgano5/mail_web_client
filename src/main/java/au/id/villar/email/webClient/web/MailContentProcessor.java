package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.mail.HtmlEscaperReader;
import au.id.villar.email.webClient.mail.MailMessage;
import au.id.villar.email.webClient.mail.MailPart;
import au.id.villar.email.webClient.mail.Mailbox;
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
        try {

            processMailReference(mailReference, (mailPart, part, path) -> {

                if(mailPart == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                response.addHeader("Content-Type", mailPart.contentTypeHeaderValue());
                response.addHeader("Content-Disposition", mailPart.contentDispositionHeaderValue());

                InputStream input = mailPart.contentType.equals("text/html") && !mailPart.attachment?
                        new HtmlEscaperReader(Charset.forName(mailPart.charset), part.getInputStream(), MailPart.hrefMappings(part, path)):
                        part.getInputStream();

                OutputStream output = response.getOutputStream();
                transfer(input, output);

            });

        } catch (MessagingException | IOException e) {
            LOG.error("Error getting content: " + e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    List<MailPart> getAttachments(String mailReference) throws MessagingException, IOException {
        List<MailPart> attachments = new ArrayList<>();

        List<Object> tokens = MailMessage.getTokens(mailReference);
        String messageId = tokens.get(0).toString();

        mailbox.processMessage(messageId, message -> attachments.addAll(MailPart.getAttachments(message, messageId)));
        return attachments;
    }

    private void processMailReference(String mailReference, MailPartProcessor processor)
            throws MessagingException, IOException {

        List<Object> tokens = MailMessage.getTokens(mailReference);
        if(tokens == null) {
            processor.processPart(null, null, null);
            return;
        }
        String mailMessageId = (String)tokens.get(0);
        boolean isRequestForMainContent = tokens.size() == 1;
        String path = isRequestForMainContent? mailReference: null;

        boolean found = mailbox.processMessage(mailMessageId, message -> {

            Part part = message;
            for(int i = 1; i < tokens.size(); i++) {
                MailPart mailPart = MailPart.getSinglePartInfo(part, path);
                if(!mailPart.isMultipart) {
                    processor.processPart(null, null, null);
                    return;
                }
                int partNumber = (Integer)tokens.get(i);
                Multipart multipart = (Multipart)part.getContent();
                if(multipart.getCount() - 1 < partNumber) {
                    processor.processPart(null, null, null);
                    return;
                }
                part = multipart.getBodyPart(partNumber);
            }

            processPart(part, path, processor::processPart);
        });

        if(!found) processor.processPart(null, null, null);
    }

    private static void processPart(Part part, String path, MailPartProcessor processor)
            throws MessagingException, IOException {

        MailPart mailPart = MailPart.getSinglePartInfo(part, path);

        if(mailPart.isMultipart) {
            processMultipart(mailPart, (Multipart)part.getContent(), path, processor);
        } else {
            processor.processPart(mailPart, part, path);
        }
    }

    private static void processMultipart(MailPart mailPart, Multipart multipart, String path,
            MailPartProcessor processor) throws IOException, MessagingException {
        switch(mailPart.contentType) {
            case "multipart/alternative":
                processMultipartAlternative(multipart, path, processor);
                break;
            default:
                processMultipartMixed(multipart, path, processor);
        }

    }

    private static void processMultipartAlternative(Multipart multipart, String path, MailPartProcessor processor)
            throws MessagingException, IOException {
        int textPlain = 0;
        MailPart textPlainMailPart = null;
        int count = multipart.getCount();
        for(int i = 0; i < count; i++) {
            BodyPart part = multipart.getBodyPart(i);
            MailPart mailPart = MailPart.getSinglePartInfo(part, path);
            switch (mailPart.contentType) {
                case "text/html":
                    if(path != null) path += "," + i;
                    processor.processPart(mailPart, part, path);
                    return;
                case "text/plain":
                    textPlain = i;
                    textPlainMailPart = mailPart;
                    break;
                default:
                    if(mailPart.isMultipart) {
                        if(path != null) path += "," + i;
                        processMultipart(mailPart, (Multipart)part.getContent(), path, processor);
                        return;
                    }
            }
        }
        if(path != null) path += "," + textPlain;
        processor.processPart(textPlainMailPart, multipart.getBodyPart(textPlain), path);
    }

    private static void processMultipartMixed(Multipart multipart, String path, MailPartProcessor processor)
            throws MessagingException, IOException {
        // TODO verify that the main part will always be #0
        if(path != null) path += ",0";
        processPart(multipart.getBodyPart(0), path, processor);
    }

    private static void transfer(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }


    private static void send() {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO,
                new InternetAddress(to));

        message.setSubject("Peace ", "UTF-8");
// Create the message part
        BodyPart messageBodyPart = new MimeBodyPart();

// Fill the message body
        messageBodyPart.setContent("Hello attachment", "text/html; charset=UTF-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        String[] filename = {"C:/Users/Dake/Desktop/music.mp3"};
        for (int i = 0; i < filename.length; i++) {
            // Part two is attachment
            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(filename[i]);
            messageBodyPart.setDataHandler(new DataHandler(source));
            // messageBodyPart.setFileName(filename[i]);
            //messageBodyPart.setHeader("Content-Type", "text/plain; charset=UTF-8; name="+MimeUtility.encodeText(filename[i]));
            // messageBodyPart.setHeader("Content-ID", filename[i]);
            messageBodyPart.setFileName(MimeUtility.encodeText(filename[i], "UTF-8", null)); //encode filename
            //bodyPart.setFileName(MimeUtility.encodeText(attachment.getName(), "UTF-8", null));
            multipart.addBodyPart(messageBodyPart);
        }
        // Put parts in message
        message.setContent(multipart);
        //set the time
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:MM:SS");
        Date date = new Date();
        String sentDate = df.format(date);
        Date dd = (Date) df.parse(sentDate);
        message.setSentDate(date);

        // Send the message
        Transport.send(message);
        System.out.println("Message sent...");
        // Copy message to "Sent Items" folder as read
        Store store = session.getStore("imap");
        store.connect(host, "user", "userpwd");

        Folder folder = (Folder) store.getFolder("Sent");
        if (!folder.exists()) {
            folder.create(Folder.HOLDS_MESSAGES);
        }
        folder.open(Folder.READ_WRITE);
        System.out.println("appending...");
        // folder.appendMessages(new Message[]{message});
        try {

            folder.appendMessages(new Message[]{message});
            // Message[] msgs = folder.getMessages();
            message.setFlag(FLAGS.Flag.RECENT, true);

        } catch (Exception ignore) {
            System.out.println("error processing message " + ignore.getMessage());
        } finally {
            store.close();
            // folder.close(false);
        }
    }


    @FunctionalInterface
    private interface MailPartProcessor {
        void processPart(MailPart mailPart, Part part, String path) throws IOException, MessagingException;
    }
}
