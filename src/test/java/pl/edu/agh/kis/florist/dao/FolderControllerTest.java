package pl.edu.agh.kis.florist.dao;
 
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pl.edu.agh.kis.florist.controller.FolderController;
import pl.edu.agh.kis.florist.db.Tables;
import pl.edu.agh.kis.florist.db.tables.daos.FolderMetadataDao;
 
import java.nio.file.Path;
import java.nio.file.Paths;
 
import static org.junit.Assert.*;

public class FolderControllerTest {
 
    private final String DB_URL = "jdbc:sqlite:test.db";
    private DSLContext create;
 
    @Before
    public void setUp() {
        create = DSL.using(DB_URL);
        //clean all data before every test
        create.query(" DELETE FROM folder_metadata;").execute();
        create.query("DELETE FROM sqlite_sequence WHERE name = 'folder_metadata';").execute();
    }
 
    @After
    public void tearDown() {
        create.close();
    }
 
 
    @Test
    public void createDirTest(){
        FolderController folderController = new FolderController();
        folderController.createDir(Paths.get("/root/def"));
        folderController.createDir(Paths.get("/root/unicef"));
        folderController.createDir(Paths.get("/root/unicef/bajlando"));
    }
 

    }

