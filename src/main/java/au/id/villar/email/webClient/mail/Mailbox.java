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

    public MailFolder getStartingFolder(final String fullFolderName, final int startingPageIndex, final int pageLength)
            throws MessagingException {
        final MailFolder mailFolder = new MailFolder();

        runWithStore(store -> {

            Folder folder = fullFolderName != null ?
                    store.getFolder(new URLName("imap", host, 993, fullFolderName, username, password)):
                    store.getDefaultFolder();
            populateMailFolder(mailFolder, folder, false);

            mailFolder.setSubFolders(getSubFolders(folder, null));

            Folder parent = folder.getParent();
            MailFolder current = mailFolder;
            while (parent != null) {
                MailFolder mailParent = new MailFolder();
                current.setParent(mailParent);
                populateMailFolder(mailParent, parent, true);
                mailParent.setSubFolders(getSubFolders(parent, current));
                current = mailParent;
                parent = parent.getParent();
            }

            if(!mailFolder.getName().isEmpty()) runWithFolder(folder, Folder.READ_ONLY, false, f -> {

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
        });

        return mailFolder;
    }

    private void populateMailFolder(MailFolder mailFolder, Folder folder, boolean withFull) throws MessagingException {
        mailFolder.setName(folder.getName());
        if(mailFolder.getName().isEmpty()) return;
        mailFolder.setTotalMessages(folder.getMessageCount());
        mailFolder.setNewMessages(folder.getNewMessageCount());
        mailFolder.setUnreadMessages(folder.getUnreadMessageCount());
        if(withFull) mailFolder.setFullName(folder.getFullName());
    }

    private MailFolder[] getSubFolders(Folder folder, MailFolder exclude) throws MessagingException {
        Folder[] subFolders = folder.list();
        MailFolder[] mailSubFolders = new MailFolder[subFolders.length - (exclude != null? 1: 0)];
        int offset = 0;
        for(int i = 0; i < subFolders.length; i++) {
            if(exclude == null || !exclude.getName().equals(subFolders[i].getName())) {
                mailSubFolders[i + offset] = new MailFolder();
                populateMailFolder(mailSubFolders[i + offset], subFolders[i], true);
            } else {
                offset = -1;
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
