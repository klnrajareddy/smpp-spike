package org.motechproject;

import org.apache.log4j.Logger;
import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.*;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.InvalidDeliveryReceiptException;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SMSGatewayWithSMSCReconnect {
    private static final Logger LOGGER = Logger.getLogger(SMSGatewayWithSMSCReconnect.class);

    private AtomicReference<SMPPSession> smppSessionHolder = new AtomicReference<SMPPSession>();
    private AtomicBoolean currentlyInitializingSMPPSession = new AtomicBoolean();

    private String smscAddress;
    private int smscPort;
    private String userName;
    private String password;


    public SMSGatewayWithSMSCReconnect(String smscAddress, int smscPort, String userName, String password) {
        this.smscAddress = smscAddress;
        this.smscPort = smscPort;
        this.userName = userName;
        this.password = password;
        currentlyInitializingSMPPSession.set(false);
        initializeSMPPSession();
    }

    private void initializeSMPPSession() {
        if(currentlyInitializingSMPPSession.compareAndSet(false, true)) {
            try {
                LOGGER.info("###################### Trying to connect and bind...");
                BindParameter bindParameter = new BindParameter(BindType.BIND_TRX, this.userName, this.password, null, TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null);
                SMPPSession smppSession = new SMPPSession();
                smppSession.connectAndBind(smscAddress, smscPort, bindParameter);
                smppSession.setMessageReceiverListener(new SMSCMessageListener());
                smppSessionHolder.set(smppSession);
                LOGGER.info("###################### Binding successful.");
            } catch (Exception e) {
                LOGGER.error("###################### Could not bind to the SMSC, will not propagate this exception.", e);
            } finally {
                currentlyInitializingSMPPSession.set(false);
            }
        }
    }

    public void sendMessage(String sourceAddr, String destinationAddr, String message) {
        SMPPSession smppSession = smppSessionHolder.get();
        try {
            final String serviceType = null;

            final ESMClass messageTypeClass = new ESMClass(MessageType.DEFAULT.value());
            final byte protocolId = (byte) 0;
            final byte priority = (byte) 1;
            final byte replaceIfPresentFlag = (byte) 0;
            final String scheduleDeliveryTime = new AbsoluteTimeFormatter().format(new Date());

            final byte smDefaultMsgId = (byte) 0;
            final String validityPeriod = null;


            String messageId = smppSession.submitShortMessage(serviceType, TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN,
                                            sourceAddr, TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN,
                                            destinationAddr, messageTypeClass, protocolId, priority,
                                            scheduleDeliveryTime, validityPeriod, new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE),
                                            replaceIfPresentFlag, new GeneralDataCoding(), smDefaultMsgId, message.getBytes());

            LOGGER.info(String.format("###################### Message submitted, message ID is %s.", messageId));
        } catch (Exception e) {
            LOGGER.error("###################### An exception has occurred, will try to reconnect and propagate the exception.", e);
            closeSessionSilently(smppSession);
            initializeSMPPSession();
            throw new RuntimeException(e);
        }
    }

    public void closeSessionSilently(SMPPSession smppSession) {
        try {
            smppSession.unbindAndClose();
        } catch(Exception ignore) {

        }
    }

    private static class SMSCMessageListener implements MessageReceiverListener {
        public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
            if(MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
                try {
                    DeliveryReceipt deliveryReceipt = deliverSm.getShortMessageAsDeliveryReceipt();
                    LOGGER.info(String.format("Receiving delivery receipt for message ID %s from %s to %s: %s", deliveryReceipt.getId(), deliverSm.getSourceAddr(), deliverSm.getDestAddress(), deliveryReceipt));
                } catch(InvalidDeliveryReceiptException invalidDeliveryReceiptException) {
                    LOGGER.error("Error while parsing the delivery receipt");
                }
            }
            else {
                 LOGGER.info(String.format("Message that I am getting is: %s, and the message class is: %d", new String(deliverSm.getShortMessage()), deliverSm.getEsmClass()));
            }
        }

        public void onAcceptAlertNotification(AlertNotification alertNotification) {
        }

        public DataSmResult onAcceptDataSm(DataSm dataSm, Session source) throws ProcessRequestException {
            return null;
        }
    }

}