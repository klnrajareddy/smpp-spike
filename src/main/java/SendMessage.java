import org.apache.log4j.Logger;
import org.motechproject.SMPPMessageGateWay;

import java.io.IOException;

public class SendMessage {
    private static Logger LOGGER = Logger.getLogger(SendMessageWithConnectionRetry.class);

    public static void main(String args[]) throws IOException, InterruptedException {
        SMPPMessageGateWay messageGateWay = new SMPPMessageGateWay("localhost", 8085, "pavel", "wpsd");
        messageGateWay.sendMessage("9980930495", "Trying to send this message.");
        Thread.sleep(100000);
    }
}