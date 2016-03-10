import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Test {

	public static void main(String[] args) throws Exception {

		String username = "";
		String password = "";


		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setDatabaseName("mail");
		dataSource.setServerName("localhost");
		dataSource.setUser("mail");
		dataSource.setPassword("mail");

		boolean r = false;
		try(
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"select ENCRYPT(?, substring(password, 1, 19)) = password " +
						"from user where username = ?")) {

			statement.setString(1, password);
			statement.setString(2, username);

			try(ResultSet resultSet = statement.executeQuery()) {
				if(resultSet.next()) r = resultSet.getBoolean(1);
			}
		}

		System.out.println(">>> " + r);
	}


}
/*


create table user (
    id int not null auto_increment primary key,
    username varchar(100) not null,
    password char(106) not null,
    email varchar(100) not null,
    active tinyint not null
);

insert into user(username, password, email, active) values (
  'morgano',
  '$6$7cde7fce7d89f60b$lfg93FKfShaa0hHaFl5ZYGS0Nwc7XBKh.lSGb6Ob0c2pQ9n690vsdk3RaLuJcr6YLU5/HknxuZTb66PmUIvM70',
  'morgano@villar.me',
  1
);

 */