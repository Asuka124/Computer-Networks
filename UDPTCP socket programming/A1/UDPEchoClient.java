/*
  UDPEchoClient.java
*/

package dv201.labb2;
import java.io.IOException;
import java.net.*;

public class UDPEchoClient extends NetworkLayer {
    public static final int BUFSIZE = 64;
    public static final int MYPORT = 4950;
    public static final String MSG = "An Echo Message!!!!!";

    public UDPEchoClient(String[] args) {
        if (!validateArguments(args)) {
            System.exit(1);
        }
    }

    private void startSend() {
        byte[] buffer = new byte[bufferSize > 0 ? bufferSize : BUFSIZE];
        int messageLeft = transferRate;

        /* Create socket */
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(null);
        } catch (SocketException exception) {
            System.err.println("Failed to create socket.");
            System.err.println(exception.getMessage());
            System.exit(1);
        }

        /* Create local endpoint using bind() */
        SocketAddress localBindPoint= new InetSocketAddress(MYPORT);
        try {
            socket.bind(localBindPoint);
        } catch (SocketException exception) {
            System.err.println("Failed to bind socket.");
            System.err.println(exception.getMessage());
            System.exit(1);
        }

        try {
            socket.setSoTimeout(100);  // 100ms timeout
        } catch (SocketException exception) {
            System.err.println("Failed to set socket timeout.");
            System.err.println(exception.getMessage());
        }

        /* Create remote endpoint */
        SocketAddress remoteBindPoint = new InetSocketAddress(ip, port);

        long startTime = System.currentTimeMillis();
        long currentTime = 0L;
        while (messageLeft > 0) {
            currentTime = System.currentTimeMillis();
            System.out.println(String.format("%d message(s) to be sent. %dms have elapsed.", messageLeft, currentTime - startTime));
            if (currentTime - startTime > 1000) {
                System.out.println(String.format("One minute passed, the amount of remaining messages is %d", messageLeft));
                break;
            }

            /* Create datagram packet for sending message */
            DatagramPacket sendPacket = new DatagramPacket(MSG.getBytes(), MSG.length(), remoteBindPoint);

            /* Create datagram packet for receiving echoed message */
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

            /* Send and receive message*/
            try {
                socket.send(sendPacket);
                socket.receive(receivePacket);
            } catch (SocketTimeoutException exception) {
                System.err.println("Timeout while sending/receiving message.");
                System.err.println(exception.getMessage());
                messageLeft--;
                continue;
            } catch (IOException exception) {
                System.err.println("I/O error while sending/receiving message.");
                System.err.println(exception.getMessage());
                messageLeft--;
                continue;
            }

            /* Compare sent and received message */
            String receivedString = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
            if (receivedString.compareTo(MSG) == 0)
                System.out.println(String.format("  %d bytes sent and received.", receivePacket.getLength()));
            else
                System.out.println("  Sent and received msg not equal!");

            messageLeft--;
        }

        socket.close();
    }

    public static void main(String[] args) {
        UDPEchoClient client = new UDPEchoClient(args);
        client.startSend();
    }
}