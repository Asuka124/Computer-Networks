package assignment3;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;


public class TFTPServer {
	public static final int TFTPPORT = 4970;
	public static final int BUFSIZE = 516;
	public static final int TFTP_MAX_BLOCK_SIZE = 512;
	// public static final String READDIR = "/home/username/read/"; //custom address
	// public static final String WRITEDIR = "/home/username/write/"; //custom
	// address
	public static final String READDIR = "/tmp/"; // custom address
	public static final String WRITEDIR = "/tmp/"; // custom address

	public static final int MAX_RETRY_TIMES = 5;
	// OP codes
	public static final int OP_RRQ = 1;
	public static final int OP_WRQ = 2;
	public static final int OP_DAT = 3;
	public static final int OP_ACK = 4;
	public static final int OP_ERR = 5;

	public static final short TFTP_ERROR_CODE_UNKNOWN_ERROR = 0;
	public static final short TFTP_ERROR_CODE_FILE_NOT_FOUND = 1;
	public static final short TFTP_ERROR_CODE_ACCESS_VIOLATION = 2;
	public static final short TFTP_ERROR_CODE_DISK_FULL = 3;
	public static final short TFTP_ERROR_CODE_ILLEGAL_OPERATION = 4;
	public static final short TFTP_ERROR_CODE_UNKNOWN_TRANSFER_ID = 5;
	public static final short TFTP_ERROR_CODE_FILE_ALREADY_EXISTS = 6;
	public static final short TFTP_ERROR_CODE_FILE_NO_SUCH_USER = 7;

	public static final String TFTP_ERROR_MSG_UNKNOWN_ERROR = "Unknown error";
	public static final String TFTP_ERROR_MSG_FILE_NOT_FOUND = "File not found";
	public static final String TFTP_ERROR_MSG_ACCESS_VIOLATION = "Access violation";
	public static final String TFTP_ERROR_MSG_DISK_FULL = "Disk full or allocation exceeded";
	public static final String TFTP_ERROR_MSG_ILLEGAL_OPERATION = "Illegal TFTP operation";
	public static final String TFTP_ERROR_MSG_UNKNOWN_TRANSFER_ID = "Unknown transfer ID";
	public static final String TFTP_ERROR_MSG_FILE_ALREADY_EXISTS = "File already exists";
	public static final String TFTP_ERROR_MSG_FILE_NO_SUCH_USER = "No such user";

	public static int client_TID;

