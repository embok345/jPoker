/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package space.poulter.poker;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Em Poulter
 */
public class Util {

    public static final String VERSION_STR = "JPoker_0.0.1";
    public static final Level LOG_LEVEL = Level.FINE;
    static final Logger rootLogger = Logger.getLogger("");
    public static int DEFAULT_PORT = 1111;

    static {
        rootLogger.setLevel(LOG_LEVEL);
        rootLogger.getHandlers()[0].setLevel(LOG_LEVEL);
        rootLogger.getHandlers()[0].setFilter(record -> {
            if (record.getLevel().intValue() >= Level.INFO.intValue())
                return true;
            try {
                return Class.forName(record.getSourceClassName()).getPackage().equals(Util.class.getPackage()) ||
                        Class.forName(record.getSourceClassName()).getPackage().equals(space.poulter.poker.server.Main.class.getPackage()) ||
                        Class.forName(record.getSourceClassName()).getPackage().equals(space.poulter.poker.client.PokerClient.class.getPackage());
            } catch (ClassNotFoundException ex) {
                return false;
            }
        });
    }

    public static int bytesToInt(byte[] bytes) {
        if (bytes.length != 4 || bytes[0] < 0) {
            return -1;
        }
        int ret = 0;
        for (byte b : bytes) {
            ret <<= 8;
            ret += b & 0xff;
        }
        return ret;
    }

    public static byte[] intToBytesPrim(int val) {
        byte[] ret = new byte[4];
        ret[0] = (byte) (val >> 24);
        ret[1] = (byte) (val >> 16);
        ret[2] = (byte) (val >> 8);
        ret[3] = (byte) val;
        return ret;
    }

    public static Byte[] intToBytes(int val) {
        Byte[] ret = new Byte[4];
        ret[0] = (byte) (val >> 24);
        ret[1] = (byte) (val >> 16);
        ret[2] = (byte) (val >> 8);
        ret[3] = (byte) val;
        return ret;
    }

    /**
     * Expands a command separated by colons and returns them as a list of strings
     *
     * @param s The command to unpack
     * @return A List of the parts of the command
     */
    public static List<String> expandCommand(String s) {
        String newString;
        List<String> strings = new ArrayList<>();
        while (s.contains(":")) {
            newString = s.substring(0, s.indexOf(":"));
            s = s.substring(newString.length() + 1);
            strings.add(newString);
        }
        strings.add(s);
        return strings;
    }

    public enum PokerAction {
        CALL, CHECK, FOLD, RAISE, NONE
    }
}
