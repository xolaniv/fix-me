package com.fixme.tools;

public class Log {
    public static void clientConnectedLog(String clientType, int ID){
        System.out.printf("CONNECTED: ID (%d) CLIENT (%s)\n" , ID, clientType);
    }

    public static String messageReceiverErrorLog(int orderID, int senderID, int receiverID, int index){
        String[] errors = new String[]{"ERROR: Market mentioned not found", "ERROR: Broker mentioned not found"};
        String content = "|35=3|39=8|49=" + senderID + "|56=" + receiverID + "|37=" + orderID +  "|58=" + errors[index] + "|";

        return Trade.fixNotation(content);
    }

    public static void log(String message) {
        System.out.println(message);
    }
}
