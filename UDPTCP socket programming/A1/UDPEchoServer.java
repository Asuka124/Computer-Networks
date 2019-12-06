/*
  UDPEchoServer.java
*/

package dv201.labb2;

import java.io.IOException;
import java.net.*;

public class UDPEchoServer {
    public static final int BUFSIZE = 128;
    public static final int MYPORT = 4950;

    public UDPEchoServer() {
        startListen();
    }

    private void startListen() {
        byte[] buf= new byte[BUFSIZE];

        /* Create socket */
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(null);
        } catch (SocketException exception) {
            System.err.println("Failed to create socket.");
            System.err.println(exception.getMessage());
            System.exit(1);
        }

        /* Create local bind point */
        SocketAddress localBindPoint= new InetSocketAddress(MYPORT);
        try {
            socket.bind(localBindPoint);
        } catch (SocketException exception) {
            System.err.println("Failed to bind socket.");
            System.err.println(exception.getMessage());
            System.exit(1);
        }

        System.out.println("UDP Server Start.");

        while (true) {
            /* Create datagram packet for receiving message */
            DatagramPacket receivePacket= new DatagramPacket(buf, buf.length);

            /* Receiving message */
            try {
                socket.receive(receivePacket);
            } catch (IOException exception) {
                System.err.println("I/O error while receiving.");
                System.err.println(exception.getMessage());
            }

            System.out.println("  Received message: " + new String(receivePacket.getData(), 0, receivePacket.getLength()));

            /* Create datagram packet for sending message */
            DatagramPacket sendPacket=
                    new DatagramPacket(receivePacket.getData(),
                            receivePacket.getLength(),
                            receivePacket.getAddress(),
                            receivePacket.getPort());

            /* Send message*/
            try {
                socket.send(sendPacket);
            } catch (IOException exception) {
                System.err.println("I/O error while sending response.");
                System.err.println(exception.getMessage());
            }
            System.out.printf("UDP echo request from %s", receivePacket.getAddress().getHostAddress());
            System.out.printf(" using port %d\n", receivePacket.getPort());
        }
    }

    public static void main(String[] args) throws IOException {
        UDPEchoServer server = new UDPEchoServer();
    } 
}