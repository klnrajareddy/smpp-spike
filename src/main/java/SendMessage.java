import org.apache.log4j.Logger;
import org.motechproject.SMPPMessageGateWay;

import java.io.IOException;

public class SendMessage {
    private static Logger LOGGER = Logger.getLogger(SendMessageWithConnectionRetry.class);

    public static void main(String args[]) throws IOException, InterruptedException {
        final String smscAddress = "localhost";
        final int smscPort = 8085;
        final String userName = "pavel";
        final String password = "wpsd";

        SMPPMessageGateWay messageGateWay = new SMPPMessageGateWay(smscAddress, smscPort, userName, password);

        messageGateWay.initialize();
        messageGateWay.sendMessage("9980930495", "Trying to send this message.");

        Thread.sleep(1000000);

        messageGateWay.close();
    }
}