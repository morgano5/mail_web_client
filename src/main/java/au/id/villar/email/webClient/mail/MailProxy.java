package au.id.villar.email.webClient.mail;

import org.apache.commons.net.imap.IMAP;
import org.apache.commons.net.imap.IMAPSClient;

import java.io.IOException;
import java.util.Map;

public class MailProxy {

    private IMAPSClient client;
    private String username;
    private String password;
    private String hostname;

    private String currentFolder;
    private boolean readOnly;
    private int totalMessages;
    private int newMessages;
    private int unreadMessages; // TODO get this value from somewhere
    private int uidValidity;
    private int nextUid;

    public int getTotalMessages() {
        return totalMessages;
    }

    public int getNewMessages() {
        return newMessages;
    }

    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void configure(Map<String, Object> properties) throws IOException {
        if(client != null) {
            close();
            client = null;
        }
        username = (String)properties.get("username");
        password = (String)properties.get("password");
        hostname = (String)properties.get("hostname");
        currentFolder = null;
    }

    public boolean credentialsOk() throws IOException {
        return connectIfNeeded();
    }

    public void setCurrentFolder(String name, boolean readOnly) throws IOException {

        if(name == null) {
            if(currentFolder != null) {
                if(client.isConnected()) client.close();
                currentFolder = null;
            }
            return;
        }

        checkConected();
        boolean success = readOnly? client.examine(name): client.select(name);
        String outcome = client.getReplyString();
        if(!success)
            throw new IOException("Error selecting folder: " + outcome.substring(outcome.indexOf("NO") + 3).trim());

        currentFolder = name;
        this.readOnly = readOnly;

        parseSelectResponse();
    }

    public void close() throws IOException {
        if(client.isConnected()) {
            client.logout();
            client.disconnect();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    private void checkConected() throws IOException {
        if(!connectIfNeeded()) throw new IOException("Bad credentials");
    }

    private boolean connectIfNeeded() throws IOException {
        if(client == null) client = new IMAPSClient("SSL", true);
        if(!client.isConnected()) client.connect(hostname);
        return client.getState() == IMAP.IMAPState.AUTH_STATE || client.login(username, password);
    }

    private void parseSelectResponse() throws IOException {
        for(String line: client.getReplyStrings()) {
            if(line.length() < 3) badLine(line);
            if(line.charAt(0) != '*') continue;
            char hint = line.charAt(2);
            switch(hint) {
                case 'O':
                    parseSelectOkResponse(line);
                    break;
                case 'F':
                    parseSelectFlagsResponse(line);
                    break;
                default:
                    parseSelectSizeResponse(line);
            }
        }
    }

    private void parseSelectOkResponse(String line) throws IOException {
        if(!follows("OK ", line, 2) || line.length() < 6 || line.charAt(5) != '[') badLine(line);
        int limitIndex = line.indexOf(']', 2);
        if(limitIndex == -1) badLine(line);

        if(follows("UNSEEN", line, 6)) {
            System.out.format("First unseen: %d%n", readNumber(line, 13, limitIndex)); // TODO
            return;
        }

        if(follows("UIDVALIDITY", line, 6)) {
            uidValidity = readNumber(line, 18, limitIndex);
            return;
        }

        if(follows("UIDNEXT", line, 6)) {
            nextUid = readNumber(line, 14, limitIndex);
            return;
        }

        warningLine(line);
    }

    private void parseSelectFlagsResponse(String line) throws IOException {
        if(!follows("FLAGS ", line, 2)) badLine(line);
        warningLine(line);
    }

    private void parseSelectSizeResponse(String line) throws IOException {
        int spaceIndex = line.indexOf(' ', 2);
        if(spaceIndex == -1) badLine(line);
        int number = readNumber(line, 2, spaceIndex);

        if(follows("EXISTS", line, spaceIndex + 1) && line.endsWith("EXISTS")) {
            this.totalMessages = number;
            return;
        }

        if(follows("RECENT", line, spaceIndex + 1) && line.endsWith("RECENT")) {
            this.newMessages = number;
            return;
        }

        warningLine(line);
    }

    private int readNumber(String line, int fromIndex, int toIndex) throws IOException {
        int number = 0;
        int pos = fromIndex;
        char ch;
        while(pos < line.length() && pos < toIndex && (ch = line.charAt(pos++)) >= '0' && ch <= '9')
            number = number * 10 + ch - '0';
        if(pos == fromIndex || pos != toIndex) badLine(line);
        return number;
    }

    private boolean follows(String constant, String line, int fromIndex) {
        if(constant.length() > line.length() - fromIndex) return false;
        for(int index = 0; index < constant.length(); index++)
            if(constant.charAt(index) != line.charAt(fromIndex++)) return false;
        return true;
    }

    private void badLine(String line) throws IOException {
        throw new IOException("Bad line from IMAP server: " + line);
    }

    private void warningLine(String line) {
        // TODO find a way to log a warning instead of throwing an error here
        System.out.format("WARNING skipping line: %s%n", line);
    }
}
