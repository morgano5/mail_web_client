package au.id.villar.email.webClient.mail;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.SortTerm;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Map;

public class Mailbox {

    @FunctionalInterface public interface MessageProcess { void process(IMAPMessage message) throws MessagingException, IOException; }

    private String username;
    private String password;
    private String host;
    private Session session;

    Mailbox(String username, String password, String host, Session session) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.session = session;
    }

    public MailFolder getStartingFolder(String fullFolderName, int startingPageIndex, int pageLength)
            throws MessagingException, IOException {
        MailFolder mailFolder = new MailFolder();

        runWithStore(store -> {

            IMAPFolder folder = fullFolderName != null && !fullFolderName.isEmpty() ?
                    (IMAPFolder)store.getFolder(new URLName("imap", host, 993, fullFolderName, username, password)):
                    (IMAPFolder)store.getDefaultFolder();
            populateMailFolder(folder, mailFolder);

            mailFolder.setSubFolders(getSubFolders(folder, null));

            Folder parent = folder.getParent();
            MailFolder current = mailFolder;
            while (parent != null) {
                MailFolder mailParent = new MailFolder();
                current.setParent(mailParent);
                populateMailFolder(parent, mailParent);
                mailParent.setSubFolders(getSubFolders(parent, current));
                current = mailParent;
                parent = parent.getParent();
            }

            populateMessages(folder, mailFolder/*, startingPageIndex, pageLength*/);
        });

        return mailFolder;
    }

    public MailFolder getFolder(String fullFolderName, int startingPageIndex, int pageLength)
            throws MessagingException, IOException {

        MailFolder mailFolder = new MailFolder();

        runWithStore(store -> {
            IMAPFolder folder = (IMAPFolder)store.getFolder(
                    new URLName("imap", host, 993, fullFolderName, username, password));
            populateMailFolder(folder, mailFolder);
            populateMessages(folder, mailFolder/*, startingPageIndex, pageLength*/);
        });

        return mailFolder;
    }

    public MailFolder[] getSubFolders(String fullFolderName) throws MessagingException, IOException {

        ObjectHolder<MailFolder[]> holder = new ObjectHolder<>();

        runWithStore(store -> {
            Folder folder = store.getFolder(new URLName("imap", host, 993, fullFolderName, username, password));
            holder.obj = getSubFolders(folder, null);
        });

        return holder.obj;
    }






