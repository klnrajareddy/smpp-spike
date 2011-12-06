import org.apache.log4j.Logger;
import org.motechproject.SMSGatewayWithSMSCReconnect;

import java.io.IOException;

public class SendMessageWithConnectionRetry {
    private static Logger LOGGER = Logger.getLogger(SendMessageWithConnectionRetry.class);

    public static void main(String args[]) throws IOException, InterruptedException {
        SMSGatewayWithSMSCReconnect messageGateWay = new SMSGatewayWithSMSCReconnect("localhost", 8085, "pavel", "wpsd");
        for(int i=0;i<1000;i++) {
            LOGGER.info(String.format("###################### Sending message %s th time",i));
            try {
                messageGateWay.sendMessage("Motech", "9980930495", "Trying to send this message.");
            } catch (Exception e) {
                LOGGER.error(String.format("###################### Sending %s th message failed.", i));
            }
            Thread.sleep(1000);
        }
    }
}