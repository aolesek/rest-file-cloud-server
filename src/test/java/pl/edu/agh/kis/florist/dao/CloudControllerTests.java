package pl.edu.agh.kis.florist.dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.junit.Test;

import pl.edu.agh.kis.florist.db.tables.daos.SessionDataDao;
import pl.edu.agh.kis.florist.db.tables.daos.UsersDao;
import pl.edu.agh.kis.florist.db.tables.pojos.FolderMetadata;
import pl.edu.agh.kis.florist.db.tables.pojos.SessionData;
import pl.edu.agh.kis.florist.db.tables.pojos.Users;

public class CloudControllerTests {
	private final String DB_URL = "jdbc:sqlite:test.db";
	private Configuration configuration;
	
	@Test
	public void simpleTest() throws MalformedURLException {
		String a = "/a/b/c";
		System.out.println(a.replace("/a/b", "/d/xd"));
											
	}
}
