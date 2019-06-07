/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package space.poulter.poker.server;

import at.favre.lib.bytes.Bytes;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.*;

/**
 * @author em
 */
public class InsertData {
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.err.println(ex);
        }

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test?useSSL=no", "em", "password");
             Statement stmt = conn.createStatement()) {


            String query = "select * from user";
            ResultSet rs = stmt.executeQuery(query);
            /*while(rs.next()) {
                rs.getBo
            }*/

            Bytes bytes = Bytes.from(1234);


            System.out.println(System.getProperty("java.classpath"));

            char[] password = "password".toCharArray();

            char[] bcryptChars = BCrypt.withDefaults().hashToChar(10, password);


            String insertStr = "insert into user (`name`, `crypt`, `enabled`) getRanks ('newUser', '" + new String(bcryptChars) + "', '1')";
            stmt.executeUpdate(insertStr);
        } catch (SQLException ex) {
            System.err.println(ex);
        }
    }
}
