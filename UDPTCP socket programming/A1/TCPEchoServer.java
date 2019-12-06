/*
  UDPEchoServer.java
*/

package dv201.labb2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPEchoServer {
	public static final int BUFSIZE = 128;
	public static final int MYPORT = 4950;

	private ServerSocket serverSocket = null;

    public TCPEchoServer() {
    	try {
			serverSocket = new ServerSocket(MYPORT);
		} catch (IOException exception) {
			System.err.println("Failed to create socket.");
			System.err.println(exception.getMessage());
			System.exit(1);
		}
	}

	public void startListen() {
		if (serverSocket != null) {
			System.out.println("TCP Server started.");

			while (true) {
				Socket socket = null;
				try {
					socket = serverSocket.accept();
				} catch (IOException exception) {
					System.err.println("Failed to accept connection.");
					System.err.println(exception.getMessage());
				}

				if (socket != null) {
					System.out.println("Connection request accepted.");
					SocketThread socketThread = new SocketThread(socket);
					socketThread.start();
				}
			}
		}
	}

	private class SocketThread extends Thread {
		private Socket socket;
		byte[] buffer = new byte[BUFSIZE];
		int receiveSize;

    	SocketThread(Socket socket) {
    		this.socket = socket;
		}

		public void run() {
			try {
				System.out.println("TCP socket thread start.");
				InputStream inputStream = socket.getInputStream();
				OutputStream outputStream = socket.getOutputStream();

				while ((receiveSize = inputStream.read(buffer)) != -1) {
					System.out.println("  Received message: " + new String(buffer, 0, receiveSize));
					outputStream.write(buffer, 0, receiveSize);
				}
				socket.close();
			} catch (IOException exception) {
				System.err.println("I/O error while reading/writing through socket.");
				System.err.println(exception.getMessage());
			}
		}
	}

	public static void main(String[] args) {
		TCPEchoServer server = new TCPEchoServer();
		server.startListen();
	}
}