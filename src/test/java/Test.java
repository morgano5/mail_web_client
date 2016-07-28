import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import java.io.IOException;
import java.net.Inet4Address;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
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
//	private static final Pattern FETCH_LINE_PATTERN = Pattern.compile("\\* ([0-9]+) FETCH .\\)\r\n");
//	private static final Pattern DATE_PATTERN = Pattern.compile("(?:[A-Za-z]{3}, )?([0-9]+ [A-Za-z]{3} [0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2} [+-][0-9]{4})");

	public static void main(String[] args) throws IOException, ParseException {

		long start = System.currentTimeMillis();

		String reply;

		HeaderProcessor processor = new DateProcessor("DATE");

		IMAPSClient client = new IMAPSClient("SSL", true);
		client.connect("mail.villar.id.au");
		System.out.println("CONNECT: " + client.getReplyString());
		boolean result = client.login("", "");
		System.out.println("LOGIN: (" + result + ") " + client.getReplyString());

		client.select("INBOX");
		reply = client.getReplyString();
		System.out.println("SELECT |" + reply);

		int total = -1;
		int recent = 0;

		Matcher matcher = RESPONSE_LINE_PATTERN.matcher(reply);
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


		// DATE FROM SUBJECT
		client.fetch("1:" + total, "(FLAGS BODY[HEADER.FIELDS (DATE FROM SUBJECT" + /*processor.getHeaderName() +*/ ")])");

		result = client.select("INBOX");
		System.out.println("SELECT: (" + result + ") " + client.getReplyString());
//		String selectResult = client.getReplyString();
//
//		int total = -1;
//		int recent = 0;
//
//		Matcher matcher = RESPONSE_LINE_PATTERN.matcher(selectResult);
//		while(matcher.find()) {
//
//			if(!matcher.group(1).equals("*")) break;
//
//			if(matcher.group(3).equals("EXISTS")) {
//				total = Integer.valueOf(matcher.group(2));
//				continue;
//			}
//
//			if(matcher.group(3).equals("RECENT")) {
//				recent = Integer.valueOf(matcher.group(2));
//			}
//
//		}
//
//
//		// DATE FROM SUBJECT
//		client.fetch("1:" + total, "(FLAGS BODY[HEADER.FIELDS (DATE FROM SUBJECT" + /*processor.getHeaderName() +*/ ")])");
//
//		System.out.println("<<< FETCH");
////		SimpleDateFormat dateFormatter = new SimpleDateFormat("d MMM yyyy HH:mm:ss Z");
////		for(String line: client.getReplyStrings()) {
////			if(line.startsWith("*")) {
////
////			} else if (line.isEmpty() || line.equals(")")) {
////
////			} else {
////
////
////
////				String value = line.substring(line.indexOf(':') + 1).trim();
////				char ch = value.charAt(0);
////				if(ch < '0' || ch > '9') value = value.substring(value.indexOf(' ') + 1);
////				if(value.charAt(value.length() - 1) == ')') value = value.substring(0, value.lastIndexOf(' '));
////				System.out.println(">>>> " + dateFormatter.parse(value));
////			}
////		}
//
//		System.out.println(">>> " + client.getReplyString());
//
//		//Thu, 18 Sep 2014 20:43:53 +0000 (UTC)
////		for(String reply: client.getReplyStrings()) System.out.println(">>> " + reply);


		result = client.logout();
		System.out.println("LOGOUT: (" + result + ") " + client.getReplyString());
		client.disconnect();
		System.out.println("DISCONNECT: " + client.getReplyString());


		System.out.println("TOTAL: " + total + ", RECENT: " + recent);
		System.out.println(">>" + (System.currentTimeMillis() - start));
	}


	interface HeaderProcessor<T> {

		String getHeaderName();

		T parseValue(String value);

	}

	static class DateProcessor implements HeaderProcessor<Date> {

		private String headerName;
		private SimpleDateFormat dateFormatter = new SimpleDateFormat();

		public DateProcessor(String headerName) {
			this.headerName = headerName;
		}

		@Override
		public String getHeaderName() {
			return headerName;
		}

		@Override
		public Date parseValue(String value) {
			// TODO implement
			return null;
		}

	}
}
