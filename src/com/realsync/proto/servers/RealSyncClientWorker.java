/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.realsync.proto.servers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author int soumen
 */
enum EnumSocketClientType {
    en_ClientTypeNone, /**
     * Not a valid type of client
     */
    en_ClientTypeFileClient, /**
     * Will upload a file
     */
    en_ClientTypeEventClient
    /**
     * Need to keep the client alive to send event
     */
}

public abstract class RealSyncClientWorker implements Runnable {

    protected Socket clientSocket = null;
    protected String serverText = null;
    private boolean listenToClient = true;
    InputStream input;
    OutputStream output;
    private StringBuilder MessageBuilder = new StringBuilder();
    //private StringBuilder Message;
    private int BufferLength = 0;
    private StringBuilder API_KEY = null;
    private boolean HandShake = false;
    RealSyncTimer HandshakeTimer = null;
    RealSyncTimer ReplyWaitTimer = null;

    public abstract void statusClientMessage(String s);

    public abstract void clientType(EnumSocketClientType clientType);

    public abstract void processingStarted();

    public abstract void clientClosed();

    public abstract void handshake(boolean status);

    public String getServerText() {
        return this.serverText;
    }

    public RealSyncClientWorker(Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        this.serverText = serverText;
        HandshakeTimer = new RealSyncTimer(this,HANDSHAKE_TIMEOUT_mills, "Handshake timer.") {
            @Override
            public void timerDone() {
                if (HandShake == false) {
                    this.getWorker().statusClientMessage("Handshake timeout.Closing client...");
                    this.getWorker().close();
                }
            }
        };

        ReplyWaitTimer = new RealSyncTimer(this,REPLY_WAIT_TIMEOUT_mills, "Reply wait timer.") {
            @Override
            public void timerDone() {
                if (HandShake == false) {
                    this.getWorker().statusClientMessage("Reply wait timeout. Closing client...");
                    this.getWorker().close();
                }
            }
        };
    }

    public static final String DLIM = "\r\n\r\n";

    public static final int HANDSHAKE_TIMEOUT_mills = 10000; //10s
    public static final int REPLY_WAIT_TIMEOUT_mills = 20000; //20s

    /**
     * Default client socket client type none
     */
    protected EnumSocketClientType EN_CLIENT_TYPE = EnumSocketClientType.en_ClientTypeNone;

    /**
     * Current command command index
     */
    private int SEND_CMD_INDEX = -1;

    /**
     * Current command status
     */
    enum EnumCommandStstus {
        en_NO_CMD,
        en_WAITING_FOR_CMD_RESPONSE,
        en_RECEIVING_CMD_RESPONSE,
        en_PROCESS_CMD_RESPONSE
    };
    public static final int NO_CMD = 0; //[0 = No comand sent
    public static final int WAITING_FOR_CMD_RESPONSE = 1; //1 = command sent and waiting for renponse]
    public static final int RECEIVING_CMD_RESPONSE = 2;    //2 RECEIVING COMMAND RESPONSE
    public static final int PROCESS_CMD_RESPONSE = 3;    //3 PROCESSING COMMAND RESPONSE
    public static final int RESET_CMD = NO_CMD;    //0 SET TO NO COMMAND

    private int COMMAND_STATUS = NO_CMD; //[0 = No comand sent, 1 = command sent and waiting/receiving for renponse]

    /**
     * ** Available client type code to EnumSocketClientType map
     */
    public static final HashMap<Integer, EnumSocketClientType> CLIENT_TYPE_MAP = new HashMap<Integer, EnumSocketClientType>();

    /**
     * Avaliable client type codes
     */
    private static final int CLIENT_TYPE_CODE_FILE_CLIENT = 2;
    private static final int CLIENT_TYPE_CODE_EVENT_CLIENT = 3;

    static {
        CLIENT_TYPE_MAP.put(new Integer(CLIENT_TYPE_CODE_FILE_CLIENT), EnumSocketClientType.en_ClientTypeFileClient);
        CLIENT_TYPE_MAP.put(new Integer(CLIENT_TYPE_CODE_EVENT_CLIENT), EnumSocketClientType.en_ClientTypeEventClient);
    }

    /**
     * Various Command Index
     */
    public static final int CMD_TYPE_REQ_CLIENT_TYPE = 0; //Command index to get client type
    public static final int CMD_TYPE_REQ_CLIENT_API_KEY = 1; //Command index to get client api key
    public static final int CMD_TYPE_REQ_FILE_NAME = 2; //Command index to get file name
    public static final int CMD_TYPE_REQ_FILE_LEN = 3; //Command index to get file length
    public static final int CMD_TYPE_REQ_FILE_CHECKSUM = 4; //Command index to get file ckecksum
    public static final int CMD_TYPE_SEND_STS_OK = 0; //Command index to send client ack
    public static final int CMD_TYPE_SEND_STS_NOT_OK = 1; //Command index to get client api key

