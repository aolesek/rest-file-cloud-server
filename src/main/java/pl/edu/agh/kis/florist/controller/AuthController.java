package pl.edu.agh.kis.florist.controller;

import spark.Request;
import spark.Response;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.security.SecureRandom;
import java.math.BigInteger;
import java.nio.file.Paths;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.mindrot.jbcrypt.BCrypt;

import pl.edu.agh.kis.florist.dao.FilesystemDAO;
import pl.edu.agh.kis.florist.db.tables.daos.SessionDataDao;
import pl.edu.agh.kis.florist.db.tables.daos.UsersDao;
import pl.edu.agh.kis.florist.db.tables.pojos.SessionData;
import pl.edu.agh.kis.florist.db.tables.pojos.Users;
import pl.edu.agh.kis.florist.exceptions.DirectoryAlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.IllegalPathException;
import pl.edu.agh.kis.florist.exceptions.SessionExpiredException;
import pl.edu.agh.kis.florist.exceptions.UnsuccessfulLoginException;
import pl.edu.agh.kis.florist.exceptions.UserAlreadyExistException;

import static pl.edu.agh.kis.florist.db.Tables.SESSION_DATA;

public class AuthController {
	private final String DB_URL = "jdbc:sqlite:test.db";
	private final Configuration configuration;
	
	private final SessionDataDao sessionDao;
	private final UsersDao usersDao;
	private final SecureRandom random;
	private final FilesystemDAO fsDao;
	
	public AuthController() {
		Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        configuration = new DefaultConfiguration().set(connection).set(SQLDialect.SQLITE);
        
		sessionDao = new SessionDataDao(configuration);
		usersDao = new UsersDao(configuration);
		random = new SecureRandom();
		fsDao = new FilesystemDAO();
	}
	
	public static String createNewHashedPassword(String password) {
	    return BCrypt.hashpw(password, BCrypt.gensalt());
	}
	
	public static boolean checkPassword(String candidatePassword,String storedHashedPassword) {
	    return BCrypt.checkpw(candidatePassword, storedHashedPassword);
	}
	

	synchronized public String genSessionId() {
		String randomId;
		List<SessionData> sessionList;
		
		do {
			randomId = new BigInteger(80, random).toString(32);
			sessionList = sessionDao.fetchBySessionId(randomId);
			
		} while (!sessionList.isEmpty());
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());

		SessionData session = new SessionData(randomId, 0, timestamp);
		
		try {sessionDao.insert(session);} catch(Exception e) {}
		
		return randomId;
	  }
	
	public Object handleCreateUser(Request request, Response response) throws UserAlreadyExistException, DirectoryAlreadyExistsException, IllegalPathException {
		String userName = request.queryParams("user_name").toLowerCase();
		String displayName = request.queryParams("display_name");
		String userPassword = request.body();
		String hashedPassword = createNewHashedPassword(userPassword);
		
		Users user = new Users(
				null,
				userName,
				displayName,
				hashedPassword
				);
		
		if ( !usersDao.fetchByUserName(userName).isEmpty() )
			throw new UserAlreadyExistException();
		
		usersDao.insert(user);
		
		Users insertedUser = usersDao.fetchByUserName(userName).get(0);

		fsDao.createDirectory(Paths.get("/"+insertedUser.getId()));
		response.status(201);		
		return "User "+userName+" created successfully.";
	}
	
	public Object handleAccess(Request request, Response response) throws UnsuccessfulLoginException, SessionExpiredException {
		String userName = request.queryParams("user_name").toLowerCase();
		String userPassword = request.body();

		List<Users> users = usersDao.fetchByUserName(userName);
		
		
		if (users.isEmpty())
			throw new UnsuccessfulLoginException();
		
		if (!checkPassword(userPassword, users.get(0).getHashedPassword()))
			throw new UnsuccessfulLoginException();
		List<SessionData> data = sessionDao.fetchByUserId(users.get(0).getId());
		
		if( !data.isEmpty() ) {
			try {
				Timestamp timestamp = new Timestamp(System.currentTimeMillis());
				SessionData sessionUpdated = new SessionData(data.get(0).getSessionId(), data.get(0).getUserId(),timestamp);
				sessionDao.update(sessionUpdated);
				
			} catch (Exception e) {}
			response.status(200);
			return data.get(0).getSessionId()+"already";
		}
		
		String uniqueID = genSessionId();
		
		try {
			SessionData sessionDraft = sessionDao.fetchBySessionId(uniqueID).get(0);
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			SessionData sessionFinal = new SessionData(uniqueID, users.get(0).getId(),timestamp);
			sessionDao.update(sessionFinal);
		} catch(Exception e) {}
	
		response.status(200);
		return uniqueID;
	}
	
	public Boolean isSessionActive(String sessionId) {
		List<SessionData> sessions = sessionDao.fetchBySessionId(sessionId);
		
		if (sessions.isEmpty())
			return false;
		
		try {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			SessionData extendedSession = new SessionData(sessions.get(0).getSessionId(), sessions.get(0).getUserId(),timestamp);
			sessionDao.update(extendedSession);
		} catch(Exception e) {}
		
		return true;
	}
	
	public Integer getSessionUserId(String sessionId) throws SessionExpiredException {
		List<SessionData> sessions = sessionDao.fetchBySessionId(sessionId);
		if (sessions.isEmpty())
			throw new SessionExpiredException();
		return sessions.get(0).getUserId();
	}
}
