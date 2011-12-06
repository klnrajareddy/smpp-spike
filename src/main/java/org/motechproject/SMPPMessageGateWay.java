package org.motechproject;

import org.apache.log4j.Logger;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.*;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.InvalidDeliveryReceiptException;

import java.io.IOException;
import java.util.Date;

public class SMPPMessageGateWay {
    private static final Logger LOGGER = Logger.getLogger(SMPPMessageGateWay.class);

    private SMPPSession smppSession;
    private String smscAddress;
    private int smscPort;
    private String userName;
    private String password;

    public SMPPMessageGateWay(String smscAddress, int smscPort, String userName, String password) {
        this.smscAddress = smscAddress;
        this.smscPort = smscPort;
        this.userName = userName;
        this.password = password;
    }

    public void initialize() throws IOException {
        BindParameter bindParameter = new BindParameter(BindType.BIND_TRX, this.userName, this.password, null, TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null);
        this.smppSession = new SMPPSession();
        smppSession.connectAndBind(smscAddress, smscPort, bindParameter);

        smppSession.setMessageReceiverListener(new SMSCMessageListener());
    }

    public void sendMessage(String destinationAddr, String message) {
        try {
            final String serviceType = null;
            final String sourceAddr = "MOTECH";

            final ESMClass messageTypeClass = new ESMClass(MessageType.DEFAULT.value());
            final byte protocolId = (byte) 0;
            final byte protocolPriority = (byte) 1;
            final byte replaceIfPresentFlag = (byte) 0;
            final String scheduleDeliveryTime = new AbsoluteTimeFormatter().format(new Date());

            final byte smDefaultMsgId = (byte) 1234;

            String messageId = smppSession.submitShortMessage(serviceType, TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN,
                                            sourceAddr, TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.UNKNOWN,
                                            destinationAddr, messageTypeClass, protocolId, protocolPriority,
                                            scheduleDeliveryTime, null, new RegisteredDelivery(SMSCDeliveryReceipt.SUCCESS_FAILURE),
                    replaceIfPresentFlag, new GeneralDataCoding(), smDefaultMsgId, message.getBytes());

            LOGGER.info(String.format("Message submitted, message ID is %s.", messageId));
        } catch (PDUException e) {
            LOGGER.error("Invalid PDU parameter", e);
            e.printStackTrace();
        } catch (ResponseTimeoutException e) {
            LOGGER.error("Response timeout", e);
        } catch (InvalidResponseException e) {
            LOGGER.error("Receive invalid respose", e);
        } catch (NegativeResponseException e) {
            LOGGER.error("Receive negative response", e);
        } catch (IOException e) {
            LOGGER.error("IO error occured", e);
        }

    }

    public void close() {
        smppSession.unbindAndClose();
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