package com.fixme.broker;

import com.fixme.tools.*;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Scanner;

public class Broker {
    public final String GREETING = "BROKER\nConnected to server...\n\n";
    private Selector selector = null;
    private final static String HOST = "localhost";
    private final static int PORT = 5000;
    private SocketAddress brokerSocketAddress = null;
    private SocketChannel brokerChannel = null;
    private Socket brokerSocket = null;
    private int myID = 0;
    private int marketID = 0;
    private boolean gotID = false;
    private static int orderID = 201800000;

    public Broker() throws Exception {
        this.setup();
    }

    private void setup() throws Exception {
        this.selector = Selector.open();
        this.brokerSocketAddress = new InetSocketAddress(HOST, PORT);
    }

    private void connectServer() throws Exception {
        this.brokerChannel = SocketChannel.open(this.brokerSocketAddress);
        this.brokerSocket = this.brokerChannel.socket();
        this.brokerChannel.configureBlocking(false);

        int selections = SelectionKey.OP_CONNECT | SelectionKey.OP_READ;
        this.brokerChannel.register(this.selector, selections);

        Log.log(GREETING);

        while (true){
            // Waiting for event
            this.selector.select();
            Iterator<SelectionKey> iterator = this.selector.selectedKeys().iterator();
            while (iterator.hasNext()){
                SelectionKey key = iterator.next();
                iterator.remove();

                if (key.isReadable())
                    this.receiveMessageFromServer();
            }
        }
    }

    private void initMessage() throws Exception {
        Scanner scanner = new Scanner(System.in);
        int id;

        while (true){
            try {
                System.out.println("Enter receiver ID: ");
                id = scanner.nextInt();
                if (String.valueOf(id).length() != 6){
                    throw new Exception();
                }
                break;
            } catch (Exception e) {
                System.out.println("ERROR: ID should be 6 digits");
            }
        }
        scanner.close();

        this.marketID = id;

        MessageWrite messageWriteThread = new MessageWrite(this.brokerChannel);
        messageWriteThread.brokerInitMessage(++Broker.orderID, this.myID, this.marketID);
        messageWriteThread.start();
    }

    private void receiveMessageFromServer() throws Exception {
        String response = null;
        ByteBuffer buf = ByteBuffer.allocate(Tools.byteBufferSize);
        this.read(this.brokerChannel, buf);

        String request = new String(Tools.UTF8.decode(buf).array());
        if (request.length() > 0){
            Log.log(request);

            HashMap<Integer, String> requestFixMessageHashMap = Trade.getFixMessageHashMap(request);

            if (this.gotID == false){
                if (requestFixMessageHashMap.get(35).equals("A")){
                    this.myID = Integer.parseInt(requestFixMessageHashMap.get(58).trim());
                    String logMessage = String.format("Server created ID(%s)", this.myID);
                    Log.log(logMessage);
                    this.gotID = true;

                    response = Trade.getAdminCreatedIDFeedbackFixMessage(this.myID, this.myID, 100000);
                    this.sendMessageToServer(response);
                    this.initMessage();
                }
            } else {
                String logMessage = null;
                String orderID = requestFixMessageHashMap.get(37);

                if (requestFixMessageHashMap.get(35).equals("3")){
                    logMessage = String.format("Server error for order (%s)", orderID);
                } else if (requestFixMessageHashMap.get(35).equals("8")){
                    if (requestFixMessageHashMap.get(39).equals("2")) {
                        logMessage = String.format("Complete order (%s)", orderID);
                    } else {
                        logMessage = String.format("Rejected order (%s)", orderID);
                    }
                }
                Log.log(logMessage);
            }
        }
    }

    private void sendMessageToServer(String response) throws Exception {
        this.write(this.brokerChannel, response);
    }

    private void read(SocketChannel channel, ByteBuffer buf) throws Exception {
        MessageRead messageReadThread = new MessageRead(channel);
        messageReadThread.read(buf);
        messageReadThread.start();
        messageReadThread.join();
    }

    private void write(SocketChannel channel, String response) throws Exception {
        MessageWrite messageWriteThread = new MessageWrite(channel);
        messageWriteThread.write(response);
        messageWriteThread.start();
    }

    public void run() throws Exception {
        this.connectServer();
    }

    public static void main(String[] args) throws Exception {
        try {
            Broker broker = new Broker();
            broker.run();
        } catch (Exception e){
            Log.log("BROKER: Connection lost.");
        }

    }
}