package http;

import java.io.IOException;
import java.io.OutputStream;

public class HttpResponse {

	public static void response200(OutputStream output, String fileType) {
		write(output, "HTTP/1.1 200 OK\r\n");
		// Response different content type according to the file type
		fileType = fileType.toLowerCase();
		switch(fileType) {
			// Image
			case "gif": case "png": case "jpg": case "jpeg": case "bmp": case "webp":
				write(output, ("Content-Type:image/" + fileType + "\r\n"));
				break;
			// Text
			case "html": case "htm": case "txt": default: 
				write(output, "Content-Type:text/html\r\n");
				break;
		}
        write(output, "\r\n");
	}
	
	public static void response302(OutputStream output, String location) {
		write(output, "HTTP/1.1 302 Found\r\n");
    	write(output, "Content-Type:text/html\r\n");
    	write(output, ("Location:"+location));
	}
	
	public static void response403(OutputStream output) {
		write(output, "HTTP/1.1 403 Forbidden\r\n");
    	write(output, "Content-Type:text/html\r\n");
	}
	
	public static void response404(OutputStream output) {
		write(output, "HTTP/1.1 404 Not Found\r\n");
    	write(output, "Content-Type:text/html\r\n");
	}
	
	public static void response500(OutputStream output) {
		write(output, "HTTP/1.1 500 Internal Server Error\r\n");
    	write(output, "Content-Type:text/html\r\n");
	}
	
	public static void write(OutputStream output, String content) {
		try {
			output.write(content.getBytes());
		} catch (IOException e) {
			System.out.println("No output stream is available.");
		}
	}
	
	public static void write(OutputStream output, byte[] buf, int start, int len) {
		try {
			output.write(buf, 0, len);
		} catch (IOException e) {
			System.out.println("No output stream is available.");
		}
	}
	
}
