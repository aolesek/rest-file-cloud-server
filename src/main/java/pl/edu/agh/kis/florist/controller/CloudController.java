package pl.edu.agh.kis.florist.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import pl.edu.agh.kis.florist.dao.FilesystemDAO;
import pl.edu.agh.kis.florist.db.tables.pojos.FileMetadata;
import pl.edu.agh.kis.florist.exceptions.DestinationDirectoryInsideSourceDirectoryException;
import pl.edu.agh.kis.florist.exceptions.DirectoryAlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.DirectoryNotFoundException;
import pl.edu.agh.kis.florist.exceptions.IllegalPathException;
import pl.edu.agh.kis.florist.exceptions.NewParentDirectoryNotFoundException;
import pl.edu.agh.kis.florist.exceptions.SessionExpiredException;
import spark.Request;
import spark.Response;


public class CloudController {
	private FilesystemDAO filesystemDAO;
	private AuthController auth;
	final private Thread sessionMan;

	
	public CloudController() {
		filesystemDAO= new FilesystemDAO();
		sessionMan = new Thread(new SessionManager());
		sessionMan.start();
	}
	
	public CloudController(AuthController c) {
		this();
		this.auth = c;
	}
	
	public Object handleCreateDirectory(Request request, Response response) throws DirectoryAlreadyExistsException, IllegalPathException, SessionExpiredException {
		String path = request.params("path");
		String session = request.queryParams("session");
		if(auth.isSessionActive(session)) {
			response.status(201);
			return filesystemDAO.createDirectory(Paths.get("/"+auth.getSessionUserId(session)+"/"+path));
		} else {
			response.status(401);
			return "Authorization required";
		}
		
	}
	
	public Object handleListFolderContent(Request request, Response response) throws DirectoryAlreadyExistsException, IllegalPathException, DirectoryNotFoundException, SessionExpiredException {
		String path = request.params("path");
		String session = request.queryParams("session");
		Boolean recursive = false;
		if ( (request.queryParams("recursive") != null) && request.queryParams("recursive").equals("true") ) 
			recursive = true;
		
		
		if(auth.isSessionActive(session)) {
			response.status(200);
			return filesystemDAO.listFolderContent(Paths.get("/"+auth.getSessionUserId(session)+"/"+path), recursive);
		} else {
			response.status(401);
			return "Authorization required";
		}
	}
	
	public Object handleDeleteFolder(Request request, Response response) throws IllegalPathException, DirectoryNotFoundException, FileNotFoundException, SessionExpiredException {
		String session = request.queryParams("session");

		if ((request.params("path") == null) || (request.params("path").equals("")))
			throw new FileNotFoundException();
		
		String spath = "/"+auth.getSessionUserId(session)+"/"+request.params("path");
		Path path = Paths.get(spath);

		
		if(auth.isSessionActive(session)) {
			if (filesystemDAO.exists(path)) {
				if (filesystemDAO.isDirectory(path)) {
					response.status(204);
					response.body("<h1> 204 </h1> Successful delete operation");
					return filesystemDAO.deleteFolder(path);

				}
					
				
				if (filesystemDAO.isFile(path)) {
					response.status(204);
					response.body("<h1> 204 </h1> Successful delete operation");
						return filesystemDAO.deleteFile(path);
				}
				
			} else throw new FileNotFoundException();
		} else {
			response.status(401);
			return "Authorization required";
		}
		return null;		
	}
	
	public Object handleUploadFile(Request request, Response response) throws IOException, ServletException, DirectoryNotFoundException, SessionExpiredException, IllegalPathException {
		String session = request.queryParams("session");
		if(auth.isSessionActive(session)) {
			String location = "tmpfiles"; //the directory location where files will be stored
			long maxFileSize = 100000000;  // the maximum size allowed for uploaded files
			long maxRequestSize = 100000000;  // the maximum size allowed for multipart/form-data requests
			int fileSizeThreshold = 1024;  // the size threshold after which files will be written to disk
			MultipartConfigElement multipartConfigElement = new MultipartConfigElement(location, maxFileSize, maxRequestSize, fileSizeThreshold);
			request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
			
			Part uploadedFile = request.raw().getPart("upfile");
		
			Path path = Paths.get("/"+auth.getSessionUserId(session)+request.queryParams("path"));
			
			FileMetadata metadata = filesystemDAO.uploadFile(path, uploadedFile);
			
			uploadedFile.delete();
			
			multipartConfigElement = null;
			uploadedFile = null;
			
			response.status(200);
			
			return metadata;
		} else {
			response.status(401);
			return "Authorization required";
		}
		
		
		

	}
	
