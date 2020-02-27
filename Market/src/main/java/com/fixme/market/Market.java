package com.fixme.market;

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

public class Market {
    public final String GREETING = "MARKET\nConnected to server...\n\n";
    private Selector selector = null;
    private final static String HOST = "localhost";
    private final static int PORT = 5001;
    private SocketAddress marketSocketAddress = null;
    private SocketChannel marketChannel = null;
    private Socket marketSocket = null;
    private boolean gotID = false;
    private int myID = 0;

    public Market() throws Exception {
        this.setup();
    }

    private void setup() throws Exception {
        this.selector = Selector.open();
        this.marketSocketAddress = new InetSocketAddress(HOST, PORT);
    }

    private void connectServer() throws Exception {
        this.marketChannel = SocketChannel.open(this.marketSocketAddress);
        this.marketSocket = this.marketChannel.socket();
        this.marketChannel.configureBlocking(false);

        int selections = SelectionKey.OP_CONNECT | SelectionKey.OP_READ;
        this.marketChannel.register(this.selector, selections);

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

    private void receiveMessageFromServer() throws Exception {
        String response = null;
        ByteBuffer buf = ByteBuffer.allocate(Tools.byteBufferSize);
        this.read(this.marketChannel, buf);

        String request = new String(Tools.UTF8.decode(buf).array());
        if (request.length() > 0){
            Log.log(request);
            HashMap<Integer, String> requestFixMessageHashMap = Trade.getFixMessageHashMap(request);

            if (this.gotID == false){
                if (requestFixMessageHashMap.get(35).equals("A")){
                    this.myID = Integer.parseInt(requestFixMessageHashMap.get(58).trim());
                    String logMessage = String.format("Server created ID (%s)", this.myID);
                    Log.log(logMessage);
                    this.gotID = true;

                    response = Trade.getAdminCreatedIDFeedbackFixMessage(this.myID, this.myID, 100000);
                    this.sendMessageToServer(response);
                }
            } else {
                if (requestFixMessageHashMap.get(35).equals("D")){
                    String logMessage = String.format("New order (%s) for (%s)", requestFixMessageHashMap.get(37), requestFixMessageHashMap.get(55));
                    Log.log(logMessage);

                    int receiverID = Integer.parseInt(requestFixMessageHashMap.get(49).trim());
                    int senderID = Integer.parseInt(requestFixMessageHashMap.get(56).trim());
                    response = Trade.getFeedbackFixMessage(senderID, receiverID, requestFixMessageHashMap);
                    this.sendMessageToServer(response);
                }
            }
        }
    }

    private void sendMessageToServer(String response) throws Exception {
        this.write(this.marketChannel, response);
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
            Market market = new Market();
            market.run();
        } catch (Exception e){
            Log.log("MARKET: Connection lost.");
        }
    }
}