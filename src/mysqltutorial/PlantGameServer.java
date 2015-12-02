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


public class PlantGameServer {
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
    private static boolean pTRM_Initialized() throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs;
        myRs = stmt.executeQuery("select * from PlantTypeResMod");
        return myRs.next();

    }
    
    private static int getIntLoop(DataInputStream in, DataOutputStream out, String message) throws IOException{
        boolean loop = true;
        int fromClient = 0;
        while (loop) {
            try {
                fromClient = in.readInt();
                loop = false;
            } catch (IOException ex) {
                loop = true;
                out.writeUTF(message);
            }
        }
        return fromClient;
    }
    
    private static int getIntInRange(DataInputStream in, DataOutputStream out, int low, int high, String message) throws IOException {
        int fromClient = getIntLoop(in, out, "failed integer input, Try again.");
        while (fromClient < low || fromClient > high) {
            out.writeUTF(message);
            fromClient = getIntLoop(in, out, "failed integer input, Try again.");
        }
        return fromClient;
    }
    
    private static void pTRM_Initialize() throws SQLException{
        if (pTRM_Initialized())
            return;
        Statement stmt = (Statement) con.createStatement();            
            stmt.executeUpdate("insert into PlantType (typeName)\n" +
                    "values ('flower');");
            
            stmt.executeUpdate("insert into PlantResReference (resourceName, resVal)\n" +
                    "values ('water', 1);");
            
            stmt.executeUpdate("insert into PlantResReference (resourceName, resVal)\n" +
                    "values ('soil', 1);");
            
            stmt.executeUpdate("insert into PlantResReference (resourceName, resVal)\n" +
                    "values ('scent', 1);");

            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
                    "values (1, 1, 1.0)");
            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
                    "values (1, 2, 1.0)");
            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
                    "values (1, 3, 1.0)");
    }
    
    private static String getNameOfPlayer(int playerID) throws SQLException {
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery("select playerName from Player where id_player = " + playerID + ";");
        myRs.next();
        return myRs.getString(1);
    }
    
    public static void main(String[] args){
         try {
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/plantgamedb", "root", "");
         } catch (SQLException ex) {
            Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
           pTRM_Initialize();   //initialize plantTypeResMod table
        } catch (SQLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //CREATE DEFAULT GAME
        try {
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

                            // Read in from client
                            outputToClient.writeUTF("Enter player's name! \n(will be created in database if doesn't exist):");
                            String playerName = inputFromClient.readUTF();
                            outputToClient.writeUTF("Enter plant's name! \n(will be created in database if doesn't exist):");
                            String plantName = inputFromClient.readUTF();
                            outputToClient.writeUTF("Enter your plantType Number. Available plantTypes: ");
                            try {
                                int numberOfTypes = printPlantTypesAvailable(outputToClient);
                                int plantType = getIntInRange(inputFromClient, outputToClient, 1, numberOfTypes, "invalid Type. Enter an integer:");
                            
                                gamePlantTypes[currPlayer] = plantType;
                            
                                System.out.println("playername: " + playerName + ", plantName: " + plantName + ", plantType " + plantType);
                            
                                //add player ID to gamePlayerIDs[]: creates player in database if not found
                                findAndLoadPlayerID(playerName, gamePlayerIDs, currPlayer);
                                loadPlayerPlantData(currPlayer, gamePlayerIDs, gamePlantIDs, plantName, plantType);
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
                        Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    System.out.println("playersNum " + playersNum + new Date());
                }
                System.out.println("Enough players for a game!");
                while (playersReady < PLAYERSPERGAME) {   // leave loop after enough players ready
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
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
                        DataOutputStream out = playerOutStreams[j];
                        int playerID = gamePlayerIDs[j];
                        int plantID = gamePlantIDs[j];
                        int plantTypeID = gamePlantTypes[j];
                        double modVal;
                        int resActiveID;
                        int resQuantity;

                        ///vvv increment resource
                        try {   //read from Client and perform gameplay operations
                           out.writeUTF("please enter resource [1. water, 2. soil, 3. scent]\n" 
                                   + "and integer quantity to increment 1-10");
                           int resourceID = getIntInRange(in, out, 1, 3, "Resources are numbered 1-3. Try again:");
                           System.out.println("debugging1");
                           int resourceAmount = getIntInRange(in, out, 1, 10, "enter integer 1-10:");
                           System.out.println("resourceID: " + resourceID + ", resourceAmount: " + resourceAmount);

                           Statement stmt;

                            try {   //get modval for chosen resource
                                stmt = (Statement) con.createStatement();
                                ResultSet resQRs = stmt.executeQuery("select resQuantity from PlantResActive where PlantResActive.fk_plant_PlRa = " + plantID 
                                    + " and PlantResActive.fk_resource_PlRA = " + resourceID + ";");
                                resQRs.next();
                                double  rQ= resQRs.getDouble(1);
                                playerOutStreams[j].writeUTF("previous quantity of resource " + resourceID + ": " + String.format("%.1f", rQ));  
                                double newResQuantity = incResource(resourceID, resourceAmount, plantTypeID, plantID);
                                playerOutStreams[j].writeUTF("new quantity of resource " + resourceID + ": " + newResQuantity);  

                            
                                String playersList = "";
                                for (int k = 0; k < PLAYERSPERGAME; k++) {
                                    int currPlayerID = gamePlayerIDs[k];
                                    String nameOfPlayer =  getNameOfPlayer(currPlayerID);
                                    playersList += "\nPlayer name: " + nameOfPlayer;
                                    playersList += "\n\tPlayer ID: " + gamePlayerIDs[k] + ", PlantID: " + gamePlantIDs[k];
                                }
                                playerOutStreams[j].writeUTF(playersList);
                            } catch (SQLException ex) {
                                Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                            }

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
                    System.out.println("end of attack");
                }
                
            }).start();
        } catch (IOException ex) {
            Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static int printPlantTypesAvailable(DataOutputStream out) throws SQLException, IOException {
        Statement stmtPT = (Statement) con.createStatement();
        Statement stmtPTRM = (Statement) con.createStatement();
        
        ResultSet rsPT = stmtPT.executeQuery("select * from plantType");
        int i = 0;
        
        while(rsPT.next()){
            i++;
            int plantType = rsPT.getInt(1);
            out.writeUTF("PlantType: "+ plantType +", Name: "+rsPT.getString(2));
            ResultSet psPTRM = stmtPTRM.executeQuery(
                "select fk_resource_PTRM, modval from plantTypeResMod where fk_plantType_PTRM = " +
                        plantType);
            while (psPTRM.next()) {
                out.writeUTF("\t resource type: " + psPTRM.getInt(1) + ", resource modifier: " + String.format("%.1f", psPTRM.getDouble(2)));
            }
        }
        return i; //returns number of available plantTypes
    }
    
    private static double incResource(int resourceID, int resourceAmount, int plantTypeID, int plantID) throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet ptrmRs = stmt.executeQuery("select modVal from PlantTypeResMod where PlantTypeResMod.fk_plantType_PTRM = " + plantTypeID 
            + " and PlantTypeResMod.fk_resource_PTRM = " + resourceID + ";");
        ptrmRs.next();
        Double modVal = ptrmRs.getDouble(1);

        //get resource quantity from PlantResActive
        ResultSet resQRs = stmt.executeQuery("select id_plantResActive, resQuantity from PlantResActive where PlantResActive.fk_plant_PlRa = " + plantID 
            + " and PlantResActive.fk_resource_PlRA = " + resourceID + ";");
        resQRs.next();
        int resActiveID = resQRs.getInt(1);
        double resQuantity = resQRs.getDouble(2);
        //playerOutStreams[j].writeUTF("previous quantity of resource " + resourceID + ": " + resQuantity);

        System.out.println("resource: " + resourceID + ", amount to add: " + resourceAmount + ", modval: " + modVal
            + ", current resource quantity: " + resQuantity + ", modVal*amount + currentResourceQuantity = "
            + resourceAmount*modVal + resQuantity);
        stmt.executeUpdate("Update PlantResActive Set ResQuantity = " + (resourceAmount*modVal + resQuantity)  + " where id_plantResActive = " + resActiveID + ";");
        return resourceAmount*modVal + resQuantity;
    }
    
    private static void loadPlayerPlantData(int currPlayer, Integer[] gamePlayerIDs, Integer[] gamePlantIDs, String plantName, int plantType) throws SQLException{

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
            int plantId = plantIdrst.getInt(1);
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
    


