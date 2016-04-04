package au.id.villar.email.webClient.mail;

import javax.mail.*;

public class Mailbox {

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
            throws MessagingException {
        MailFolder mailFolder = new MailFolder();

        runWithStore(store -> {

            Folder folder = fullFolderName != null && !fullFolderName.isEmpty() ?
                    store.getFolder(new URLName("imap", host, 993, fullFolderName, username, password)):
                    store.getDefaultFolder();
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

            populateMessages(folder, mailFolder, startingPageIndex, pageLength);
        });

        return mailFolder;
    }

    public MailFolder getFolder(String fullFolderName, int startingPageIndex, int pageLength)
            throws MessagingException {

        MailFolder mailFolder = new MailFolder();

        runWithStore(store -> {
            Folder folder = store.getFolder(new URLName("imap", host, 993, fullFolderName, username, password));
            populateMailFolder(folder, mailFolder);
            populateMessages(folder, mailFolder, startingPageIndex, pageLength);
        });

        return mailFolder;
    }

    public MailFolder[] getSubFolders(String fullFolderName) throws MessagingException {

        class ObjectHolder<T> { private T obj; }
        ObjectHolder<MailFolder[]> holder = new ObjectHolder<>();

        runWithStore(store -> {
            Folder folder = store.getFolder(new URLName("imap", host, 993, fullFolderName, username, password));
            holder.obj = getSubFolders(folder, null);
        });

        return holder.obj;
    }

    private void populateMailFolder(Folder folder, MailFolder mailFolder) throws MessagingException {
        mailFolder.setName(folder.getName());
        mailFolder.setFullName(folder.getFullName());
        if((folder.getType() & Folder.HOLDS_MESSAGES) == 0) return;
        mailFolder.setTotalMessages(folder.getMessageCount());
        mailFolder.setNewMessages(folder.getNewMessageCount());
        mailFolder.setUnreadMessages(folder.getUnreadMessageCount());
    }

    private void populateMessages(Folder folder, MailFolder mailFolder, int startingPageIndex, int pageLength)
            throws MessagingException {
        if((folder.getType() & Folder.HOLDS_MESSAGES) != 0) runWithFolder(folder, Folder.READ_ONLY, false, f -> {

            int start = startingPageIndex * pageLength + 1;
            int end = start + pageLength - 1;
            if (end > mailFolder.getTotalMessages()) end = mailFolder.getTotalMessages();
            MailMessage[] mailMessages;
            if (start > end) {
                mailMessages = new MailMessage[0];
            } else {
                Message[] messages = f.getMessages(start, end);
                mailMessages = new MailMessage[messages.length];
                for (int i = 0; i < messages.length; i++) {
                    mailMessages[i] = new MailMessage();
                    Address[] from = messages[i].getFrom();
                    String[] mailFrom = new String[from.length];
                    for (int j = 0; j < from.length; j++) mailFrom[j] = from[j].toString();
                    mailMessages[i].setFrom(mailFrom);
                    mailMessages[i].setSubject(messages[i].getSubject());
                    mailMessages[i].setSentDate(messages[i].getSentDate());
                }
            }
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

    @FunctionalInterface private interface StoreOperation { void doOperation(Store store) throws MessagingException; }

    @FunctionalInterface private interface FolderOperation { void doOperation(Folder folder) throws MessagingException; }

    private void runWithStore(StoreOperation operation) throws MessagingException {
        MessagingException messagingException = null;
        RuntimeException runtimeException = null;

        Store store = session.getStore("imap");
        try {
            store.connect(host, username, password);
            operation.doOperation(store);
        } catch(MessagingException e) {
            messagingException = e;
        } catch(RuntimeException e) {
            runtimeException = e;
        } finally {
            try {
                store.close();
            } catch(MessagingException e) {
                if(messagingException == null) messagingException = e;
            }
            if(messagingException != null) throw messagingException;
            if(runtimeException != null) throw runtimeException;
        }
    }

    private void runWithFolder(Folder folder, int mode, boolean expunge, FolderOperation operation)
            throws MessagingException {
        MessagingException messagingException = null;
        RuntimeException runtimeException = null;

        try {
            folder.open(mode);
            operation.doOperation(folder);
        } catch(MessagingException e) {
            messagingException = e;
        } catch(RuntimeException e) {
            runtimeException = e;
        } finally {
            try {
                folder.close(expunge);
            } catch(MessagingException e) {
                if(messagingException == null) messagingException = e;
            }
            if(messagingException != null) throw messagingException;
            if(runtimeException != null) throw runtimeException;
        }
    }
}
