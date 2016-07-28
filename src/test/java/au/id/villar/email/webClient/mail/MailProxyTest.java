package au.id.villar.email.webClient.mail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MailProxyTest {

    public static void main(String[] args) throws IOException {
        MailProxy mail = new MailProxy();

        Map<String, Object> properties = new HashMap<>();
        properties.put("username", "");
        properties.put("password", "");
        properties.put("hostname", "");

        mail.configure(properties);

        mail.setCurrentFolder("INBOX", true);

        System.out.format("Total: %d%nNew: %d%nUnread: %d%n",
                mail.getTotalMessages(), mail.getNewMessages(), mail.getUnreadMessages());

        mail.close();
    }

}
