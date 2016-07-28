package au.id.villar.email.webClient.mail;

@Deprecated
public interface MailboxService {

    Mailbox getMailbox(String username, String password);

}
