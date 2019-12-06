/*
  TCPEchoClient.java
*/

package dv201.labb2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TCPEchoClient extends NetworkLayer {
	public static final int BUFSIZE = 64;
	public static final int MYPORT = 4950;
	public static final String MSG = "An Echo Message!!!!!";

	public TCPEchoClient(String[] args) {
		if (!validateArguments(args)) {
			System.exit(1);
		}
	}

	private void startSend() {
		byte[] message = MSG.getBytes();
		byte[] buffer = new byte[bufferSize > 0 ? bufferSize : BUFSIZE];
		int messageLeft = transferRate;

		long startTime = System.currentTimeMillis();
		long currentTime = 0L;
		while (messageLeft > 0) {
			currentTime = System.currentTimeMillis();
			System.out.println(String.format("%d message(s) to be sent. %dms have elapsed.", messageLeft, currentTime - startTime));
			if (currentTime - startTime > 1000) {
				System.out.println(String.format("One second passed, the amount of remaining messages is %d", messageLeft));
				break;
			}

			Socket socket = null;
			try {
				socket = new Socket(ip, port);
			} catch (IOException exception) {
				System.err.println("Failed to create socket.");
				System.err.println(exception.getMessage());
				messageLeft--;
				continue;
			}

			try {
				InputStream inputStream = socket.getInputStream();
				OutputStream outputStream = socket.getOutputStream();

				outputStream.write(message);
				int receivedLength = inputStream.read(buffer);
				String receivedString = new String(buffer, 0, receivedLength);
				if (receivedString.compareTo(MSG) == 0)
					System.out.println(String.format("  %d bytes sent and received.", receivedLength));
				else
					System.out.println("  Sent and received msg not equal!");

				socket.close();
			} catch (IOException exception) {
				System.err.println("Failed to get input/output stream.");
				System.err.println(exception.getMessage());
			}

			messageLeft--;
		}
	}

	public static void main(String[] args) {
		TCPEchoClient client = new TCPEchoClient(args);
		client.startSend();
	}
}