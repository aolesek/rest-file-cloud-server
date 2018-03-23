package pl.edu.agh.kis.florist.dao;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.io.FileNotFoundException;
import java.io.InputStream;


import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

//import pl.edu.agh.kis.florist.controller.HttpServletResponse;
import pl.edu.agh.kis.florist.db.tables.daos.FileContentsDao;
import pl.edu.agh.kis.florist.db.tables.daos.FileMetadataDao;
import pl.edu.agh.kis.florist.db.tables.daos.FolderMetadataDao;
import pl.edu.agh.kis.florist.db.tables.pojos.FileContents;
import pl.edu.agh.kis.florist.db.tables.pojos.FileMetadata;
import pl.edu.agh.kis.florist.db.tables.pojos.FolderMetadata;
import pl.edu.agh.kis.florist.exceptions.DestinationDirectoryInsideSourceDirectoryException;
import pl.edu.agh.kis.florist.exceptions.DirectoryAlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.DirectoryNotFoundException;
import pl.edu.agh.kis.florist.exceptions.IllegalPathException;
import pl.edu.agh.kis.florist.exceptions.NewParentDirectoryNotFoundException;
import pl.edu.agh.kis.florist.model.FolderContent;

public class FilesystemDAO {
	
	private final String DB_URL = "jdbc:sqlite:test.db";
	Configuration configuration;
    FolderMetadataDao folderMetadataDao;
    FileMetadataDao fileMetadataDao;
    FileContentsDao fileContentsDao;


	
	public FilesystemDAO() {
		Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        configuration = new DefaultConfiguration().set(connection).set(SQLDialect.SQLITE);


        folderMetadataDao = new FolderMetadataDao(configuration);
        fileMetadataDao = new FileMetadataDao(configuration);
        fileContentsDao = new FileContentsDao(configuration);

	}
	
	public Boolean isDirectory(Path path) {
		return folderMetadataDao.fetchByPathLower(path.toString().toLowerCase()).size() > 0;
	}
	
	public Boolean isFile(Path path) {
		return fileMetadataDao.fetchByPathLower(path.toString().toLowerCase()).size() > 0;
	}
	
	public Boolean exists(Path path) {
		return (isFile(path) || isDirectory(path));
	}
	
	public Boolean isInside(Path sourcePath, Path destinationPath) throws IllegalPathException, DirectoryNotFoundException {
		List<FolderMetadata> sourcePathContent = listFolderContent(sourcePath, true).getFolders();
		
		for(int i = 0; i < sourcePathContent.size(); i++) {
			if (sourcePathContent.get(i).getPathLower().equals(destinationPath.toString().toLowerCase()))
				return true;
		}
		
		
		
		return false;
	}
	
	// /arek/pliki/asdfa/asdfasdf/asdgasdg
	
	public FolderMetadata createDirectory(Path path) throws DirectoryAlreadyExistsException, IllegalPathException {
		
		if (folderMetadataDao.fetchByPathLower(path.toString().toLowerCase()).size() > 0)
			throw new DirectoryAlreadyExistsException();
		
		List<FolderMetadata> parent = folderMetadataDao.fetchByPathLower(
				path.getParent().toString().toLowerCase()
			);
		
		Integer parentId = 0;
		if (path.getParent().toString().equals("/")) {
			parentId = 0; 
		} else {
			if (parent.size() == 0)
				throw new IllegalPathException();
			else 
				parentId = parent.get(0).getFolderId();
		}
        
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        
		FolderMetadata newFolder = new FolderMetadata(
														null,
														path.getFileName().toString(),
														path.toString().toLowerCase(),
														path.toString(),
														parentId,
														timestamp
														);
		
		folderMetadataDao.insert(newFolder);
				
		FolderMetadata insertedFolder = folderMetadataDao.fetchByPathLower(path.toString().toLowerCase()).get(0);
		
		//folderMetadataDao.fetchByPathLower(path.toString().toLowerCase()).get(0)
														
		return insertedFolder;
	}
	
