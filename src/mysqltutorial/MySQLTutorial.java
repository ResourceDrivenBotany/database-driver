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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
        Integer[] gamePlantIDs = new Integer[PLAYERSPERGAME];
        Integer[] gamePlantTypes = new Integer[PLAYERSPERGAME];
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
                            gamePlantTypes[currPlayer] = plantType;
                            
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
                                gamePlantIDs[currPlayer] = plantId;

                            //LOAD PLAYERPLANTS
                                stmt.executeUpdate("insert into PlayerPlants (fk_player_Plpl, fk_plant_Plpl) \n values (" + gamePlayerIDs[currPlayer]
                                + ", " + plantId + ");");

                            //  LOAD PLANTRESACTIVE
                                for (int j = 1; j < 4; j++) {   //start with 3 of each resource
                                    stmt.executeUpdate("insert into PlantResActive (fk_plant_PlRA, fk_resource_PlRA, resQuantity) \n values (" + plantId
                                    + ", " + j + ", " + 5 + ");"); // CHEATING!!! our resources are numbered 1->3 in the database lol
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
                    System.out.println("playersNum " + playersNum + new Date());
                }
                if (true) {
                    System.out.println("Enough players for a game!");
                    while (playersReady < PLAYERSPERGAME) {   // leave loop after enough players ready
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MySQLTutorial.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        System.out.println("playersReady " + playersReady + new Date());
                    }
                    //instantiate game variables!
                     for (int i = 0; i < 4; i++) {  //rounds
                         
                        HashMap<Integer, Integer> hmapAttackData = new HashMap<Integer, Integer>();

                        ArrayList<Integer> defendingPlants = new ArrayList<Integer>();
                        ArrayList<Integer> growingPlants = new ArrayList<Integer>();
                        
                        for (int j = 0; j < PLAYERSPERGAME; j++) { //players in game
                            DataInputStream in = playerInStreams[j];
                            int playerID = gamePlayerIDs[j];
                            int plantID = gamePlantIDs[j];
                            int plantTypeID = gamePlantTypes[j];
                            double modVal;
                            int resActiveID;
                            int resQuantity;
                            
                            System.out.println("startofplayerloop");
                            
                            
                            ///vvv increment resource
                            try {   //read from Client and perform gameplay operations
                               playerOutStreams[j].writeUTF("please enter resource [1. water, 2. soil, 3. scent]\n" 
                                       + "and integer quantity to increment 1-10**unchecked**");
                               int resourceID = in.readInt();
                               int resourceAmount = in.readInt();
                               System.out.println("resourceID: " + resourceID + ", resourceAmount: " + resourceAmount);
                               
                               Statement stmt;
                               
                               
                                try {   //get modval for chosen resource
                                    stmt = (Statement) con.createStatement();
                                    ResultSet ptrmRs = stmt.executeQuery("select modVal from PlantTypeResMod where PlantTypeResMod.fk_plantType_PTRM = " + plantTypeID 
                                        + " and PlantTypeResMod.fk_resource_PTRM = " + resourceID + ";");
                                    ptrmRs.next();
                                    modVal = ptrmRs.getDouble(1);
                                    
                                    //get resource quantity from PlantResActive
                                    ResultSet resQRs = stmt.executeQuery("select id_plantResActive, resQuantity from PlantResActive where PlantResActive.fk_plant_PlRa = " + plantID 
                                        + " and PlantResActive.fk_resource_PlRA = " + resourceID + ";");
                                    resQRs.next();
                                    resActiveID = resQRs.getInt(1);
                                    resQuantity = resQRs.getInt(2);
                                    playerOutStreams[j].writeUTF("previous quantity of resource " + resourceID + ": " + resQuantity);
                      
                                    System.out.println("resource: " + resourceID + ", amount to add: " + resourceAmount + ", modval: " + modVal
                                        + ", current resource quantity: " + resQuantity + ", modVal*amount + currentResourceQuantity = "
                                        + resourceAmount*modVal + resQuantity);
                                    stmt.executeUpdate("Update PlantResActive Set ResQuantity = " + (resourceAmount*modVal + resQuantity)  + " where id_plantResActive = " + resActiveID + ";");
                                    
                                    playerOutStreams[j].writeUTF("new quantity of resource " + resourceID + ": " + (resourceAmount*modVal + resQuantity));  
                                    
                                } catch (SQLException ex) {
                                    Logger.getLogger(MySQLTutorial.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                String playersList = "";
                                for (int k = 0; k < PLAYERSPERGAME; k++) {
                                    playersList += "\nPlayer ID: " + gamePlayerIDs[k] + "PlantID: " + gamePlantIDs[k];
                                }
                                playerOutStreams[j].writeUTF(playersList);
                                
                                playerOutStreams[j].writeUTF("enter attack (1) + plantToAttack + resource#, defend (2) or grow (3)");
                            
                                switch (in.readInt()) {
                                    case 1:
                                        System.out.println("attacking");
                                        int plantToAttack = in.readInt();    //throw exceptions for incorrect ID or ID same as Self
                                        int resourceToAttack = in.readInt();
                                       hmapAttackData.put(plantToAttack, resourceToAttack);
                                        break;
                                    case 2://defend
                                        System.out.println("defending");
                                        defendingPlants.add(plantID);
                                        break;
                                    case 3://grow
                                        System.out.println("growing");
                                        growingPlants.add(plantID);
                                        break;
                                }
                            } catch (IOException e) {
                            }  
                            System.out.println("end of playerloop");
                        }

                        try {
                            Statement stmt = con.createStatement();
                            
                            //ATTACKvv
                            Set attackSet = hmapAttackData.entrySet();
                            Iterator attackIterator = attackSet.iterator();

                            while(attackIterator.hasNext()) {
                               Map.Entry mentry = (Map.Entry)attackIterator.next();
                               int plantToAttack = (int)mentry.getKey();
                               int resourceToAttack;
                               if (! defendingPlants.contains(plantToAttack)) {
                                   resourceToAttack = (int) mentry.getValue();
                                   stmt.executeUpdate("Update PlantResActive Set resQuantity = resQuantity - " + 4 + " where fk_plant_plRA = " + plantToAttack 
                                           + " and fk_resource_plRA = " + resourceToAttack + ";");
                                   //SQL decrement data from player's respourceToAttack by constant amount
                               }
                            }
                            //GROW
                            for (Integer gPlantID: growingPlants) {
                                if (! hmapAttackData.containsKey(gPlantID)) {
                                        //SQL apply growth to each fk_plant_PlRA in plantResActive --> resQuantity = resQuantity + GROWCONSTANT*ln(score)
                                    stmt.executeUpdate("Update PlantResActive Set resQuantity = resQuantity + " + 1 + " where fk_plant_plRA = " + gPlantID 
                                           + ";");
                                }
                            }
                        } catch (SQLException e) {
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

        System.out.println("player ID:" + gamePlayerIDs[currentPlayer]);
        return gamePlayerIDs;
    }
        
}
    


