/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package space.poulter.poker;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Em Poulter
 */
public class Util {
    
    /**
     * Expands a command separated by colons and returns them as a list of strings
     * @param s The command to unpack
     * @return A List of the parts of the command
     */
    public static List<String> expandCommand(String s) {
        String newString;
        List<String> strings = new ArrayList<>();
        while(s.contains(":")) {
            newString = s.substring(0, s.indexOf(":"));
            s = s.substring(newString.length()+1);
            strings.add(newString);
        }
        strings.add(s);
        return strings;
    }
}
