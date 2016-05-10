import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.Base64;
import java.util.Properties;

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

	public static void main(String[] args) throws IOException {

		IMAPSClient client = new IMAPSClient("SSL", true);

		client.connect("mail.villar.id.au");
		System.out.println("<<< CONNECT");
		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);

		client.login("", "");
		System.out.println("<<< LOGIN");
		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);

		client.select("INBOX.Archivo");
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
}
