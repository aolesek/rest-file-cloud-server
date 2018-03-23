package pl.edu.agh.kis.florist.controller;

import static spark.Spark.*;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import pl.edu.agh.kis.florist.dao.FilesystemDAO;
import pl.edu.agh.kis.florist.exceptions.DirectoryAlreadyExistsException;
import pl.edu.agh.kis.florist.exceptions.DirectoryNotFoundException;
import pl.edu.agh.kis.florist.exceptions.IllegalPathException;
import pl.edu.agh.kis.florist.exceptions.ParameterFormatException;
import pl.edu.agh.kis.florist.model.ParameterFormatError;
import spark.Request;
import spark.ResponseTransformer;

public class App {

	final static private Logger LOGGER = LoggerFactory.getILoggerFactory().getLogger("requests");
	public static void main(String[] args) {

		final String CREATE_DIRECTORY_PATH = "/files/:path/create_directory";
		final String LIST_FOLDER_CONTENT_PATH = "/files/:path/list_folder_content";
		final String GET_META_DATA_PATH = "/files/:path/get_meta_data";
		final String DELETE_PATH = "/files/:path/delete";
		final String UPLOAD_PATH = "/files/upload";
		final String DOWNLOAD_PATH = "/files/:path/download";
		final String MOVE_PATH = "/files/:path/move";
		final String RENAME_PATH = "/files/:path/rename";
		final String CREATE_USER_PATH = "/users/create_user";
		final String ACCESS_PATH = "/users/access";

		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		final ResponseTransformer json = gson::toJson;
		
		final AuthController authController = new AuthController();
		final CloudController cloudController = new CloudController(authController);
		
		
		

		//secure("keystore.jks", "password", null, null);
		
		port(4567);

		before("/*/", (req, res) -> {
			info(req);
		});
		
		put(CREATE_DIRECTORY_PATH, (request, response) -> { return cloudController.handleCreateDirectory(request, response); }, json);
		
		get(LIST_FOLDER_CONTENT_PATH, (request, response) -> { return cloudController.handleListFolderContent(request, response); }, json);
		
		get(GET_META_DATA_PATH, (request, response) -> { return cloudController.handleGetMetadata(request, response); }, json);

		delete(DELETE_PATH, (request, response) -> { return cloudController.handleDeleteFolder(request, response); }, json);

		post(UPLOAD_PATH, "multipart/form-data", (request, response) -> { return cloudController.handleUploadFile(request, response); }, json);
		
		get(DOWNLOAD_PATH, (request, response) -> { return cloudController.handleDownloadFile(request, response); }, json);
		
		put(MOVE_PATH, (request, response) -> { return cloudController.handleMove(request, response); }, json);
		
		put(RENAME_PATH, (request, response) -> { return cloudController.handleRename(request, response); }, json);
		
		post(CREATE_USER_PATH, (request, response) -> { return authController.handleCreateUser(request, response); }, json);
		
		post(ACCESS_PATH, (request, response) -> { return authController.handleAccess(request, response); });
				
		
		exception(DirectoryAlreadyExistsException.class,(ex,request,response) -> {
			response.status(501);
			response.body("<h1>501</h1> Invalid path parameter.");
		});
		
		exception(DirectoryNotFoundException.class,(ex,request,response) -> {
			response.status(404);
			response.body("<h1>404</h1> Folder (Path) does not exist");
		});
		
		exception(IllegalPathException.class,(ex,request,response) -> {
			response.status(404);
			response.body("<h1>404</h1> Folder (Path) does not exist");
		});
		
		exception(Exception.class,(ex,request,response) -> {
			response.status(404);
			response.body(ex.getMessage());
			
			
		});		

	}

	private static void info(Request req) {
		LOGGER.info("{}", req);
	}

}
