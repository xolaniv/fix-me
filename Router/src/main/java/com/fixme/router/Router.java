package com.fixme.router;

import com.fixme.tools.*;

import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class Router {
    //implements Runnable {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private final String GREETING = "ROUTER\nServer running...\n\n";
    private Selector selector = null;
    private final int BROKER_PORT = 5000;
    private SocketAddress brokerSocketAddress = null;
    private final int MARKET_PORT = 5001;
    private SocketAddress marketSocketAddress = null;
    private ServerSocketChannel server1 = null;
    private ServerSocketChannel server2 = null;
    private HashMap<Integer, SocketChannel> brokersTable = new HashMap<Integer, SocketChannel>();
    private HashMap<Integer, SocketChannel> marketsTable = new HashMap<Integer, SocketChannel>();
    private int serverID = 100000;

    public Router() throws Exception {
        this.brokerSocketAddress = new InetSocketAddress(BROKER_PORT);
        this.marketSocketAddress = new InetSocketAddress(MARKET_PORT);
        this.selector = Selector.open();

        this.server1 = this.server1.open();
        this.server1.configureBlocking(false);
        this.server1.socket().bind(this.brokerSocketAddress);
        this.server1.register(this.selector, SelectionKey.OP_ACCEPT);

        this.server2 = this.server2.open();
        this.server2.configureBlocking(false);
        this.server2.socket().bind(this.marketSocketAddress);
        this.server2.register(this.selector, SelectionKey.OP_ACCEPT);
    }

    private void handleAccept(SelectionKey key) throws Exception {
        Random random = new Random();
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        int id;

        int randomValue = 100001 + random.nextInt(899999);
        while ((this.brokersTable.get(randomValue) != null) || (this.marketsTable.get(randomValue) != null)){
            randomValue = 100001 + random.nextInt(899999);
        }
        id = randomValue;

        MessageWrite messageWriteThread = new MessageWrite(client);
        messageWriteThread.routerHandleAcceptMessage(this.serverID, id);
        messageWriteThread.start();

        if (client.socket().getLocalPort() == 5000){
            this.brokersTable.put(id, client);
            Log.clientConnectedLog("Broker", id);
        } else {
            this.marketsTable.put(id, client);
            Log.clientConnectedLog("Market", id);
        }

        client.configureBlocking(false);
        client.register(this.selector, SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey key) throws Exception {
        SocketChannel sender = null;
        ByteBuffer buf = null;
        String request = null;
        HashMap<Integer, String> requestFixMessageHashMap = null;

        // Get sender
        sender = (SocketChannel) key.channel();
        buf = ByteBuffer.allocate(Tools.byteBufferSize);
        this.read(sender, buf);
        request = new String(Router.UTF8.decode(buf).array());

        if (request.length() > 0){
            Log.log(request);
            requestFixMessageHashMap = Trade.getFixMessageHashMap(request);
            if (requestFixMessageHashMap.get(35).equals("A")){
                request = "";
            } else if (request.length() > 0){
                // Send to receiver
                this.handleWrite(sender, requestFixMessageHashMap, request);
            }
        }
    }

    private void handleWrite(SocketChannel sender, HashMap<Integer, String> requestFixMessageHashMap, String request) throws Exception {
        MessageWrite messageWriteThread = null;
        String response = null;
        SocketChannel receiver = null;

        int orderID = Integer.parseInt(requestFixMessageHashMap.get(37));
        int senderID = Integer.parseInt(requestFixMessageHashMap.get(49));
        int receiverID = Integer.parseInt(requestFixMessageHashMap.get(56));

        if (sender.socket().getLocalPort() == 5000){
            receiver = this.marketsTable.get(receiverID);
            if (receiver == null){
                response = Log.messageReceiverErrorLog(orderID, this.serverID, senderID, 0);
                Log.log(response);
                messageWriteThread = new MessageWrite(sender);
                messageWriteThread.write(response);
                messageWriteThread.start();
            } else {
                messageWriteThread = new MessageWrite(receiver);
                messageWriteThread.write(request);
                messageWriteThread.start();
            }
        } else {
            receiver = this.brokersTable.get(receiverID);
            if (receiver == null){
                response = Log.messageReceiverErrorLog(orderID, this.serverID, senderID, 1);
                Log.log(response);
                messageWriteThread = new MessageWrite(sender);
                messageWriteThread.write(response);
                messageWriteThread.start();
            } else {
                messageWriteThread = new MessageWrite(receiver);
                messageWriteThread.write(request);
                messageWriteThread.start();
            }
        }
    }

    private void read(SocketChannel channel, ByteBuffer buf) throws Exception {
        MessageRead messageReadThread = new MessageRead(channel);
        messageReadThread.read(buf);
        messageReadThread.start();
        messageReadThread.join();
    }

    public void run() throws Exception {
        try {
            System.out.println(GREETING);

            while (true){
                // Waiting for event
                this.selector.select();
                Iterator<SelectionKey> iterator = this.selector.selectedKeys().iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()){
                        // Accepting connection from client
                        this.handleAccept(key);
                    }
                    if (key.isReadable()){
                        // Receiving message from client
                        this.handleRead(key);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("SERVER: Stopped");
        }
    }

    public static void main(String[] args) throws Exception {
        Router router = new Router();
        //(new Thread(router)).start();
        router.run();
    }
}