/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package space.poulter.em.poker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Em Poulter <em@poulter.space>
 */
public class UtilTest {
    
    public UtilTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of expandCommand method, of class Util.
     */
    @Test
    public void testExpandCommand() {
        System.out.println("expandCommand");
        String s = "test:command:1";
        List<String> expResult = Arrays.asList("test", "command", "1");
        List<String> result = Util.expandCommand(s);
        System.out.println(expResult);
        System.out.println(result);
        assertEquals(expResult, result);
        
        s = "test";
        expResult = Arrays.asList("test");
        result = Util.expandCommand(s);
        System.out.println(expResult);
        System.out.println(result);
        assertEquals(expResult, result);
        
        s = ":";
        expResult = Arrays.asList("", "");
        result = Util.expandCommand(s);
        System.out.println(expResult);
        System.out.println(result);
        assertEquals(expResult, result);
        
        s = "::";
        expResult = Arrays.asList("", "", "");
        result = Util.expandCommand(s);
        System.out.println(expResult);
        System.out.println(result);
        assertEquals(expResult, result);
        
        s = "test:";
        expResult = Arrays.asList("test", "");
        result = Util.expandCommand(s);
        System.out.println(expResult);
        System.out.println(result);
        assertEquals(expResult, result);
        
        s = ":test";
        expResult = Arrays.asList("", "test");
        result = Util.expandCommand(s);
        System.out.println(expResult);
        System.out.println(result);
        assertEquals(expResult, result);
    }
    
}
