import org.apache.log4j.Logger;
import org.smslib.*;
import org.smslib.smpp.BindAttributes;
import org.smslib.smpp.jsmpp.JSMPPGateway;

public class SendMessageWithSMSLib {
    private static Logger LOGGER = Logger.getLogger(SendMessageWithSMSLib.class);

    public static void main(String args[]) throws Exception {
        LOGGER.info("Starting with main...");
        final String smscAddress = "localhost";
        final int smscPort = 8085;
        final String userName = "pavel";
        final String password = "wpsd";

        LOGGER.info("==================================================== Binding Attrs.");
        BindAttributes bindAttributes = new BindAttributes(userName, password, null, BindAttributes.BindType.TRANSCEIVER);
        LOGGER.info("==================================================== Constructing a JSMPPGateway with the binding attrs.");
        JSMPPGateway jsmppGateway = new JSMPPGateway("test", smscAddress, smscPort, bindAttributes);

        LOGGER.info("==================================================== Getting a service instance.");
        final Service smsService = Service.getInstance();
        LOGGER.info("==================================================== Adding the jsmppgateway to it.");
        smsService.addGateway(jsmppGateway);

        LOGGER.info("==================================================== Setting the listeners...");
        smsService.setInboundMessageNotification(new InboundNotification());
        smsService.setGatewayStatusNotification(new GatewayStatusNotification());
        smsService.setOutboundMessageNotification(new OutboundNotification());

        LOGGER.info("==================================================== Starting the service...");
        smsService.startService();

        for(int i=0;i<1;i++) {
            LOGGER.info(String.format("###################### Sending message %s th time",i));
            OutboundMessage msg = new OutboundMessage("9980930495", "Test message from smslib.");
            smsService.sendMessage(msg);
            LOGGER.info("Sent");
            Thread.sleep(1000);
        }
        LOGGER.info("################ Now Sleeping - Hit <enter> to terminate.");
        System.in.read();
        smsService.stopService();
    }

    public static class OutboundNotification implements IOutboundMessageNotification
	{
		public void process(AGateway gateway, OutboundMessage msg)
		{
			LOGGER.info("Outbound handler called from Gateway: " + gateway.getGatewayId());
			LOGGER.info(msg);
		}
	}

	public static class InboundNotification implements IInboundMessageNotification
	{
		public void process(AGateway gateway, Message.MessageTypes msgType, InboundMessage msg)
		{
			if (msgType == Message.MessageTypes.INBOUND) LOGGER.info(">>> New Inbound message detected from Gateway: " + gateway.getGatewayId());
			else if (msgType == Message.MessageTypes.STATUSREPORT) LOGGER.info(">>> New Inbound Status Report message detected from Gateway: " + gateway.getGatewayId());
			LOGGER.info(msg);
		}
	}

	public static class GatewayStatusNotification implements IGatewayStatusNotification
	{
		public void process(AGateway gateway, AGateway.GatewayStatuses oldStatus, AGateway.GatewayStatuses newStatus)
		{
			LOGGER.info(">>> Gateway Status change for " + gateway.getGatewayId() + ", OLD: " + oldStatus + " -> NEW: " + newStatus);
		}
	}

}