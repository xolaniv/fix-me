package com.fixme.tools;

import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.io.IOException;

public class MessageRead extends Thread {
    private SocketChannel client;
    private ByteBuffer buffer;

    public MessageRead (SocketChannel client) {
        this.client = client;
    }

    public void read(ByteBuffer buf){
        this.buffer = buf;
    }

    @Override
    public void run() {
        try {
            this.client.read(this.buffer);
            this.buffer.flip();
        } catch (IOException e) {
            System.out.println("ERROR: Can't read message.");
        }
    }
}