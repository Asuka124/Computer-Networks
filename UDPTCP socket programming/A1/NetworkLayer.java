/*
  NetworkLayer.java
*/

package dv201.labb2;

public abstract class NetworkLayer {

    protected String ip;
    protected int port;
    protected int bufferSize;
    protected int transferRate;

    protected boolean validateArguments(String[] args) {
        boolean result = true;
        if (args.length != 4) {
            System.err.println("Invalid argument list. Please input IP, port, buffer size and transfer rate.");
            result = false;
        } else {
            // IP format check
            if (!validateIP(args[0])) {
                System.err.println("Bad IP address format.");
                result = false;
            } else {
                ip = args[0];
            }

            // Port check
            try {
                port = Integer.parseInt(args[1]);
                if (port < 1 || port > 65535) {
                    System.err.println("Invalid port number.");
                    result = false;
                }
            } catch (NumberFormatException exception) {
                System.err.println("Port in arguments is not an integer.");
                result = false;
            }

            // Buffer size check
            try {
                bufferSize = Integer.parseInt(args[2]);
                if (bufferSize < 1) {
                    System.err.println("Invalid buffer size.");
                    result = false;
                }
            } catch (NumberFormatException exception) {
                System.err.println("Buffer size in arguments is not an integer.");
                result = false;
            }

            // Transfer rate check
            try {
                transferRate = Integer.parseInt(args[3]);
                if (transferRate < 0) {
                    System.err.println("Invalid transfer rate.");
                    result = false;
                } else if (transferRate == 0) {
                    transferRate = 1;  // Message should be transferred at least once
                }
            } catch (NumberFormatException exception) {
                System.err.println("Transfer rate in arguments is not an integer.");
                result = false;
            }
        }

        return result;
    }

    private static boolean validateIP(final String ip) {
        String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";
        return ip.matches(PATTERN);
    }
}
