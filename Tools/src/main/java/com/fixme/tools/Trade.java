package com.fixme.tools;

import java.util.Random;
import java.util.HashMap;

public class Trade {
    private static String[] splitMessage(String message){
        String[] results = message.split("\\|");
        return results;
    }

    public static String checksum (String message){
        int cks = 0;

        for (int index = 0; index < message.length() ; index++){
            cks += (int) message.charAt(index);
        }

        cks = cks % 256;

        return (String.format("%3d", cks).replace(" ", "0"));
    }

    private static String item(){
        String[] items = new String[]{"GLD", "PLT", "CRO", "COP"};

        return items[new Random().nextInt(items.length)];
    }

    private static String buyOrSellInnerContent(int orderID, int senderID, int receiverID) {
        int buyOrSellChoice = new Random().nextInt(2 + 1);
        String content = "|35=D|49=" + senderID + "|56=" + receiverID + "|37=" + orderID +  "|54=" + buyOrSellChoice +  "|55=" + Trade.item() + "|";

        return content;
    }

    public static String fixNotation(String message){
        String results = String.format("8=FIX.4.2|9=%s%s10=%s|" , message.length(), message, Trade.checksum(message));

        return results;
    }

    public static HashMap<Integer, String> getFixMessageHashMap(String fixMessage){
        HashMap<Integer, String> results = new HashMap<Integer, String>();
        String[] messageArray = Trade.splitMessage(fixMessage);

        int tag;
        String value;
        for (String messagePart : messageArray){
            String[] tagValue = messagePart.split("=");
            tag = Integer.parseInt(tagValue[0].trim());
            value = tagValue[1];

            results.put(tag, value);
        }

        return results;
    }

    public static String getAdminCreatedIDFixMessage(int senderID, int receiverID, int textID) {
        String content = "|35=A|49=" + senderID + "|56=" + receiverID + "|58=" + textID + "|37=" + textID + "|";

        return Trade.fixNotation(content);
    }

    public static String getBuyOrSellFixMessage(int orderID, int senderID, int receiverID){
        String innerContent = Trade.buyOrSellInnerContent(orderID, senderID, receiverID);
        return Trade.fixNotation(innerContent);
    }

    public static String getFeedbackFixMessage(int senderID, int receiverID, HashMap<Integer, String> requestFixMessageHashMap){
        String content = null;

        if (Trade.item().equals(requestFixMessageHashMap.get(55))){
            content = "|35=8|39=2";
        } else {
            content = "|35=8|39=8";
        }

        content += "|49=" + senderID + "|56=" + receiverID + "|37=" + requestFixMessageHashMap.get(37) + "|";

        return Trade.fixNotation(content);
    }

    public static String getAdminCreatedIDFeedbackFixMessage(int id, int senderID, int receiverID){
        String content = "|35=A|39=2|49=" + senderID + "|56=" + receiverID + "|37=" + id + "|";

        return Trade.fixNotation(content);
    }
}