package au.id.villar.email.webClient.mail;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

import javax.mail.*;
import javax.mail.search.SearchTerm;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

            populateMessages(folder, mailFolder, startingPageIndex, pageLength);
        });

        return mailFolder;
    }

    public MailFolder getFolder(String fullFolderName, int startingPageIndex, int pageLength)
            throws MessagingException {

        MailFolder mailFolder = new MailFolder();

        runWithStore(store -> {
            IMAPFolder folder = (IMAPFolder)store.getFolder(
                    new URLName("imap", host, 993, fullFolderName, username, password));
            populateMailFolder(folder, mailFolder);
            populateMessages(folder, mailFolder, startingPageIndex, pageLength);
        });

        return mailFolder;
    }

    public MailFolder[] getSubFolders(String fullFolderName) throws MessagingException {

        ObjectHolder<MailFolder[]> holder = new ObjectHolder<>();

        runWithStore(store -> {
            Folder folder = store.getFolder(new URLName("imap", host, 993, fullFolderName, username, password));
            holder.obj = getSubFolders(folder, null);
        });

        return holder.obj;
    }

    public boolean transferMainContent(String mailMessageId, OutputStream output) throws MessagingException {
        String fullFolderName = MailMessage.extractFolder(mailMessageId);
        long uid = MailMessage.extractUID(mailMessageId);
        ObjectHolder<Boolean> found = new ObjectHolder<>();

        found.obj = false;
        runWithStore(store -> {
            IMAPFolder folder = (IMAPFolder)store.getFolder(fullFolderName);
            if(folder == null) return;
            runWithFolder(folder, Folder.READ_ONLY, false, f -> {
                Message message =  f.getMessageByUID(uid);
                if(message == null) return;
                found.obj = true;
                try {
                    InputStream input = message.getInputStream();
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
            });
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

    private void populateMessages(IMAPFolder folder, MailFolder mailFolder, int startingPageIndex, int pageLength)
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
                    IMAPMessage message = (IMAPMessage)messages[i];
                    mailMessages[i] = new MailMessage(mailFolder.getFullName(), folder.getUID(message));
                    Address[] from = message.getFrom();
                    String[] mailFrom = new String[from.length];
                    for (int j = 0; j < from.length; j++) mailFrom[j] = from[j].toString();
                    mailMessages[i].setFrom(mailFrom);
                    mailMessages[i].setSubject(message.getSubject());
                    mailMessages[i].setSentDate(message.getSentDate());
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

    private void runWithFolder(IMAPFolder folder, int mode, boolean expunge, FolderOperation operation)
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

    @FunctionalInterface private interface StoreOperation { void doOperation(Store store) throws MessagingException; }

    @FunctionalInterface private interface FolderOperation { void doOperation(IMAPFolder folder) throws MessagingException; }

    private static class Search {
        static SearchTerm byMessageId(String messageId) {
            return new MessageIdSearchTerm(messageId);
        }
    }

    private static class MessageIdSearchTerm extends SearchTerm {

        private String id;

        MessageIdSearchTerm(String id) {
            this.id = id;
        }

        @Override
        public boolean match(Message message) {
            try {
                String[] data = message.getHeader("Message-ID");
                return data != null && data.length > 0 && data[0].equals(id);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class ObjectHolder<T> { private T obj; }

}