	public Object handleDownloadFile(Request request, Response response) throws FileNotFoundException, SessionExpiredException {
		String session = request.queryParams("session");
		Path path = Paths.get("/"+auth.getSessionUserId(session)+request.params("path"));
		
		if(auth.isSessionActive(session)) {
			byte[] data = filesystemDAO.getFile(path);
			
			FileMetadata metadata = filesystemDAO.getFileMetadata(path);

	        HttpServletResponse raw = response.raw();
	        response.status(200);
	        response.header("Content-Disposition", "attachment; filename="+metadata.getName());
	        response.type("application/force-download");
	        try {
	            raw.getOutputStream().write(data);
	            raw.getOutputStream().flush();
	            raw.getOutputStream().close();
	        } catch (Exception e) {

	            e.printStackTrace();
	        }
	        return raw;
		} else {
			response.status(401);
			return "Authorization required";
		}
	}
	
	public Object handleGetMetadata(Request request, Response response) throws FileNotFoundException, DirectoryNotFoundException, SessionExpiredException, IllegalPathException {
		if ((request.params("path") == null) || (request.params("path").equals("")))
			throw new FileNotFoundException();
		
		String session = request.queryParams("session");
		Path path = Paths.get("/"+auth.getSessionUserId(session)+request.params("path"));
		
		
		
		if(auth.isSessionActive(session)) {
			if (filesystemDAO.exists(path)) {
				if (filesystemDAO.isDirectory(path)){
					response.status(200);
					return filesystemDAO.getFolderMetadata(path);

				}
				
				if (filesystemDAO.isFile(path)){
					response.status(200);
					return filesystemDAO.getFileMetadata(path);
				}
			} else throw new FileNotFoundException();
		} else {
			response.status(401);
			return "Authorization required";
		}
		
		return null;
	}
	
	public Object handleMove(Request request, Response response) throws FileNotFoundException, DirectoryNotFoundException, NewParentDirectoryNotFoundException, IllegalPathException, DestinationDirectoryInsideSourceDirectoryException, SessionExpiredException {
		
		if ((request.params("path") == null) || (request.params("path").equals("")))
			throw new FileNotFoundException();
		
		if (request.queryParams("new_path") == null || request.queryParams("new_path").equals(""))
			throw new DirectoryNotFoundException();
		
		String session = request.queryParams("session");
		Path sourcePath = Paths.get("/"+auth.getSessionUserId(session)+request.params("path"));
		Path destinationPath = Paths.get("/"+auth.getSessionUserId(session)+request.queryParams("new_path"));
		
		
		if(auth.isSessionActive(session)) {
			if (filesystemDAO.exists(sourcePath) && filesystemDAO.exists(destinationPath) ) {
				if (filesystemDAO.isDirectory(sourcePath)) {
					response.status(200);
					return filesystemDAO.moveFolder(sourcePath, destinationPath);
				}

				
				if (filesystemDAO.isFile(sourcePath)) {
					response.status(200);
					return filesystemDAO.moveFile(sourcePath, destinationPath);
				}
			}
		} else {
			response.status(401);
			return "Authorization required";
		}
		
			return null;
	}
	
	public Object handleRename(Request request, Response response) throws FileNotFoundException, SessionExpiredException, IllegalPathException, DirectoryNotFoundException {
		String session = request.queryParams("session");

		if ((request.params("path") == null) || (request.params("path").equals("")))
			throw new FileNotFoundException();
		
		String spath = "/"+auth.getSessionUserId(session)+"/"+request.params("path");
		Path path = Paths.get(spath);
		String newName = request.queryParams("new_name");
		
		if(auth.isSessionActive(session)) {
			if (filesystemDAO.exists(path)) {
				if (filesystemDAO.isDirectory(path)) {
					response.status(200);
					return filesystemDAO.renameFolder(path, newName);

				}
					
				
				if (filesystemDAO.isFile(path)) {
					response.status(200);
						return filesystemDAO.renameFile(path, newName);
				}
				
			}
		} else {
			response.status(401);
			return "Authorization required";
		}
		return null;
	}
}
