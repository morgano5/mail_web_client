import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import java.util.Properties;

import com.sun.mail.imap.*;
import org.apache.commons.codec.digest.Crypt;

public class Test {

	public static void main(String[] args) throws Exception {

		String username = "";
		String password = "test";


		Enum x;

		x = Number.ONE;



//		System.out.println(">>> " + Crypt.crypt("zzz", "$6$7cde7fce7d89f60b"));

		String salt = String.format("$6$%016X", new java.util.Random().nextLong());
		//Class type = ;// ClassNotFoundException
		String hash = Class.forName("org.apache.commons.codec.digest.Crypt").getMethod("crypt", String.class, String.class).invoke(null, password, salt).toString();
		System.out.println(">>> " + /*Crypt.crypt(password, salt)*/hash);

	}

	enum Number { ONE, TWO, THREE }

	enum Letter { A, B, C }

}
