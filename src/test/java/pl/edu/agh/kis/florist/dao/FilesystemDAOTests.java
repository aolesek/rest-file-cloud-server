package pl.edu.agh.kis.florist.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.nio.file.Paths;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import pl.edu.agh.kis.florist.db.tables.daos.FolderMetadataDao;
import pl.edu.agh.kis.florist.db.tables.pojos.FolderMetadata;
import pl.edu.agh.kis.florist.exceptions.DestinationDirectoryInsideSourceDirectoryException;
import pl.edu.agh.kis.florist.exceptions.DirectoryAlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.DirectoryNotFoundException;
import pl.edu.agh.kis.florist.exceptions.IllegalPathException;
import pl.edu.agh.kis.florist.exceptions.NewParentDirectoryNotFoundException;
import pl.edu.agh.kis.florist.model.Author;
import pl.edu.agh.kis.florist.model.Book;
import pl.edu.agh.kis.florist.model.FolderContent;


public class FilesystemDAOTests {
	
	private final String DB_URL = "jdbc:sqlite:test.db";
	private DSLContext create;
	
	@Before
	public void setUp() {
		//establish connection with database
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
	public void creatingFolderReturnsMetadata() throws Exception {
		FilesystemDAO filesystem = new FilesystemDAO();
		filesystem.createDirectory(Paths.get("/wonderland"));
		FolderMetadata newFolder = filesystem.createDirectory(Paths.get("/wonderland/alice"));

		assertNotNull(newFolder);
		assertNotNull(newFolder.getFolderId());
		assertThat(newFolder.getFolderId()).isGreaterThan(1);

		assertThat(newFolder).extracting(FolderMetadata::getName).containsOnly("alice");
		assertThat(newFolder).extracting(FolderMetadata::getParentFolderId).containsOnly(1);		
	}
	
	@Test
	public void creatingMultipleFoldersTest() throws Exception {
		FilesystemDAO filesystem = new FilesystemDAO();
		filesystem.createDirectory(Paths.get("/wonderland"));
		FolderMetadata f1 = filesystem.createDirectory(Paths.get("/wonderland/alice"));
		FolderMetadata f2 = filesystem.createDirectory(Paths.get("/wonderland/rabbithole"));
		FolderMetadata f3 = filesystem.createDirectory(Paths.get("/wonderland/rabbithole/rabbit"));
		
		assertThat(f1).extracting(FolderMetadata::getName, FolderMetadata::getFolderId, FolderMetadata::getParentFolderId).containsOnly("alice", 2, 1);
		assertThat(f2).extracting(FolderMetadata::getName, FolderMetadata::getFolderId, FolderMetadata::getParentFolderId).containsOnly("rabbithole", 3, 1);
		assertThat(f3).extracting(FolderMetadata::getName, FolderMetadata::getFolderId, FolderMetadata::getParentFolderId).containsOnly("rabbit", 4, 3);
	}
	
	@Test
	public void listingFoldersTest() throws DirectoryAlreadyExistsException, IllegalPathException, DirectoryNotFoundException {
		FilesystemDAO filesystem = new FilesystemDAO();
		filesystem.createDirectory(Paths.get("/wonderland"));
		FolderMetadata f1 = filesystem.createDirectory(Paths.get("/wonderland/alice"));
		FolderMetadata f2 = filesystem.createDirectory(Paths.get("/wonderland/rabbithole"));
		FolderMetadata f3 = filesystem.createDirectory(Paths.get("/wonderland/rabbithole/rabbit1"));
		FolderMetadata f4 = filesystem.createDirectory(Paths.get("/wonderland/rabbithole/rabbit2"));
		FolderMetadata f5 = filesystem.createDirectory(Paths.get("/wonderland/rabbithole/rabbit3"));
		FolderMetadata f6 = filesystem.createDirectory(Paths.get("/wonderland/rabbithole/rabbit4"));
		FolderMetadata f7 = filesystem.createDirectory(Paths.get("/wonderland/rabbithole/rabbit5"));
		FolderMetadata f8 = filesystem.createDirectory(Paths.get("/wonderland/rabbithole/rabbit6"));
		FolderMetadata f9 = filesystem.createDirectory(Paths.get("/wonderland/rabbithole/rabbit6/littlerabbit"));
		
		FolderContent content = filesystem.listFolderContent(Paths.get("/wonderland"), false);
		assertNotNull(content);
		assertThat(content.getFolders().size()).isEqualTo(2);
		
		FolderContent content2 = filesystem.listFolderContent(Paths.get("/wonderland/rabbithole"), true);
		assertNotNull(content2);
		assertThat(content2.getFolders().size()).isEqualTo(7);


	}
	
	@Test(expected=DirectoryAlreadyExistsException.class) public void createAlreadyExistsException() throws DirectoryAlreadyExistsException, IllegalPathException {
		FilesystemDAO filesystem = new FilesystemDAO();
		filesystem.createDirectory(Paths.get("/wonderland"));
		filesystem.createDirectory(Paths.get("/wonderland/alice"));
		filesystem.createDirectory(Paths.get("/wonderland/alice"));
	}
	
	@Test(expected=IllegalPathException.class) public void createIllegalPathException() throws DirectoryAlreadyExistsException, IllegalPathException {
		FilesystemDAO filesystem = new FilesystemDAO();
		filesystem.createDirectory(Paths.get("/wonderland/is/the/bad/place/to/live"));
	}
	
	@Test(expected=IllegalPathException.class) public void listIllegalPathException() throws IllegalPathException, DirectoryNotFoundException {
		FilesystemDAO filesystem = new FilesystemDAO();
		filesystem.listFolderContent(Paths.get("/wonderland/is"), true);
	}
	
	@Test(expected=DirectoryNotFoundException.class) public void listIDirectoryNotFoundException() throws IllegalPathException, DirectoryNotFoundException, DirectoryAlreadyExistsException {
		FilesystemDAO filesystem = new FilesystemDAO();
		filesystem.createDirectory(Paths.get("/wonderland"));
		filesystem.listFolderContent(Paths.get("/wonderland/is"), true);
	}
	
	@Test
	public void deletionTest() throws Exception {
		FilesystemDAO filesystem = new FilesystemDAO();
		filesystem.createDirectory(Paths.get("/one"));
		filesystem.createDirectory(Paths.get("/one/two"));
		filesystem.createDirectory(Paths.get("/one/three"));

		FolderContent content = filesystem.listFolderContent(Paths.get("/one"), false);
		
		assertThat(content.getFolders().size()).isEqualTo(2);
		assertThat(content.getFolders().get(0)).extracting(FolderMetadata::getName).containsOnly("two");
		assertThat(content.getFolders().get(1)).extracting(FolderMetadata::getName).containsOnly("three");
		
		filesystem.deleteFolder(Paths.get("/one/two"));
		content = filesystem.listFolderContent(Paths.get("/one"), false);
		assertThat(content.getFolders().size()).isEqualTo(1);
		assertThat(content.getFolders().get(0)).extracting(FolderMetadata::getName).containsOnly("three");
		
		filesystem.deleteFolder(Paths.get("/one/three"));
		content = filesystem.listFolderContent(Paths.get("/one"), false);
		assertThat(content.getFolders().size()).isEqualTo(0);
	}
	
	@Test
	public void getFolderMetadataTest() throws DirectoryAlreadyExistsException, IllegalPathException, DirectoryNotFoundException {
		FilesystemDAO filesystem = new FilesystemDAO();
		FolderMetadata created = filesystem.createDirectory(Paths.get("/one"));
		FolderMetadata fetched = filesystem.getFolderMetadata(Paths.get("/one"));
		assertThat(created).isEqualToComparingFieldByField(fetched);
	}
	
	@Test
	public void isIndiseTest() throws DirectoryAlreadyExistsException, IllegalPathException, DirectoryNotFoundException {
		FilesystemDAO filesystem = new FilesystemDAO();
		FolderMetadata one = filesystem.createDirectory(Paths.get("/one"));
		FolderMetadata two = filesystem.createDirectory(Paths.get("/one/two"));
		FolderMetadata three = filesystem.createDirectory(Paths.get("/one/two/three"));
		FolderMetadata four = filesystem.createDirectory(Paths.get("/one/two/three/four"));

		assertThat(filesystem.isInside(Paths.get(one.getPathLower()),  Paths.get(four.getPathLower()))).isEqualTo(true);
		assertThat(filesystem.isInside(Paths.get(four.getPathLower()),  Paths.get(one.getPathLower()))).isEqualTo(false);

	}
	
	@Test
	public void movingTest() throws DirectoryAlreadyExistsException, IllegalPathException, DirectoryNotFoundException, NewParentDirectoryNotFoundException, DestinationDirectoryInsideSourceDirectoryException {
		FilesystemDAO filesystem = new FilesystemDAO();
		FolderMetadata one = filesystem.createDirectory(Paths.get("/one"));
		FolderMetadata two = filesystem.createDirectory(Paths.get("/one/two"));
		FolderMetadata three = filesystem.createDirectory(Paths.get("/one/two/three"));
		FolderMetadata four = filesystem.createDirectory(Paths.get("/one/two/three/four"));

		filesystem.moveFolder(Paths.get("/one/two/three/four"), Paths.get("/one/six"));
		
		FolderMetadata movedFolder = filesystem.getFolderMetadata(Paths.get("/one/six"));
		
		assertThat(movedFolder).extracting(FolderMetadata::getName, FolderMetadata::getParentFolderId, FolderMetadata::getPathLower).containsOnly("six", 1, "/one/six");
		assertThat(filesystem.exists(Paths.get("/one/two/three/four"))).isEqualTo(false);
	}
	
	
}
