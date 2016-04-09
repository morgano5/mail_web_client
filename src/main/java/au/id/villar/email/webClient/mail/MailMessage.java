package au.id.villar.email.webClient.mail;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Date;

public class MailMessage {

    private final String id;
    private String[] from;
    private String subject;
    private Date sentDate;

    public MailMessage(String fullFolderName, long uid) {
        id = escape(fullFolderName) + '_' + uid;
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

    public static String extractFolder(String id) {
        return unescape(id.substring(0, id.indexOf('_')));
    }

    public static long extractUID(String id) {
        return Long.valueOf(id.substring(id.indexOf('_') + 1));
    }

    private static String escape(String str) {
        return Base64.getUrlEncoder().encodeToString(str.getBytes(Charset.forName("UTF-8")));
    }

    private static String unescape(String str) {
        return new String(Base64.getUrlDecoder().decode(str), Charset.forName("UTF-8"));
    }

}
