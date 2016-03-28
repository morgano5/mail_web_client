package au.id.villar.email.webClient.mail;

public class MailFolder {

    private String name;
    private String fullName;
    private int totalMessages;
    private int newMessages;
    private int unreadMessages;

    private MailFolder parent;
    private MailFolder[] subFolders;
    private MailMessage[] pageMessages;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }

    public int getNewMessages() {
        return newMessages;
    }

    public void setNewMessages(int newMessages) {
        this.newMessages = newMessages;
    }

    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(int unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public MailFolder getParent() {
        return parent;
    }

    public void setParent(MailFolder parent) {
        this.parent = parent;
    }

    public MailFolder[] getSubFolders() {
        return subFolders;
    }

    public void setSubFolders(MailFolder[] subFolders) {
        this.subFolders = subFolders;
    }

    public MailMessage[] getPageMessages() {
        return pageMessages;
    }

    public void setPageMessages(MailMessage[] pageMessages) {
        this.pageMessages = pageMessages;
    }
}