	public FolderContent listFolderContent(Path path, Boolean recursive) throws IllegalPathException, DirectoryNotFoundException {
		
		if (folderMetadataDao.fetchByPathLower(path.toString().toLowerCase()).size() == 0) {
			if ( folderMetadataDao.fetchByPathLower(path.getParent().toString().toLowerCase()).size() == 0 )
				throw new IllegalPathException();
			else
				throw new DirectoryNotFoundException();
		}
		
		Integer folderId = folderMetadataDao.fetchByPathLower(path.toString().toLowerCase()).get(0).getFolderId();
	
		List<FolderMetadata> internalFolders = folderMetadataDao.fetchByParentFolderId(folderId);
		List<FileMetadata> internalFiles = fileMetadataDao.fetchByEnclosingFolderId(folderId);
		
		if (recursive && !internalFolders.isEmpty())
		{
			for (int i = 0; i < internalFolders.size(); i++) {
				FolderContent subContent = new FolderContent();
				subContent = this.listFolderContent(Paths.get(internalFolders.get(i).getPathLower() ), false);
				internalFolders.addAll(subContent.getFolders());
				internalFiles.addAll(subContent.getFiles());
			}				
		}
		
		FolderContent content = new FolderContent();
		
		content.setFolders(internalFolders);
		content.setFiles(internalFiles);

		
		return content;
	}
	
	public FolderMetadata deleteFolder(Path path) throws IllegalPathException, DirectoryNotFoundException {
		if (folderMetadataDao.fetchByPathLower(path.toString().toLowerCase()).size() == 0) {
			if ( folderMetadataDao.fetchByPathLower(path.getParent().toString().toLowerCase()).size() == 0 )
				throw new IllegalPathException();
			else
				throw new DirectoryNotFoundException();
		}
		
		FolderMetadata folderToDelete = folderMetadataDao.fetchByPathLower(path.toString().toLowerCase()).get(0);
		
		FolderContent content = listFolderContent(path, true);
		
		//deleting files contents
		for(FileMetadata i:content.getFiles()) {
			fileContentsDao.deleteById(i.getFileId());
		}
		
		//deleting folder metadatas
		folderMetadataDao.delete(content.getFolders());
		
		//deleting file metadata
		fileMetadataDao.delete(content.getFiles());
		
		folderMetadataDao.delete(folderToDelete);
		
		return folderToDelete;
	}
	
	public FolderMetadata getFolderMetadata(Path path) throws DirectoryNotFoundException {
		List<FolderMetadata> metadata = folderMetadataDao.fetchByPathLower(path.toString().toLowerCase());
		
		if (metadata.size() != 1)
			throw new DirectoryNotFoundException();
		
		return metadata.get(0);
	}
	
	public FolderMetadata moveFolder(Path sourcePath, Path destinationPath) throws DirectoryNotFoundException, NewParentDirectoryNotFoundException, IllegalPathException, DestinationDirectoryInsideSourceDirectoryException {
		if (!isDirectory(sourcePath))
			throw new DirectoryNotFoundException();
		
		if (!isDirectory(destinationPath.getParent()))
			throw new NewParentDirectoryNotFoundException();
		
		if (isInside(sourcePath, destinationPath))
			throw new DestinationDirectoryInsideSourceDirectoryException();
		
		
		FolderMetadata newParent = folderMetadataDao.fetchByPathLower(destinationPath.getParent().toString().toLowerCase()).get(0);
		
		FolderMetadata oldMetadata = folderMetadataDao.fetchByPathLower(sourcePath.toString().toLowerCase()).get(0);
		
		FolderMetadata newMetadata = new FolderMetadata(
														oldMetadata.getFolderId(),
														destinationPath.getFileName().toString(),
														destinationPath.toString().toLowerCase(),
														destinationPath.toString(),
														newParent.getFolderId(),
														oldMetadata.getServerCreatedAt()
														);
		FolderContent content = listFolderContent(sourcePath, true);
		folderMetadataDao.update(newMetadata);
	
		
		List<FolderMetadata> internalFolders = content.getFolders();
			
		for (FolderMetadata current : internalFolders) {
			FolderMetadata currentUpdated = new FolderMetadata(
					current.getFolderId(),
					current.getName(),
					current.getPathLower().replace(sourcePath.toString().toLowerCase(), newMetadata.getPathLower()),
					current.getPathDisplay().replaceAll(sourcePath.toString(), newMetadata.getPathDisplay()),
					current.getParentFolderId(),
					current.getServerCreatedAt()
					);
			
			folderMetadataDao.update(currentUpdated);
		}
		
		List<FileMetadata> internalFiles = content.getFiles();
		
		for (FileMetadata current : internalFiles) {
			FileMetadata currentUpdated = new FileMetadata(
					current.getFileId(),
					current.getName(),
					current.getPathLower().replace(sourcePath.toString().toLowerCase(), newMetadata.getPathLower()),
					current.getPathDisplay().replaceAll(sourcePath.toString(), newMetadata.getPathDisplay()),
					current.getSize(),
					current.getServerCreatedAt(),
					current.getServerChangedAt(),
					current.getEnclosingFolderId()
					);
			
			fileMetadataDao.update(currentUpdated);
		}		
		return newMetadata;
	}
	
