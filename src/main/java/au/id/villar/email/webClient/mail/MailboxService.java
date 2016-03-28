package au.id.villar.email.webClient.mail;

public interface MailboxService {

    Mailbox getMailbox(String username, String password);

}
