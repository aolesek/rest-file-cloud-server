package pl.edu.agh.kis.florist.model;

import java.util.List;

import pl.edu.agh.kis.florist.db.tables.pojos.FileMetadata;
import pl.edu.agh.kis.florist.db.tables.pojos.FolderMetadata;

public class FolderContent {
	List<FolderMetadata> folders;
	List<FileMetadata> files;
	
	public List<FolderMetadata> getFolders() {
		return folders;
	}
	public void setFolders(List<FolderMetadata> folders) {
		this.folders = folders;
	}
	public List<FileMetadata> getFiles() {
		return files;
	}
	public void setFiles(List<FileMetadata> files) {
		this.files = files;
	}
	
}