    /**
     * Those commands will be sent to a
     * EnumSocketClientType.en_ClientTypeFileClient for processing
     */
    public static final char CMDS[] = {
        'I', //Type of the client [ref. CLIENT_TYPE_MAP]
        'N', //Api key of the client [This will validate client; also be used to identify the client]        

        /**
         * File upload commands [if client type is
         * SocketClientType.ClientTypeFileClient, ref. CLIENT_TYPE_MAP] *
         */
        'T', //Request Name of the file
        'S', //Request Length of the file
        'O', //Request Checksum of the file        
    };

    public static final char ACK[] = {
        'U', //Acknowledge OK
        'M' //Acknowledge NOT OK
    };

    public void run() {
        try {
            this.processingStarted();
            input = clientSocket.getInputStream();
            output = clientSocket.getOutputStream();
            this.statusClientMessage("Waiting to handshek...");
            new Thread(this.HandshakeTimer).start();
            new Thread(this.ReplyWaitTimer).start();
            this.loop();

        } catch (IOException e) {
            //report exception somewhere.
            //e.printStackTrace();
            this.statusClientMessage("Exception occured when processing client.: " + e.getMessage());
        }
    }

    private void loop() {
        while (listenToClient) {

            /**
             * check command status
             */
            switch (COMMAND_STATUS) {
                case NO_CMD:
                    if (EN_CLIENT_TYPE == EnumSocketClientType.en_ClientTypeFileClient || SEND_CMD_INDEX < CMD_TYPE_REQ_CLIENT_API_KEY) {
                        /**
                         * Perform command if client type is file client OR
                         */
                        /**
                         * SEND_CMD_INDEX < CMD_TYPE_REQ_CLIENT_API_KEY
                         */
                        /**
                         * if no command is running
                         */
                        SEND_CMD_INDEX++;
                        /**
                         * Send command
                         */
                        send(CMDS[SEND_CMD_INDEX]);
                        /**
                         * mark command status as waiting
                         */
                        COMMAND_STATUS = WAITING_FOR_CMD_RESPONSE;
                    }
                    break;

                case WAITING_FOR_CMD_RESPONSE:
                case RECEIVING_CMD_RESPONSE:
                case PROCESS_CMD_RESPONSE:
                default:
            }

            try {
                int response = input.read();
                if (response != -1) {
                    this.ReplyWaitTimer.reset();
                }
                if (response != -1) {
                    // receiving response 

                    //|| COMMAND_STATUS == RECEIVING_CMD_RESPONSE
                    switch (COMMAND_STATUS) {
                        case WAITING_FOR_CMD_RESPONSE:
                            /**
                             * in a command waiting
                             */
                            /**
                             * mark command status as receiving
                             */
                            COMMAND_STATUS = RECEIVING_CMD_RESPONSE;
                            MessageBuilder.append((char) response);
                            BufferLength++;
                            break;
                        case RECEIVING_CMD_RESPONSE:
                            /**
                             * in a command receiving
                             */
                            MessageBuilder.append((char) response);
                            BufferLength++;
                            break;
                    }

                    /**
                     * get last 4 characters
                     */
                    //BufferLength = MessageBuilder.capacity();
                    if (BufferLength > 3 && MessageBuilder.substring(BufferLength - 4, BufferLength).endsWith(DLIM)) {
                        /*One command receiving ended*/
                        /**
                         * mark command status as processing
                         */
                        SEND_CMD_INDEX = PROCESS_CMD_RESPONSE;

                        //Detect command index to work with message 
                        switch (SEND_CMD_INDEX) {
                            case CMD_TYPE_REQ_CLIENT_TYPE:
                                _processClientType();
                                break;
                            case CMD_TYPE_REQ_CLIENT_API_KEY:
                                _processAPIKey();
                                break;
                            case CMD_TYPE_REQ_FILE_NAME:
                                break;
                            case CMD_TYPE_REQ_FILE_LEN:
                                break;
                            case CMD_TYPE_REQ_FILE_CHECKSUM:
                                break;
                        }
                        /**
                         * mark command status as receiving
                         */
                        COMMAND_STATUS = RESET_CMD;
                        /*wipe message buffer*/
                        MessageBuilder = new StringBuilder();
                        /*set buffer length to zero*/
                        BufferLength = 0;
                    }
                }//response receiving                
            } catch (IOException ex) {
                this.statusClientMessage(ex.getMessage());
                this.close();
            }

        }
    }