//-------------------------- TODO under intensive construction


    public MailMessage send(String mailMessageId) throws IOException, MessagingException {
        ObjectHolder<MimeMessage> messageHolder = new ObjectHolder<>();
        ObjectHolder<MailMessage> mailMessageHolder = new ObjectHolder<>();

        processMessage(mailMessageId, message -> {
            messageHolder.obj = new MimeMessage(message);
        });

        Transport.send(messageHolder.obj, username, password);

        String fullFolderName = "Sent";//MailMessage.extractFolder(mailMessageId);
        runWithFolder(fullFolderName, Folder.READ_WRITE, false, folder -> {
            Message[] result = folder.addMessages(new Message[] {messageHolder.obj});
            mailMessageHolder.obj = result[0] != null? new MailMessage(fullFolderName, folder.getUID(result[0])): null;
        });

        deleteMessage(mailMessageId);

        return mailMessageHolder.obj;

    }

    public MailMessage addMessage(String fullFolderName, Map<String, String> headers, String body)
            throws MessagingException, IOException {

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress("rafael@villar.me"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress("morgano5@gmail.com"));
        message.setSubject("testing subject");

//        MimeBodyPart bodyPart = new MimeBodyPart();
//        bodyPart.setText(body);

        message.addHeader("Content-type", "text/plain");
        message.setText(body);

        ObjectHolder<MailMessage> mailMessageHolder = new ObjectHolder<>();
        runWithFolder(fullFolderName, Folder.READ_WRITE, false, folder -> {
            Message[] result = folder.addMessages(new Message[] {message});
            mailMessageHolder.obj = result[0] != null? new MailMessage(fullFolderName, folder.getUID(result[0])): null;
        });
        return mailMessageHolder.obj;
    }

    public void deleteMessage(String mailMessageId) throws IOException, MessagingException {
        String fullFolderName = MailMessage.extractFolder(mailMessageId);
        long uid = MailMessage.extractUID(mailMessageId);
        runWithFolder(fullFolderName, Folder.READ_WRITE, true, (folder) -> {
            Message message = folder.getMessageByUID(uid);
            if(message == null) return;
            message.setFlag(Flags.Flag.DELETED, true);
        });
    }

    // add folder
    // delete folder
    // modify folder (name, flags, ...)


    // set message flags
    // get message flags
    // move message
    // delete message

    // add draft
    // edit draft
    // delete draft
    // add attachment
    // remove attachment
    // send message

    public boolean processMessage(String mailMessageId, MessageProcess process) throws MessagingException, IOException {
        String fullFolderName = MailMessage.extractFolder(mailMessageId);
        long uid = MailMessage.extractUID(mailMessageId);
        ObjectHolder<Boolean> found = new ObjectHolder<>();

        found.obj = false;
        runWithFolder(fullFolderName, Folder.READ_ONLY, false, (folder) -> {
            IMAPMessage message = (IMAPMessage)folder.getMessageByUID(uid);
            if(message == null) return;
            found.obj = true;
            process.process(message);
        });
        return found.obj;
    }

    private void populateMailFolder(Folder folder, MailFolder mailFolder) throws MessagingException {
        mailFolder.setName(folder.getName());
        mailFolder.setFullName(folder.getFullName());
        if((folder.getType() & Folder.HOLDS_MESSAGES) == 0) return;
        mailFolder.setTotalMessages(folder.getMessageCount());
        mailFolder.setNewMessages(folder.getNewMessageCount());
        mailFolder.setUnreadMessages(folder.getUnreadMessageCount());
    }

    private void populateMessages(IMAPFolder folder, MailFolder mailFolder/*, int startingPageIndex, int pageLength*/)
            throws MessagingException, IOException {
        if((folder.getType() & Folder.HOLDS_MESSAGES) != 0) runWithFolder(folder, Folder.READ_ONLY, false, () -> {

//            int start = startingPageIndex * pageLength + 1;
//            int end = start + pageLength - 1;
//            if (end > mailFolder.getTotalMessages()) end = mailFolder.getTotalMessages();
            MailMessage[] mailMessages;
//            if (start > end) {
//                mailMessages = new MailMessage[0];
//            } else {
                Message[] messages = folder.getSortedMessages(new SortTerm[] {SortTerm.REVERSE, SortTerm.DATE}/*start, end*/);
                mailMessages = new MailMessage[messages.length];
                for (int i = 0; i < messages.length; i++) {
                    IMAPMessage message = (IMAPMessage)messages[i];
                    mailMessages[i] = new MailMessage(mailFolder.getFullName(), folder.getUID(message));
                    Address[] from = message.getFrom();
                    String[] mailFrom = new String[from.length];
                    for (int j = 0; j < from.length; j++) mailFrom[j] = from[j].toString();
                    mailMessages[i].setFrom(mailFrom);
                    mailMessages[i].setSubject(message.getSubject());
                    mailMessages[i].setSentDate(message.getSentDate());
                    mailMessages[i].setWithAttachments(MailMessage.attachmentsPresent(message));
                    Flags flags = message.getFlags();
                    mailMessages[i].setSeen(flags.contains(Flags.Flag.SEEN));
                }
//            }
            mailFolder.setPageMessages(mailMessages);
        });

    }

    private MailFolder[] getSubFolders(Folder folder, MailFolder exclude) throws MessagingException {
        Folder[] subFolders = folder.list();
        MailFolder[] mailSubFolders = new MailFolder[subFolders.length - (exclude != null? 1: 0)];
        int offset = 0;
        for(int i = 0; i < subFolders.length; i++) {
            if(exclude == null || !exclude.getFullName().equals(subFolders[i].getFullName())) {
                mailSubFolders[i + offset] = new MailFolder();
                populateMailFolder(subFolders[i], mailSubFolders[i + offset]);
            } else {
                offset--;
            }
        }
        return mailSubFolders;
    }

    private void runWithFolder(String fullFolderName, int mode, boolean expunge, MailOperation2 operation)
            throws MessagingException, IOException {
        runWithStore(store -> {
            IMAPFolder folder = (IMAPFolder)store.getFolder(fullFolderName);
            if(folder == null) return;
            runWithObject(() -> { folder.open(mode); operation.doOperation(folder); }, () -> folder.close(expunge));

        });
    }

    private void runWithStore(StoreOperation operation) throws MessagingException, IOException {

        Store store = session.getStore("imap");

        runWithObject(() -> { store.connect(host, username, password); operation.doOperation(store); }, store::close);

    }

    private void runWithFolder(IMAPFolder folder, int mode, boolean expunge, MailOperation operation)
            throws MessagingException, IOException {

        runWithObject(() -> { folder.open(mode); operation.doOperation(); }, () -> folder.close(expunge));
    }

    private void runWithObject(MailOperation operation, MailClosing close)
            throws MessagingException, IOException {
        MessagingException messagingException = null;
        IOException ioException = null;
        RuntimeException runtimeException = null;

        try {
            operation.doOperation();
        } catch(MessagingException e) {
            messagingException = e;
        } catch(IOException e) {
            ioException = e;
        } catch(RuntimeException e) {
            runtimeException = e;
        } finally {
            try {
                close.doClose();
            } catch(MessagingException e) {
                if(messagingException == null) messagingException = e;
            }
            if(messagingException != null) throw messagingException;
            if(ioException != null) throw ioException;
            if(runtimeException != null) throw runtimeException;
        }
    }

    @FunctionalInterface private interface StoreOperation { void doOperation(Store store) throws MessagingException, IOException; }

    @FunctionalInterface private interface MailOperation { void doOperation() throws MessagingException, IOException; }

    @FunctionalInterface private interface MailOperation2 { void doOperation(IMAPFolder folder) throws MessagingException, IOException; }

    @FunctionalInterface private interface MailClosing { void doClose() throws MessagingException; }

    private class ObjectHolder<T> { private T obj; }

}
