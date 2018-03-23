package pl.edu.agh.kis.florist.controller;
 
import com.google.gson.Gson;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import pl.edu.agh.kis.florist.db.tables.daos.FileMetadataDao;
import pl.edu.agh.kis.florist.db.tables.daos.FolderMetadataDao;
import pl.edu.agh.kis.florist.db.tables.daos.UsersDao;
import pl.edu.agh.kis.florist.db.tables.pojos.FolderMetadata;
 
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
 
 
public class FolderController {
    private static final int CREATED = 201;
    private final String DB_URL = "jdbc:sqlite:test.db";
    private final Gson gson=new Gson();
    Configuration configuration;
    private final FolderMetadataDao folderMetadataDao;
 
    public FolderController(){
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        configuration = new DefaultConfiguration().set(connection).set(SQLDialect.SQLITE);
        FolderMetadata newDir= new FolderMetadata(
                null,
                "root",
                "/root",
                "/root",
                -1,
                new Timestamp(System.currentTimeMillis())
        );
        folderMetadataDao = new FolderMetadataDao(configuration);
        folderMetadataDao.insert(newDir);
    }
 
    public Object createDir(Path path){
        FolderMetadata parent = folderMetadataDao.fetchByPathLower(
                path.getParent().toString().toLowerCase()).get(0);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
 
        FolderMetadata newDir = new FolderMetadata(
                null,
                path.getFileName().toString(),
                path.toString().toLowerCase(),
                path.toString(),
                parent.getFolderId(),
                timestamp
        );
 
        folderMetadataDao.insert(newDir);
        FolderMetadata insertedDir =
                folderMetadataDao.fetchByPathLower(path.toString().toLowerCase()).get(0);
 
        return insertedDir;
    }
 
}