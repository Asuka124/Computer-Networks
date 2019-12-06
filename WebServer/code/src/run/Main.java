package run;
 
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import http.HttpServer;
 
public class Main {
	
    @SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
    	
    	// Check params
    	if(args.length<2) {
    		System.out.println("Please input the port and the root directory.");
    		System.exit(1);
    	}
    	
    	// Check port
    	int port = 9090;
    	try {
    		port = Integer.parseInt(args[0]);
    	}catch(Exception e) {
    		System.out.println("Please input a positive integer as the port.");
    		System.exit(1);
    	}
    	if(port<0 || port>65535) {
    		System.out.println("The range of the port is from 1 to 65535.");
    		System.exit(1);
    	}
    	
    	// Check root directory
    	String path = "";
    	for(int i=1; i<args.length; i++) {
    		path += args[i] + " ";
    	}
    	path = path.substring(0, path.length()-1);
    	File checkRoot = new File(path);
    	if(!checkRoot.exists()) {
    		System.out.println("The root directory is unreachable.");
    		System.exit(1);
    	}
    	if(!path.substring(path.length()-1, path.length()).equals("/"))
    		path += "/";

    	// Try to create server
    	ServerSocket server = null;
        try {
			server = new ServerSocket(port);
        }catch(Exception e) {
        	System.out.println("The port " + port + " is already in use.");
        	System.exit(1);
        }
        
        // Start
        System.out.println("The server starts");
        System.out.println("The port is " + port);
        System.out.println("The root path is " + path);
    	while (true) {
            Socket socket = server.accept();
            new HttpServer(socket, path);
        }

    }
}
