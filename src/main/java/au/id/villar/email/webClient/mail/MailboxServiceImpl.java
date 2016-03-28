package au.id.villar.email.webClient.mail;

import javax.mail.Session;
import java.util.Properties;

public class MailboxServiceImpl implements MailboxService {

    private Session session;
    private String host;

    public MailboxServiceImpl(String host, Properties config) {
        this.host = host;
        Properties sessionConfig = new Properties(config != null? config: new Properties());
        sessionConfig.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        sessionConfig.setProperty("mail.imap.socketFactory.port", "993");
        sessionConfig.setProperty("mail.imap.socketFactory.fallback", "false");
        session = Session.getDefaultInstance(sessionConfig);
    }

    @Override
    public Mailbox getMailbox(String username, String password) {
        return new Mailbox(username, password, host, session);
    }

}
