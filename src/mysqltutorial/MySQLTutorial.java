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
    static Connection con;  //connection to database
    static int playersNum = -1;
    static int playersReady = 0;
    
    
    final static String DEFAULTGAME = "\"TheGame\"";
    /**
     * @param args the command line arguments
     */
    static final int PLAYERSPERGAME = 2;
    
    public static synchronized int incrementPlayersNum() {
        playersNum++;
        return playersNum;
    }
    public static synchronized int incrementPlayersReady() {
        playersReady++;
        return playersReady;
    }
    
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
        
        //CREATE DEFAULT GAME
        try {
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/plantgamedb", "root", "");
            Statement stmt = (Statement) con.createStatement();
            stmt.executeUpdate("insert into GameInstance (gameName) \n values (" + DEFAULTGAME + ");");
        } catch (SQLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        Integer[] gamePlayerIDs = new Integer[PLAYERSPERGAME];
        DataInputStream[] playerInStreams = new DataInputStream[PLAYERSPERGAME];
        DataOutputStream[] playerOutStreams = new DataOutputStream[PLAYERSPERGAME];
        
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(8000);


            for (int i = 0; i < PLAYERSPERGAME; i++) {
                new Thread(() -> {  //finds connections, creates playerIDs in gamePlayerIDs

                    try {
                         Socket socket = serverSocket.accept();

                        DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());
                        DataOutputStream outputToClient = new DataOutputStream(
                        socket.getOutputStream());

                        final int currPlayer = incrementPlayersNum();

                        if (currPlayer < PLAYERSPERGAME) {
                            playerInStreams[currPlayer] = inputFromClient;
                            playerOutStreams[currPlayer] = outputToClient;

                            // Receive radius from the client
                            String playerName = inputFromClient.readUTF();
                            String plantName = inputFromClient.readUTF();
                            int plantType = inputFromClient.readInt();
                            System.out.println("playername: " + playerName + ", plantName: " + plantName + ", plantType " + plantType);
                            //int gameNum = inputFromClient.readInt();
                            int plantId; //will be read from database after insertion of values

                            //outputToClient.writeInt(**numerical error code to client switch**); <<write error check
                            try {
                                //add player ID to gamePlayerIDs[]: creates player in database if not found
                                findAndLoadPlayerID(playerName, gamePlayerIDs, currPlayer);
                            } catch (SQLException ex) {
                                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            // now read information and LOAD IN DESCENDING ORDER:

                            //ADD Player + Gamenum to MasterList
                            try {
                                Statement stmt = (Statement) con.createStatement();
                                stmt.executeUpdate("insert into MasterList \n (fk_game_Mas, fk_player_Mas) \n values ( " + 1
                                + ", " + gamePlayerIDs[currPlayer] + " );");
                            } catch (SQLException ex) {
                                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                            }


                            try {
                            // LOAD PLANTLIST //give plant temp unique name
                                Statement stmt = (Statement) con.createStatement();
                                stmt.executeUpdate("insert into PlantList (plantName, fk_plantType_PlLi) \n values ('" + plantName
                                + "', " + plantType + ");");

                                Statement stmt2 = (Statement) con.createStatement();
                                ResultSet plantIdrst = stmt2.executeQuery("select id_Plant from PlantList where PlantList.plantName = \"" + plantName + "\";");
                                plantIdrst.next();
                                plantId = plantIdrst.getInt(1);

                            //LOAD PLAYERPLANTS
                                stmt.executeUpdate("insert into PlayerPlants (fk_player_Plpl, fk_plant_Plpl) \n values (" + gamePlayerIDs[currPlayer]
                                + ", " + plantId + ");");

                            //  LOAD PLANTRESACTIVE
                                for (int j = 1; j < 4; j++) {   //start with 3 of each resource
                                    stmt.executeUpdate("insert into PlantResActive (fk_plant_PlRA, fk_resource_PlRA, resQuantity) \n values (" + plantId
                                    + ", " + j + ", " + 3 + ");"); // CHEATING!!! our resources are numbered 1->3 in the database lol
                                }
                            } catch (SQLException ex) {
                                Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            incrementPlayersReady();
                        } //else send message back to client to join a different game
                    }
                    catch(IOException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
            
            new Thread(() -> {
                while (playersNum < PLAYERSPERGAME -1) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MySQLTutorial.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.out.println("waiting...");
                    System.out.println(playersNum);
                }
                if (true) {
                    System.out.println("okay gameplay go!");
                    while (playersReady < PLAYERSPERGAME-1) {   // leave loop after enough players ready
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MySQLTutorial.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    //instantiate game variables!
                     for (int i = 0; i < 4; i++) {
                         for (int j = 0; j < PLAYERSPERGAME; j++) {
                            DataInputStream in = playerInStreams[j];
                            int id = gamePlayerIDs[j];
                             try {
                                int resourceID = in.readInt();
                                System.out.println(resourceID);

                                //assume increment 1 to id in plantResActive

                             } catch (IOException e) {
                             }


                         }

                     }
                }
            }).start();
        } catch (IOException ex) {
            Logger.getLogger(MySQLTutorial.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        
        
    }
    
    private static Integer[] findAndLoadPlayerID(String playerName, Integer[] gamePlayerIDs, int currentPlayer) throws SQLException {
        boolean playerFound = false;
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery("select * from Player");
        System.out.println("finding and loading playerID!");
        while (myRs.next()) {
            if (myRs.getString(1).equals(playerName.trim())) {
                playerFound = true;
            }
        }
        if (!playerFound) {
            stmt.executeUpdate("insert into Player(playerName) \n values (\"" + playerName + "\");");
        }
        ResultSet playerID = stmt.executeQuery("select id_player from Player\n where Player.playerName = \"" + playerName + "\";");
        playerID.next();
        gamePlayerIDs[currentPlayer] = playerID.getInt(1);

        return gamePlayerIDs;
    }
        
}
    


