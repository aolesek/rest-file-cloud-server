package pl.edu.agh.kis.florist.controller;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

import pl.edu.agh.kis.florist.dao.FilesystemDAO;
import pl.edu.agh.kis.florist.db.tables.daos.SessionDataDao;
import pl.edu.agh.kis.florist.db.tables.daos.UsersDao;
import pl.edu.agh.kis.florist.db.tables.pojos.SessionData;

public class SessionManager implements Runnable{

	private final String DB_URL = "jdbc:sqlite:test.db";
	private final Configuration configuration;
	
	private final SessionDataDao sessionDao;
	
	public SessionManager() {
		Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        configuration = new DefaultConfiguration().set(connection).set(SQLDialect.SQLITE);
        
		sessionDao = new SessionDataDao(configuration);
	}

	@Override
	public void run() {
		  try {
		        while( !Thread.currentThread().isInterrupted() ) {
		        	checkSessions();
			        Thread.sleep(20000);
		        }
		    } catch(InterruptedException ex) {
		    } finally {
		    }
	}
	
	public void checkSessions() {
        List<SessionData> currentSessions = sessionDao.findAll();
        
        for(int i = 0; i < currentSessions.size(); i++) {
    		Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        	Timestamp lastAccessed = currentSessions.get(i).getLastAccessed();
        	
        	if ( (currentTime.getTime()-lastAccessed.getTime()) > 60000*100) {
        		sessionDao.deleteById(currentSessions.get(i).getSessionId());
        	}
        }
	}

}