	public static void main(String[] args) {
		if (args.length > 0) {
			System.err.printf("usage: java %s\n", TFTPServer.class.getCanonicalName());
			System.exit(1);
		}
		// Starting the server
		try {
			TFTPServer server = new TFTPServer();
			server.start();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private void start() throws SocketException {
		byte[] buf = new byte[BUFSIZE];

		// Create socket
		DatagramSocket socket = new DatagramSocket(null);

		// Create local bind point
		SocketAddress localBindPoint = new InetSocketAddress(TFTPPORT);
		socket.bind(localBindPoint);

		System.out.printf("Listening at port %d for new requests\n", TFTPPORT);

		// Loop to handle client requests
		while (true) {

			final InetSocketAddress clientAddress = receiveFrom(socket, buf);

			// If clientAddress is null, an error occurred in receiveFrom()
			if (clientAddress == null)
				continue;

			final StringBuffer requestedFile = new StringBuffer();
			final int reqtype = ParseRQ(buf, requestedFile);
			if (reqtype == -1) {
				continue;
			}

			new Thread() {
				public void run() {
					try {
						DatagramSocket sendSocket = new DatagramSocket(0);
						// socket timeout
						sendSocket.setSoTimeout(1000);

						// Connect to client
						sendSocket.connect(clientAddress);

						client_TID = sendSocket.getPort();

						System.out.printf("%s request for %s from %s using port %d\n",
								(reqtype == OP_RRQ) ? "Read" : "Write", requestedFile.toString(),
								clientAddress.getHostName(), clientAddress.getPort());

						// Read request
						if (reqtype == OP_RRQ) {
							requestedFile.insert(0, READDIR);
							HandleRQ(sendSocket, requestedFile.toString(), OP_RRQ);
						}
						// Write request
						else {
							requestedFile.insert(0, WRITEDIR);
							HandleRQ(sendSocket, requestedFile.toString(), OP_WRQ);
						}
						sendSocket.close();
					} catch (SocketException e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}

	/**
	 * Reads the first block of data, i.e., the request for an action (read or
	 * write).
	 * 
	 * @param socket
	 *            (socket to read from)
	 * @param buf
	 *            (where to store the read data)
	 * @return socketAddress (the socket address of the client)
	 */
	private InetSocketAddress receiveFrom(DatagramSocket socket, byte[] buf) {
		// Create datagram packet
		DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);

		try {
			// Receive packet
			socket.receive(recvPacket);
		} catch (SocketException e) {
			System.out.println("catch a socket exception " + e.getMessage());
		} catch (IOException e) {
			System.out.println("catch an io exception " + e.getMessage());
		}

		// Get client address and port from the packet
		return (InetSocketAddress) recvPacket.getSocketAddress();
	}

	/**
	 * Parses the request in buf to retrieve the type of request and requestedFile
	 * 
	 * @param buf
	 *            (received request)
	 * @param requestedFile
	 *            (name of file to read/write)
	 * @return opcode (request type: RRQ or WRQ)
	 */
	private int ParseRQ(byte[] buf, StringBuffer requestedFile) {
		int opcode = (buf[0] & 0xFF) << 8 | (buf[1] & 0xFF);
		// See "TFTP Formats" in TFTP specification for the RRQ/WRQ request contents
		if (opcode != OP_RRQ && opcode != OP_WRQ) {
			System.out.println("receive an invalid request");
			return -1;
		}

		for (int i = 2; buf[i] != 0; i++) {
			requestedFile.append((char) buf[i]);
		}

		return opcode;
	}

	/**
	 * Handles RRQ and WRQ requests
	 * 
	 * @param sendSocket
	 *            (socket used to send/receive packets)
	 * @param requestedFile
	 *            (name of file to read/write)
	 * @param opcode
	 *            (RRQ or WRQ)
	 */
	private void HandleRQ(DatagramSocket sendSocket, String requestedFile, int opcode) {
		if (opcode == OP_RRQ) {
			// See "TFTP Formats" in TFTP specification for the DATA and ACK packet contents
			boolean result = send_DATA_receive_ACK(sendSocket, requestedFile);
		} else if (opcode == OP_WRQ) {
			boolean result = receive_DATA_send_ACK(sendSocket, requestedFile);
		} else {
			System.err.println("Invalid request. Sending an error packet.");
			// See "TFTP Formats" in TFTP specification for the ERROR packet contents
			send_ERR(sendSocket, TFTP_ERROR_CODE_ILLEGAL_OPERATION, TFTP_ERROR_MSG_ILLEGAL_OPERATION);
			return;
		}
	}

	private boolean send_DATA_receive_ACK(DatagramSocket sendSocket, String requestedFile) {
		try {
			File file = new File(requestedFile);
			if (!file.exists() || !file.isFile() || !file.canRead()) {
				// send error packet
				System.out.printf("RRQ: %s not found!\n", requestedFile);
				send_ERR(sendSocket, TFTP_ERROR_CODE_FILE_NOT_FOUND, TFTP_ERROR_MSG_FILE_NOT_FOUND);
				return false;
			}

			int blockNum = 1;
			FileInputStream inputStream = new FileInputStream(file);
			while (true) {
				int offset = 0;
				byte[] dataPacketBuf = new byte[BUFSIZE];
				// opcode
				dataPacketBuf[0] = (byte) (OP_DAT >>> 8);
				dataPacketBuf[1] = (byte) (OP_DAT % 256);
				offset += 2;
				// block number
				dataPacketBuf[2] = (byte) (blockNum >>> 8);
				dataPacketBuf[3] = (byte) (blockNum % 256);
				offset += 2;
				// data
				int bytesRead = inputStream.read(dataPacketBuf, offset, TFTP_MAX_BLOCK_SIZE);
				if (bytesRead < 0) {
					// Read file done
					bytesRead = 0;
				}
				sendSocket.send(new DatagramPacket(dataPacketBuf, bytesRead + 4));

				int retryTimes = 0;
				while (retryTimes < MAX_RETRY_TIMES) {
					try {
						byte[] ackPacketBuf = new byte[BUFSIZE];
						DatagramPacket ackPacket = new DatagramPacket(ackPacketBuf, BUFSIZE);
						sendSocket.receive(ackPacket);
						if (client_TID != sendSocket.getPort()) {
							send_ERR(sendSocket, TFTP_ERROR_CODE_UNKNOWN_TRANSFER_ID,
									TFTP_ERROR_MSG_UNKNOWN_TRANSFER_ID);
							throw new SocketTimeoutException("The packet lost, resend!");
						}

						int opcode = (ackPacketBuf[0] & 0xFF) << 8 | (ackPacketBuf[1] & 0xFF);
						int ackBlockNum = (ackPacketBuf[2] & 0xFF) << 8 | (ackPacketBuf[3] & 0xFF);
						if (opcode == OP_ERR) {
							send_ERR(sendSocket, TFTP_ERROR_CODE_UNKNOWN_ERROR, TFTP_ERROR_MSG_UNKNOWN_ERROR);
							return false;
						}
						if (opcode == OP_ACK && ackBlockNum == blockNum) {
							break;
						} else {
							throw new SocketTimeoutException("The packet lost, resend!");
						}
					} catch (SocketTimeoutException t) {
						System.out.println("resend block " + blockNum);
						retryTimes++;
						sendSocket.send(new DatagramPacket(dataPacketBuf, bytesRead + 4));
					}
				}

				if (retryTimes == MAX_RETRY_TIMES) {
					// send_ERR(sendSocket, )
					System.out.println("send file failed.\n");
					break;
				}

				if (bytesRead == 0 || bytesRead < TFTP_MAX_BLOCK_SIZE) {
					System.out.println("File was sent completely.");
					break;
				}

				blockNum++;
			}

		} catch (Exception e) {
			System.out.print("exception caught while send_DATA_receive_ACK: " + e.getMessage());
		}

		return true;
	}

	private boolean receive_DATA_send_ACK(DatagramSocket sendSocket, String requestedFile) {
		try {
			File file = new File(requestedFile);
			if (file.exists()) {
				send_ERR(sendSocket, TFTP_ERROR_CODE_FILE_ALREADY_EXISTS, TFTP_ERROR_MSG_FILE_ALREADY_EXISTS);
				return false;
			}

			int expectedBlockNum = 1;
			FileOutputStream outputStream = new FileOutputStream(file);
			// send ack 0
			send_ACK(sendSocket, 0);

			while (true) {
				int retryTimes = 0;
				int bytesWritten = TFTP_MAX_BLOCK_SIZE;
				byte[] dataPacketBuf = new byte[BUFSIZE];
				while (retryTimes < MAX_RETRY_TIMES) {
					DatagramPacket dataPacket = new DatagramPacket(dataPacketBuf, BUFSIZE);
					sendSocket.receive(dataPacket);
					if (client_TID != sendSocket.getPort()) {
						send_ERR(sendSocket, TFTP_ERROR_CODE_UNKNOWN_TRANSFER_ID, TFTP_ERROR_MSG_UNKNOWN_TRANSFER_ID);
						throw new SocketTimeoutException("packet may be lost, resend!");
					}
					int opcode = (dataPacketBuf[0] & 0xFF) << 8 | (dataPacketBuf[1] & 0xFF);
					if (opcode == OP_ERR) {
						send_ERR(sendSocket, TFTP_ERROR_CODE_UNKNOWN_ERROR, TFTP_ERROR_MSG_UNKNOWN_ERROR);
						return false;
					}

					if (opcode != OP_DAT) {
						System.out.println("receive_DATA_send_ACK | invalid opcode, expect OP_DAT");
						send_ERR(sendSocket, TFTP_ERROR_CODE_ILLEGAL_OPERATION, TFTP_ERROR_MSG_ILLEGAL_OPERATION);
						retryTimes++;
						continue;
					}

					int blockNum = (dataPacketBuf[2] & 0xFF) << 8 | (dataPacketBuf[3] & 0xFF);
					if (blockNum != expectedBlockNum) {
						System.out.println("receive_DATA_send_ACK | invalid blockNum, expect " + expectedBlockNum);
						// resend ack pkt
						if (expectedBlockNum != 1) {
							send_ACK(sendSocket, expectedBlockNum - 1);
						}
						retryTimes++;
						continue;
					}

					// data received successfully
					int dataPacketLen = dataPacket.getLength();
					long diskFreeSpace = new File(WRITEDIR).getUsableSpace();
					if (dataPacketLen > diskFreeSpace) {
						send_ERR(sendSocket, TFTP_ERROR_CODE_DISK_FULL, TFTP_ERROR_MSG_DISK_FULL);
						return false;
					}
					outputStream.write(dataPacketBuf, 4, dataPacketLen - 4);
					bytesWritten = dataPacketLen - 4;
					send_ACK(sendSocket, expectedBlockNum);
					expectedBlockNum++;
					break;
				}

				if (retryTimes == MAX_RETRY_TIMES) {
					System.out.println("recv file failed.\n");
					break;
				}

				if (bytesWritten < TFTP_MAX_BLOCK_SIZE) {
					System.out.println("File was recved completely.");
					break;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
			send_ERR(sendSocket, TFTP_ERROR_CODE_ACCESS_VIOLATION, TFTP_ERROR_MSG_ACCESS_VIOLATION);
			System.out.print("Exception caught while receive_DATA_send_ACK: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			send_ERR(sendSocket, TFTP_ERROR_CODE_UNKNOWN_ERROR, TFTP_ERROR_MSG_UNKNOWN_ERROR);
			System.out.print("Exception caught while receive_DATA_send_ACK: " + e.getMessage());
		}

		return true;
	}

	private void send_ACK(DatagramSocket sendSocket, int blockNum) {
		try {
			byte[] ackPacketBuf = new byte[BUFSIZE];
			// opcode
			ackPacketBuf[0] = (byte) (OP_ACK >>> 8);
			ackPacketBuf[1] = (byte) (OP_ACK % 256);
			// block num
			ackPacketBuf[2] = (byte) (blockNum >>> 8);
			ackPacketBuf[3] = (byte) (blockNum % 256);

			int packetLen = 4;
			sendSocket.send(new DatagramPacket(ackPacketBuf, packetLen));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception caught while send error packet: " + e.getMessage());
		}
	}

	private void send_ERR(DatagramSocket sendSocket, short errorCode, String errorMsg) {
		try {
			byte[] ackPacketBuf = new byte[BUFSIZE];
			// opcode
			ackPacketBuf[0] = (byte) (OP_ERR >>> 8);
			ackPacketBuf[1] = (byte) (OP_ERR % 256);
			// error code
			ackPacketBuf[2] = (byte) (errorCode >>> 8);
			ackPacketBuf[3] = (byte) (errorCode % 256);
			// error msg
			errorMsg.getBytes(0, errorMsg.length(), ackPacketBuf, 4);
			ackPacketBuf[4 + errorMsg.length()] = 0;

			int packetLen = 4 + errorMsg.length() + 1;
			sendSocket.send(new DatagramPacket(ackPacketBuf, packetLen));
		} catch (Exception e) {
			System.out.println("Exception caught while send error packet: " + e.getMessage());
		}
	}

}