    public synchronized int getCommandStatus() {
        return COMMAND_STATUS;
    }

    private void _processAPIKey() {
        /**
         * Keep on checking upto CONNECTION_HANDSHEK_TIMEOUT_mills else Close
         * the connection with handshake failed status
         */
        API_KEY = MessageBuilder;
        boolean ValidKey = true;
        if (ValidKey) {
            /**
             * Auth validated Valid API key found
             */
            /**
             * Handshake is OK.
             */
            this.HandShake = true;
            /**
             * Notify server.
             */
            this.handshake(true);
            /**
             * Notify client.
             */
            this.send(ACK[CMD_TYPE_SEND_STS_OK]);
            /**
             * stop handshake timer
             */
            this.HandshakeTimer.done();
            /**
             * send message to server
             */
            this.statusClientMessage("Client accepted.");
            /**
             * update client listening status
             */
            this._updateClientListeningStatus();
        } else {
            /**
             * Invalid Auth Handshake is not OK.
             */
            this.HandShake = false;
            /**
             * Notify server.
             */
            this.handshake(false);
            /**
             * Notify client.
             */
            this.send(ACK[CMD_TYPE_SEND_STS_NOT_OK]);
            /**
             * Send message to server
             */
            this.statusClientMessage("Client handshake failed.Closing client...");
            /**
             * Close channel.
             */

            this.close();
        }
    }

    public synchronized StringBuilder getAPI_KEY() {
        return API_KEY;
    }

    public synchronized EnumSocketClientType getEN_CLIENT_TYPE() {
        return EN_CLIENT_TYPE;
    }

    /**
     * Stop listening to client as client will not send any response. and do not
     * close the connection
     */
    private void _updateClientListeningStatus() {
        if (this.EN_CLIENT_TYPE != EnumSocketClientType.en_ClientTypeFileClient) {
            /**
             * if client type not en_ClientTypeFileClient
             */
            this.listenToClient = false;

            /**
             * But do not close the connection
             */
        }
    }

    private void _processClientType() {
        /**
         * Keep on checking upto CONNECTION_HANDSHEK_TIMEOUT_mills else Close
         * the connection with handshake failed status Expecting a ASCII integer
         */
        try {
            /**
             * Parse client type.
             */
            int type = Integer.parseInt(MessageBuilder.toString().trim());

            /**
             * if client type available
             */
            if (CLIENT_TYPE_MAP.containsKey(type)) {
                /**
                 * Valid client Update client type
                 */
                EN_CLIENT_TYPE = CLIENT_TYPE_MAP.get(type);
                /**
                 * Notify server
                 */
                this.clientType(EN_CLIENT_TYPE);
                /* Notify client */
                this.send(ACK[CMD_TYPE_SEND_STS_OK]);
            } else {
                /**
                 * Not a valid client Notify server
                 */
                this.clientType(EN_CLIENT_TYPE);
                /**
                 * Notify server
                 */
                this.handshake(false);
                /**
                 * Notify client
                 */
                this.send(ACK[CMD_TYPE_SEND_STS_NOT_OK]);
                /**
                 * Close channel
                 */
                this.close();
            }
        } catch (Exception ex) {
            //Invalid message
            /**
             * Notify client
             */
            this.send(ACK[CMD_TYPE_SEND_STS_NOT_OK]);
            //Thread.sleep(10);
            this.close();
        }
    }

    /* ("HTTP/1.1 200 OK\n\nWorkerRunnable: " + this.serverText + " - " + time + "").getBytes() **/
    public synchronized void send(StringBuilder s) {
        this._sendBytes(s.toString().getBytes());
    }

    public synchronized void send(int s) {
        this._sendBytes(new byte[]{(byte) s});
    }

    public synchronized void send(char s) {
        this._sendBytes(new byte[]{(byte) s});
    }

    private void _sendBytes(byte s[]) {
        long time = System.currentTimeMillis();
        try {
            output.write(s);
        } catch (IOException e) {
            //report exception somewhere.
            this.statusClientMessage("Exception occured sending message to client: " + e.getMessage());
        }
    }

    public synchronized void close() {
        long time = System.currentTimeMillis();
        try {
            listenToClient = false;
            output.close();
            input.close();
            this.statusClientMessage("Client closed at : " + time);
            this.clientClosed();
        } catch (IOException ex) {
            this.statusClientMessage("Exception occured when closing client.: " + ex.getMessage());
        }
    }
}
