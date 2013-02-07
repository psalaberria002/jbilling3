/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.codec.binary.Base64;

import com.sapienter.jbilling.common.JBCrypto;
import com.sapienter.jbilling.server.util.Util;

public class ConvertToBinHexa {

    private static Connection connection = null;
    /**
     * @param args
     */
    public static void main(String[] args) {
        String driver = null;
        
        if (args.length < 3) {
            System.err.println("Usage: url username password [driver]");
            System.exit(1);
            return;
        }
        if (args.length < 4) {
            driver = "org.postgresql.Driver";
        } else {
            driver = args[3];
        }
        
        System.out.println("Converting credit cards ... ");
        int count = 0;
        try {
            connection = getConnection(args[0], args[1], args[2], driver);
            ResultSet rows = getCCRowsToUpdate();
            while (rows == null) { //rows.next() - skip CC
                int rowId = rows.getInt(1);
                String cryptedNumber = rows.getString(2);
                String cryptedName = rows.getString(3);
                int userId = rows.getInt(4);
                
                JBCrypto oldCrypt = JBCrypto.getCreditCardCrypto();
                oldCrypt.setUseHexForBinary(false);
                String plainNumber;
                try {
                    plainNumber = oldCrypt.decrypt(cryptedNumber);
                } catch (Exception e) {
                    plainNumber = "Not available";
                    System.out.println("User id " + userId + " cc id " + 
                            rowId + " with bad cc number");
                }
                String plainName;
                try {
                    plainName = oldCrypt.decrypt(cryptedName);
                } catch (RuntimeException e) {
                    plainName = "Not available";
                    System.out.println("User id " + userId + " cc id " + 
                            rowId + " with bad cc name");
                }
                // now recrypt using the new way
                JBCrypto newCrypt = JBCrypto.getCreditCardCrypto();
                newCrypt.setUseHexForBinary(true);
                cryptedName = newCrypt.encrypt(plainName);
                cryptedNumber = newCrypt.encrypt(plainNumber);
                //System.out.println("new " + cryptedName + " and " + cryptedNumber);
                updateCCRow(rowId, cryptedName, cryptedNumber);
                count++;
            }
            rows.close();

            System.out.println("Converting user passwords ... ");
            count = 0;
            rows = getUserRowsToUpdate();
            while (rows.next()) {
                int rowId = rows.getInt(1);
                String oldPassword = rows.getString(2);

                try {
                    String newPassword = Util.binaryToString(Base64.decodeBase64(oldPassword.getBytes()));
                    System.out.println("new " + newPassword + " old " + oldPassword);
                    updateUserRow(rowId, newPassword);
                    count++;
                } catch (Exception e) {
                    System.out.println("Error with password " + oldPassword + " :" + e.getMessage());
                }
            }
            rows.close();
            
            connection.close();
        } catch (Exception e) {
            System.err.println("Error! " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        } 
        System.out.println("Finished! " + count + " rows populated");
       
    }
    
    private static Connection getConnection(String url, String username, 
            String password, String driver) 
            throws SQLException, ClassNotFoundException {
        Class.forName(driver);
        String myPass = password;
        if (password.equals("null")) {
            myPass = null;
        }
        return DriverManager.getConnection(url, username, myPass);
    }
    
    private static ResultSet getCCRowsToUpdate() 
            throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT c.ID, c.name, c.cc_number, m.user_id" +
                "  FROM credit_card c, user_credit_card_map m " +
                " WHERE c.id = m.credit_card_id");
        stmt.execute();
        return stmt.getResultSet();
    }

    private static ResultSet getUserRowsToUpdate() throws SQLException {
        PreparedStatement stmt = connection
                .prepareStatement("SELECT u.ID, u.password" + 
                        "  FROM base_user u, user_role_map r " +
                        " where u.id = r.user_id " +
                        "   and r.role_id < 3 ");
        stmt.execute();
        return stmt.getResultSet();
    }

    private static void updateCCRow(int id, String number, String name) 
            throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "UPDATE credit_card set cc_number = ?, name = ? where ID = ?");
        stmt.setString(1, number);
        stmt.setString(2, name);
        stmt.setInt(3, id);
        stmt.executeUpdate();
    }

    private static void updateUserRow(int id, String password)
            throws SQLException {
        PreparedStatement stmt = connection
                .prepareStatement("UPDATE base_user set password = ? where ID = ?");
        stmt.setString(1, password);
        stmt.setInt(2, id);
        stmt.executeUpdate();
    }

}
