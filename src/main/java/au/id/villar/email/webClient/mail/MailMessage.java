package au.id.villar.email.webClient.mail;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Date;

public class MailMessage {

    public static final char SEPARATOR = ',';

    private final String id;
    private String[] from;
    private String subject;
    private Date sentDate;
    private boolean withAttachments;

    public MailMessage(String fullFolderName, long uid) {
        id = escape(fullFolderName) + SEPARATOR + uid;
    }

    public String getId() {
        return id;
    }

    public String[] getFrom() {
        return from;
    }

    public void setFrom(String[] from) {
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    public boolean isWithAttachments() {
        return withAttachments;
    }

    public void setWithAttachments(boolean hasAttachments) {
        this.withAttachments = hasAttachments;
    }

    public static String extractFolder(String id) {
        return unescape(id.substring(0, id.indexOf(SEPARATOR)));
    }

    public static long extractUID(String id) {
        return Long.valueOf(id.substring(id.indexOf(SEPARATOR) + 1));
    }

    public static boolean attachmentsPresent(Message message) throws IOException, MessagingException {
        if(!Utils.isMultipart(message.getContentType())) return false;
        Multipart multipart = (Multipart)message.getContent();
        int count = multipart.getCount();
        for(int x = 0; x < count; x++) {
            if(MailPart.attachmentsPresent(multipart.getBodyPart(x))) return true;
        }
        return false;
    }

    private static String escape(String str) {
        return Base64.getUrlEncoder().encodeToString(str.getBytes(Charset.forName("UTF-8")));
    }

    private static String unescape(String str) {
        return new String(Base64.getUrlDecoder().decode(str), Charset.forName("UTF-8"));
    }

}