	public FileMetadata uploadFile(Path filePath, Part uploadedFile) throws DirectoryNotFoundException, IllegalPathException {
		Path enclosingFolderPath = filePath.getParent();
		
		//if( !exists(enclosingFolderPath) && !enclosingFolderPath.equals("/"))
		// new DirectoryNotFoundException();
		
		if (isDirectory(filePath))
			throw new IllegalPathException();
		
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());

        int fileId;
        
        FileMetadata insertedFileMetadata = null;
        
		if (!exists(filePath)) {
			FileMetadata metadata = new FileMetadata(
					null,
					filePath.getFileName().toString(),
					filePath.toString().toLowerCase(),
					filePath.toString(),
					(int) (uploadedFile.getSize()/1000),
					currentTime,
					currentTime,
					getFolderMetadata(enclosingFolderPath).getFolderId()
					);
			
			fileMetadataDao.insert(metadata);
			
			insertedFileMetadata = fileMetadataDao.fetchByPathLower(filePath.toString().toLowerCase()).get(0);
			
			fileId = insertedFileMetadata.getFileId();
			
		} else {
			FileMetadata oldFileMetadata = fileMetadataDao.fetchByPathLower(filePath.toString().toLowerCase()).get(0);

			insertedFileMetadata = new FileMetadata(
					oldFileMetadata.getFileId(),
					oldFileMetadata.getName(),
					oldFileMetadata.getPathLower(),
					oldFileMetadata.getPathDisplay(),
					(int) (uploadedFile.getSize()/1000),
					oldFileMetadata.getServerCreatedAt(),
					currentTime,
					oldFileMetadata.getEnclosingFolderId()
					);
			
			fileId = insertedFileMetadata.getFileId();
			fileMetadataDao.update(insertedFileMetadata);
		}
		
		try {
		Connection c = DriverManager.getConnection(DB_URL);

		String sql = "INSERT INTO file_contents (file_id, contents) VALUES (?, ?)";
		PreparedStatement stmt = c.prepareStatement(sql);
		stmt.setInt(1, fileId);
		stmt.setBinaryStream(2, uploadedFile.getInputStream(), (int) uploadedFile.getSize());
		stmt.execute();
		
		c.commit();
		c.close();

		} catch (SQLException ex) {
			// handle any errors
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			}catch(Exception e){
			e.printStackTrace();
		}	
		
