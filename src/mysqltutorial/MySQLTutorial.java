/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mysqltutorial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

/**
 *
 * @author mishakanai
 */


public class MySQLTutorial {
    static Connection con;
    static int playersNum = 0;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ClassNotFoundException {
        

        
        try {
           // Class.forName("com.mysql.jdbc.Driver");
            
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/plantgamedb", "root", "");
            
            Statement stmt = (Statement) con.createStatement();
//            
//            stmt.executeUpdate("insert into PlantType (typeName)\n" +
//                    "values ('flower');");
//            
//            stmt.executeUpdate("insert into PlantResReference (resourceName, resVal)\n" +
//                    "values ('water', 1);");
//            
//            stmt.executeUpdate("insert into PlantResReference (resourceName, resVal)\n" +
//                    "values ('soil', 1);");
//            
//            stmt.executeUpdate("insert into PlantResReference (resourceName, resVal)\n" +
//                    "values ('scent', 1);");
//
//            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
//                    "values (1, 1, 1.0)");
//            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
//                    "values (1, 2, 1.0)");
//            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
//                    "values (1, 3, 1.0)");
//            
            
            ResultSet myRs = stmt.executeQuery("select * from PlantTypeResMod");
            
            
            System.out.println("PlantTypeResMod:");
            while (myRs.next()) {
                System.out.println("ID: " + myRs.getInt(1) + ", plantTypeID: " + myRs.getString(2) + ", resourceID: " + myRs.getInt(3) + ", modval: " + myRs.getDouble(4));
            }
        } catch (SQLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        new Thread(() -> {
            try {
                Integer[] gamePlayerIDs = new Integer[2];
                // Create a server socket
                ServerSocket serverSocket = new ServerSocket(8000);
                // Listen for a connection request
                 Socket socket = serverSocket.accept();

                 // Create data input and output streams
                DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());

                DataOutputStream outputToClient = new DataOutputStream(
                socket.getOutputStream());

                while (playersNum < 2) {
                    // Receive radius from the client
                    String playerName = inputFromClient.readUTF();
                    int plantType = inputFromClient.readInt();
                    //int gameNum = inputFromClient.readInt();

                    //outputToClient.writeInt(**numerical error code to client switch**); <<write error check
                    try {
                        boolean playerFound = false;
                        Statement stmt = (Statement) con.createStatement();
                        ResultSet myRs = stmt.executeQuery("select * from Player");
                        while (myRs.next()) {
                            if (myRs.getString(1).equals(playerName.trim())) {
                                playerFound = true;
                            }
                        }
                        if (!playerFound) {
                            stmt.executeUpdate("insert into Player(playerName) \n values (" + playerName + ");");
                        }
                        ResultSet playerID = stmt.executeQuery("select id_player from Player where Player.playerName = " + playerName + ");");
                        gamePlayerIDs[playersNum] = playerID.getInt(1);
                        playersNum++;
                        
                    } catch (SQLException ex) {
                        Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }
                // gameplay loop!
                //4 rounds
                for (int i = 0; i < 4; i++) {
                
                    
                    
                    
                    
                }
                
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }
        
}
    

