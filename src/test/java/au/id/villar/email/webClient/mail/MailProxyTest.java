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

        mail.setCurrentFolder("INBOX", false);


//mail.TEMP2();

        System.out.format("Total: %d%nNew: %d%nUnread: %d%n",
                mail.getTotalMessages(), mail.getNewMessages(), mail.getUnreadMessages());



        java.util.GregorianCalendar gc = new java.util.GregorianCalendar();
        gc.set(java.util.Calendar.HOUR_OF_DAY, 21);
        long finishTime = gc.getTimeInMillis();
        while(System.currentTimeMillis() < finishTime) {
            try {
                Thread.sleep(10_000);
                mail.TEMP();
//                mail.TEMP2();
                if(Thread.interrupted()) break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }





        mail.close();
    }

}
