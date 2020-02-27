package com.fixme.tools;

import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

public class MessageWrite extends Thread {
    private SocketChannel client;
    private ByteBuffer buf;

    public MessageWrite (SocketChannel client) {
        this.client = client;
    }

    public void routerHandleAcceptMessage(int serverID, int id){
        String response = Trade.getAdminCreatedIDFixMessage(serverID, id, id);
        this.write(response);
    }

    public void brokerInitMessage(int orderID, int myID, int marketID){
        String response = Trade.getBuyOrSellFixMessage(orderID, myID, marketID);
        this.write(response);
    }

    public void write(String response){
        try {
            this.buf = ByteBuffer.allocate(response.length());
            this.buf.put(response.getBytes());
            this.buf.flip();
            this.client.write(this.buf);
            this.buf.clear();
        } catch (IOException e) {
            System.out.println("ERROR: Can't send message.");
        }
    }

    @Override
    public void run() {

    }
}