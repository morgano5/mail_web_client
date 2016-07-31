package au.id.villar.email.webClient.mail;

import org.apache.commons.net.imap.IMAP;
import org.apache.commons.net.imap.IMAPSClient;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailProxy {

    private static final Pattern RESPONSE_LINE_PATTERN = Pattern.compile("^(\\*|\\+|[^ ]++) ++([^ ]++) *+(.*+)$");
    private static final Pattern STATUS_RESPONSE_PATTERN = Pattern.compile("^\\[([^] ]++)( ++[^]]++)?\\]");

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

    private long lastFetch;
    private boolean continuationRequest;
    private int lastUnseen;

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
        if(success) {
            this.currentFolder = name;
            this.readOnly = readOnly;
            parseResponse();

            success = client.search("UNSEEN");
        }
        if(success) {
            for(String line: client.getReplyStrings()){
                int start = line.indexOf("SEARCH");
                if(start > -1) {
                    boolean inNumber = false;
                    int total = 0;
                    for(int i = start + 7; i < line.length(); i++) {
                        char ch = line.charAt(i);
                        if(!inNumber && ch >= '0' && ch <= '9') {
                            inNumber = true;
                            total++;
                        } else if(inNumber && (ch < '0' || ch > '9')) {
                            inNumber = false;
                        }
                    }
                    this.unreadMessages = total;
                }
            }
        }
        if(!success) {
            String outcome = client.getReplyString();
            throw new IOException("Error selecting folder: " + outcome.substring(outcome.indexOf("NO") + 3).trim());
        }

    }

    public void TEMP() throws IOException { // TODO
        client.noop();
        for(String line: client.getReplyStrings()) System.out.println("NOOP - " + line); // TODO
    }

    public void TEMP2() throws IOException { // TODO
        client.fetch("" + totalMessages, "(FLAGS INTERNALDATE BODY[HEADER.FIELDS (DATE FROM SUBJECT)])");
        for(String line: client.getReplyStrings()) System.out.println("FETCH - " + line); // TODO
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

    private void parseResponse() throws IOException {
        continuationRequest = false;
        for(String line: client.getReplyStrings()) parseLine(line);
    }






    private void parseLine(String line) throws IOException {
        int responseStart = line.indexOf(' ');
        if(responseStart < 0) badLine(line);

        Matcher matcher = RESPONSE_LINE_PATTERN.matcher(line);
        if(!matcher.matches()) badLine(line);

        String type = matcher.group(1);

        if("+".equals(type)) {
            continuationRequest = true;
            return;
        }

        processCommand(matcher.group(2), matcher.group(3), !"*".equals(type));

    }

    private void processCommand(String firstAtom, String restOfLine, boolean tagged) throws IOException {

        switch(firstAtom) {
            case "OK":
                parseOkResponse(restOfLine, tagged);
                break;
            case "FLAGS":
                parseFlagsResponse(restOfLine, tagged);
                break;
            default:
                if(isNumber(firstAtom)) {
                    int number = readNumber(firstAtom);
                    parseSizeResponse(number, restOfLine);
                } else {
                    warningLine(firstAtom + " " + restOfLine);
                }
        }

    }

    private void parseOkResponse(String restOfLine, boolean tagged) throws IOException {

        if(tagged) { warningLine("TAG OK " + restOfLine); return; } // TODO

        Matcher matcher = STATUS_RESPONSE_PATTERN.matcher(restOfLine);
        if(!matcher.find()) return;

        switch(matcher.group(1)) {
            case "UNSEEN":
                lastUnseen = readNumber(matcher.group(2).trim());
                return;
            case "UIDVALIDITY":
                uidValidity = readNumber(matcher.group(2).trim());
                return;
            case "UIDNEXT":
                nextUid = readNumber(matcher.group(2).trim());
                return;
        }

        warningLine("* OK " + restOfLine);
    }

    private void parseFlagsResponse(String line, boolean tagged) throws IOException {
        // TODO
        warningLine("FLAGS " + line);
    }

    private void parseSizeResponse(int number, String type) throws IOException {
        // TODO

        if("EXISTS".equals(type)) {
            this.totalMessages = number;
            return;
        }

        if("RECENT".equals(type)) {
            this.newMessages = number;
            return;
        }

        warningLine(number + ' ' + type);
    }

    private boolean isNumber(String strNumber) {
        char ch;
        for(int pos = 0; pos < strNumber.length(); pos++)
            if((ch = strNumber.charAt(pos)) < '0' || ch > '9') return false;
        return true;
    }

    private int readNumber(String strNumber) throws IOException {
        int number = 0;
        int pos = 0;
        char ch;
        while(pos < strNumber.length() && (ch = strNumber.charAt(pos++)) >= '0' && ch <= '9')
            number = number * 10 + ch - '0';
        if(pos < strNumber.length()) throw new IOException("bad number: \"" + strNumber + "\"");
        return number;
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
