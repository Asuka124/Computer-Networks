package http;
 
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
 
public class HttpServer {

	private Socket socket;
	
	private final String DEFAULTFILE = "index.html";
	private String ROOT;
	private String uploadPath = "upload/";
	
	private ArrayList<String> allowMethod = new ArrayList<String>();

	private InputStream input;
	private OutputStream output; 

	public HttpServer(Socket socket, String root) {
		ROOT = root;
		this.socket = socket;
		allowMethod.add("GET");
		allowMethod.add("POST");
		allowMethod.add("PUT");
		// Create input and output stream
		try {
			input = socket.getInputStream();
			output = socket.getOutputStream();
		} catch (IOException e1) {
			System.out.println("Create stream failed.");
		}
		Server server = new Server();
		server.start();
	}

	private class Server extends Thread {
		@SuppressWarnings("resource")
		public void run() {
			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			// Read the header
			String line = "";
			try {
				line = br.readLine();
			} catch (IOException e) {
				System.out.println("No input stream.");
			}
            System.out.println(line);
            
            String[] lineArray = null;
            try {
            	lineArray = line.split(" ");
            }catch(Exception e) {
            	HttpResponse.response500(output);
	            closeSocket();
	            return;
            }
            
            // Deal with the header
            if(lineArray.length<2) {
            	HttpResponse.response403(output);
            	closeSocket();
            	return;
            }
            
            // Deal with the method
            String method = lineArray[0];
            if(!allowMethod.contains(method)) {
            	HttpResponse.response403(output);
            	closeSocket();
            	return;
            }
            
            // Deal with the path
            String[] pathDeal = lineArray[1].substring(1).split("\\?");
            String path = pathDeal[0];	// pathDeal[1] is param
            
            // Deal with the upload
            if(method.equals("POST") && path.equals("upload.do")) {
            	File fileText = new File(ROOT+uploadPath + System.currentTimeMillis() + ".png");
            	if(!fileText.exists()) {
					try {
						fileText.createNewFile();
					} catch (IOException e) {
						System.out.println("Upload failed(File creates failed).");
						HttpResponse.write(output, "Upload failed.");
			            closeSocket();
			            return;
					}
            	}	
            	FileWriter fileWriter = null;
				try {
					fileWriter = new FileWriter(fileText);
				} catch (IOException e) {
					System.out.println("Upload failed(File input failed).");
					HttpResponse.write(output, "Upload failed.");
		            closeSocket();
		            return;
				}
            	int count = 0;
            	byte[] fileData = new byte[count];
				try {
					count = input.available();
					input.read(fileData);
	            	fileWriter.write(new String(fileData));
	            	fileWriter.close();
				} catch (IOException e) {
					System.out.println("Upload failed(Read the input stream failed).");
					HttpResponse.write(output, "Upload failed.");
		            closeSocket();
		            return;
				}
            	HttpResponse.response200(output, "");
	            HttpResponse.write(output, "Upload successfully");
	            closeSocket();
	            return;
            }
            
            // Test the PUT
            if(method.equals("PUT")) {
            	HttpResponse.response200(output, "");
	            HttpResponse.write(output, "PUT successfully");
	            closeSocket();
	            return;
            }
            
            // Read the path
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(ROOT + path);
            } catch (IOException e) {
            	String directory = path;
            	try {
            		if(!path.equals("") && !path.equals("/") && !path.substring(path.length()-1, path.length()).equals("/"))
            			path += "/";
            		path += DEFAULTFILE;
            		fis = new FileInputStream(ROOT + path);
            	}catch(IOException e2) {
            		File dir = new File(ROOT+directory);
            		if(dir.isDirectory()) {
            			HttpResponse.response200(output, "");
            			HttpResponse.write(output, "<h2>Directory: "+dir.getName()+"</h2>");
            	        File[] files = dir.listFiles();
            	        if (files != null) {
            	            for (int i = 0; i < files.length; i++) {
            	                String fileName = files[i].getName();
            	                HttpResponse.write(output, "<a href=\""+dir.getName()+"/"+fileName+"\" style=\"padding-left:10px\">"+fileName+"</a></br>");
            	            }
            	        }
            	        
            		}else {
            			HttpResponse.response404(output);
            		}
		            closeSocket();
		            return;
            	}
            }
            
            // File Type
            String fileTypearr[] = path.split("\\.");
            String fileType = fileTypearr.length==1 ? "" : fileTypearr[fileTypearr.length-1];
            
            // *** Do the 302 test
            if(path.equals("302ToGoogle.html")) {
            	HttpResponse.response302(output, "https://google.se");
	            closeSocket();
	            return;
            }
            
            // *** Do the 403 test
            if(path.equals("403.html")) {
            	HttpResponse.response403(output);
	            closeSocket();
	            return;
            }
            
            // *** Do the 500 test
            if(path.equals("500.html")) {
            	HttpResponse.response500(output);
	            closeSocket();
	            return;
            }
            
            // Response
            byte[] buffer = new byte[1024];
            int len = -1;
            try {
            	HttpResponse.response200(output, fileType);
				while ((len = fis.read(buffer)) != -1) {
				    HttpResponse.write(output, buffer, 0, len);
				}
				fis.close();
			} catch (IOException e2) {
				System.out.println("Read the path failed.");
				HttpResponse.response500(output);
			}
            closeSocket();
            return;

	    }
	}
	
	private void closeSocket() {
		try {
			socket.shutdownOutput();
		} catch (IOException e) {
			System.out.println("Socket shut down the output stream failed.");
		}
    	try {
			output.close();
		} catch (IOException e) {
			System.out.println("Output close failed.");
		}
	}
	
}
