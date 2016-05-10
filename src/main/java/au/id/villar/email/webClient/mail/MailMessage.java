package au.id.villar.email.webClient.mail;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class MailMessage {

    static final char SEPARATOR = ',';

    private final String id;
    private String[] from;
    private String subject;
    private Date sentDate;
    private boolean seen;
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

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
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

    public static List<Object> getTokens(String mailReference) {
        int charPos = mailReference.indexOf(MailMessage.SEPARATOR);
        if(charPos == -1) return null;
        charPos = mailReference.indexOf(MailMessage.SEPARATOR, charPos + 1);
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

    private static String escape(String str) {
        return Base64.getUrlEncoder().encodeToString(str.getBytes(Charset.forName("UTF-8")));
    }

    private static String unescape(String str) {
        return new String(Base64.getUrlDecoder().decode(str), Charset.forName("UTF-8"));
    }

}