		return 	insertedFileMetadata;
	}

	public byte[] getFile(Path path) throws FileNotFoundException {
		if(!isFile(path))
			throw new FileNotFoundException();
		
		FileMetadata metadata = fileMetadataDao.fetchByPathLower(path.toString().toLowerCase()).get(0);
		
		int fileId = metadata.getFileId();
		
		FileContents file = fileContentsDao.fetchByFileId(fileId).get(0);
		
		return file.getContents();
		
	}
	
	public FileMetadata getFileMetadata(Path path) throws FileNotFoundException {
		if (!isFile(path))
			throw new FileNotFoundException();
		
		FileMetadata fetchedMetadata = fileMetadataDao.fetchByPathLower(path.toString().toLowerCase()).get(0);
		
		return fetchedMetadata;
	}

	public FileMetadata moveFile(Path sourcePath, Path destinationPath) throws FileNotFoundException, NewParentDirectoryNotFoundException {
		if (!isFile(sourcePath))
			throw new FileNotFoundException();
		
		if (!isDirectory(destinationPath))
			throw new NewParentDirectoryNotFoundException();
		
		FolderMetadata newParent = folderMetadataDao.fetchByPathLower(destinationPath.toString().toLowerCase()).get(0);
		
		FileMetadata oldMetadata = fileMetadataDao.fetchByPathLower(sourcePath.toString().toLowerCase()).get(0);
		
		FileMetadata newMetadata = new FileMetadata(
										oldMetadata.getFileId(),
										oldMetadata.getName(),
										destinationPath.toString().toLowerCase()+"/"+oldMetadata.getName().toLowerCase(),
										destinationPath.toString()+"/"+oldMetadata.getName(),
										oldMetadata.getSize(),
										oldMetadata.getServerCreatedAt(),
										oldMetadata.getServerChangedAt(),
										newParent.getFolderId()
										);
		
		fileMetadataDao.update(newMetadata);
		
		return newMetadata;
	}

	public FileMetadata renameFile(Path path, String newName) throws FileNotFoundException {
		if (!exists(path) || !isFile(path))
			throw new FileNotFoundException();
		
		FileMetadata oldMetadata = fileMetadataDao.fetchByPathLower(path.toString().toLowerCase()).get(0);
		
		FileMetadata newMetadata = new FileMetadata(
				oldMetadata.getFileId(),
				newName,
				Paths.get(oldMetadata.getPathLower()).getParent().toString().toLowerCase()+"/"+newName.toLowerCase(),
				Paths.get(oldMetadata.getPathLower()).getParent().toString()+"/"+newName,
				oldMetadata.getSize(),
				oldMetadata.getServerCreatedAt(),
				oldMetadata.getServerChangedAt(),
				oldMetadata.getEnclosingFolderId()
				);
		
		fileMetadataDao.update(newMetadata);
		
		return newMetadata;				
	}


	public FileMetadata deleteFile(Path path) throws FileNotFoundException {
		if (!isFile(path))
			throw new FileNotFoundException();
		
		FileMetadata fileToDelete = fileMetadataDao.fetchByPathLower(path.toString().toLowerCase()).get(0);
		
		FileContents contents = fileContentsDao.fetchByFileId(fileToDelete.getFileId()).get(0);
		
		fileContentsDao.delete(contents);
		
		fileMetadataDao.delete(fileToDelete);
		
		return fileToDelete;
	}
	
	public FolderMetadata renameFolder(Path path, String newName) throws IllegalPathException, DirectoryNotFoundException {
		List<FolderMetadata> dbFolders = folderMetadataDao.fetchByPathLower(path.toString().toLowerCase());
		
		if (dbFolders.isEmpty() )
			throw new DirectoryNotFoundException();
		
		FolderMetadata folderToRename = folderMetadataDao.fetchByPathLower(path.toString().toLowerCase()).get(0);

		Path newPath = Paths.get(
				Paths.get(folderToRename.getPathDisplay()).getParent().toString().toLowerCase()+"/"+newName
				);

				
		
		FolderMetadata renamedFolder = new FolderMetadata(
				folderToRename.getFolderId(),
				newName,
				newPath.toString().toLowerCase(),
				newPath.toString(),
				folderToRename.getParentFolderId(),
				folderToRename.getServerCreatedAt()
				);
		
		folderMetadataDao.update(renamedFolder);
		
		FolderContent content = listFolderContent(path, true);
		List<FolderMetadata> internalFolders = content.getFolders();
			
		for (FolderMetadata current : internalFolders) {
			FolderMetadata currentUpdated = new FolderMetadata(
					current.getFolderId(),
					current.getName(),
					current.getPathLower().replace(path.toString().toLowerCase(), newPath.toString().toLowerCase()),
					current.getPathDisplay().replaceAll(path.toString(), newPath.toString()),
					current.getParentFolderId(),
					current.getServerCreatedAt()
					);
			
			folderMetadataDao.update(currentUpdated);
		}
		
		List<FileMetadata> internalFiles = content.getFiles();
		
		for (FileMetadata current : internalFiles) {
			FileMetadata currentUpdated = new FileMetadata(
					current.getFileId(),
					current.getName(),
					current.getPathLower().replaceAll(path.toString().toLowerCase(), newPath.toString().toLowerCase()),
					current.getPathDisplay().replaceAll(path.toString(), newPath.toString()),
					current.getSize(),
					current.getServerCreatedAt(),
					current.getServerChangedAt(),
					current.getEnclosingFolderId()					
					);
			
			fileMetadataDao.update(currentUpdated);
		}
		
		return renamedFolder;
		
	}
	
}
