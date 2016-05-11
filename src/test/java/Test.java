import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.Base64;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.mail.imap.*;
import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.net.imap.IMAPSClient;
import org.apache.commons.net.pop3.POP3MessageInfo;

public class Test {

	public static void main2(String[] args) throws Exception {

		String username = "";
		String password = "";

//		System.out.println(">>> " + Crypt.crypt("zzz", "$6$7cde7fce7d89f60b"));

		String salt = String.format("$6$%016X", new java.util.Random().nextLong());
		//Class type = ;// ClassNotFoundException
		String hash = Class.forName("org.apache.commons.codec.digest.Crypt").getMethod("crypt", String.class, String.class).invoke(null, password, salt).toString();
		System.out.println(">>> " + /*Crypt.crypt(password, salt)*/hash);

	}

	public static void main3(String[] args) {
		byte[] raw = new byte[] {0, 0, 63};
		System.out.println(">> " + Base64.getUrlEncoder().encodeToString(raw));
	}

	public static void main4(String[] args) throws IOException {

		IMAPSClient client = new IMAPSClient("SSL", true);

		client.connect("mail.villar.id.au");
		System.out.println("<<< CONNECT");
		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);

		client.login("", "");
		System.out.println("<<< LOGIN");
		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);

		client.select("INBOX");
		System.out.println("<<< SELECT");
		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);

//		client.select("Archivo");
//		System.out.println("<<< SELECT");
//		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);

		client.list("\"\"", "\"INBOX.*\"");
		System.out.println("<<< LIST");
		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);

//		client.fetch("1:136", "(FLAGS BODY[HEADER.FIELDS (DATE SUBJECT FROM)])");
//		System.out.println("<<< FETCH");
//		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);

		client.disconnect();
		System.out.println("<<< DISCONNECT");
		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);

	}

	private static final Pattern RESPONSE_LINE_PATTERN = Pattern.compile("([^ ]+) +([^ ]+) +(.*)\r\n");

	public static void main(String[] args) throws IOException {

		String headerOrder = "DATE";

		IMAPSClient client = new IMAPSClient("SSL", true);
		client.connect("mail.villar.id.au");
		client.login("", "");

		client.select("INBOX");
		String selectResult = client.getReplyString();

		int total = -1;
		int recent = 0;

		Matcher matcher = RESPONSE_LINE_PATTERN.matcher(selectResult);
		while(matcher.find()) {

			if(!matcher.group(1).equals("*")) break;

			if(matcher.group(3).equals("EXISTS")) {
				total = Integer.valueOf(matcher.group(2));
				continue;
			}

			if(matcher.group(3).equals("RECENT")) {
				recent = Integer.valueOf(matcher.group(2));
			}

		}


		client.fetch("4,8,6"/*"1:" + total*/, "(FLAGS BODY[HEADER.FIELDS (" + headerOrder + ")])");
		System.out.println("<<< FETCH");
		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);


		client.logout();
		client.disconnect();


		System.out.println("TOTAL: " + total + ", RECENT: " + recent);
	}

}
