
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

public class PlantGameServer {
    private final static int SPRING = 8;
    private final static int SUMMER = 10;
    private final static int FALL = 6;
    private final static int WINTER = 4;
    
    static Connection con;  //connection to database
    static int playersNum = -1;
    static int playersReady = 0;
    static int playerRoundFinished = 0;
    
    final static String DEFAULTGAME = "\"TheGame\"";
    /**
     * @param args the command line arguments
     */
    static final int PLAYERSPERGAME = 2;
    
    public static synchronized int incPlayerRoundFinished() {
        playerRoundFinished++;
        return playerRoundFinished;
    }
    
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
                    "values (1, 1, 1.1)");
            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
                    "values (1, 2, 1.1)");
            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
                    "values (1, 3, 1.0)");
            
            
            // next plant type
            stmt.executeUpdate("insert into PlantType (typeName)\n" +
                    "values ('vine');");

            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
                    "values (2, 1, 1.0)");
            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
                    "values (2, 2, 1.0)");
            stmt.executeUpdate("insert into PlantTypeResMod (fk_plantType_PTRM, fk_resource_PTRM, modVal)\n" +
                    "values (2, 3, 1.1)");
    }
    
    private static String getNameOfPlayer(int playerID) throws SQLException {
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery("select playerName from Player where id_player = " + playerID + ";");
        myRs.next();
        return myRs.getString(1);
    }
    
    private static int getTypeOfPlant(int plantID) throws SQLException {
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery("select fk_plantType_PlLi from plantList where id_plant = " + plantID + ";");
        myRs.next();
        return myRs.getInt(1);
    }
    private static String getNameOfPlant(int plantID) throws SQLException {
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery("select plantName from plantList where id_plant = " + plantID + ";");
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
        
        Integer[] gamePlantSizeInitial = new Integer[PLAYERSPERGAME];
        
        Integer[] gamePlantGrowths = new Integer[PLAYERSPERGAME];
        for (int i = 0; i < PLAYERSPERGAME; i++) {
            gamePlantGrowths[i] = 0;
        }
        
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
                try {
                    for (int i = 0; i < PLAYERSPERGAME; i++) {
                        gamePlantSizeInitial[i] = getPlantSize(gamePlantIDs[i]);
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                for (int s = 0; s < 4; s++) {   //seasons
                    int season;
                    switch (s) {
                        case 0: season = SPRING; break;
                        case 1: season = SUMMER; break;
                        case 2: season = FALL; break;
                        case 3: season = WINTER; break;
                        default: season = SPRING; break;
                    }
                    String seasonName = "";
                    switch (s) {
                        case 0: seasonName += "spring"; break;
                        case 1: seasonName += "summer"; break;
                        case 2: seasonName += "fall"; break;
                        case 3: seasonName += "winter"; break;
                    }
                    
                    //instantiate game variables!
                    for (int i = 0; i < 3; i++) {  //rounds
                       HashMap<Integer, Integer> hmapAttackData = new HashMap<Integer, Integer>();

                       ArrayList<Integer> defendingPlants = new ArrayList<Integer>();
                       ArrayList<Integer> growingPlants = new ArrayList<Integer>();
                       
                       playerRoundFinished = 0;
                       final String seasonNameF = seasonName;
                       final int seasonF = season;
                       for (int playerIndex = 0; playerIndex < PLAYERSPERGAME; playerIndex++) { //players in game
                           final int j = playerIndex;
                            new Thread(() -> {
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
                                    out.writeUTF("The season is " + seasonNameF + "with sunlight " + seasonF + "/10");
                                     try {
                                         out.writeUTF("The following plants are in the game: ");
                                         for (int k = 0; k < PLAYERSPERGAME; k++) {
                                             out.writeUTF("Plant: " + getNameOfPlant(gamePlantIDs[k]) + ", size = " + getPlantSize(gamePlantIDs[k]) + ", growth in this game: " + gamePlantGrowths[k]);
                                         }
                                     } catch (SQLException ex) {
                                         Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                                         out.writeUTF("error reading plant list");
                                     }
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
                                            playersList += "\nPlayer name: " + nameOfPlayer + ((playerID == gamePlayerIDs[k])? "(you)": "");    // if playerID = 
                                            playersList += "\n\tPlayer ID: " + gamePlayerIDs[k] + ", PlantID: " + gamePlantIDs[k];
                                        }
                                        playerOutStreams[j].writeUTF(playersList);
                                    } catch (SQLException ex) {
                                        Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                                    }


                                    boolean inputLoop;
                                    do {
                                         inputLoop= false;
                                        playerOutStreams[j].writeUTF("enter attack (1) + plantToAttack + resource#, defend (2) or grow (3)");
                                        switch (getIntInRange(in, out, 1, 3, "select attack(1), defense(2) or grow(3)")) {
                                            case 1:
                                                playerOutStreams[j].writeUTF("select a plantID to attack.");
                                                int plantToAttack = in.readInt();    //throw exceptions for incorrect ID or ID same as Self
                                                inputLoop = true;
                                                String msg = "invalid plant to attack: ";
                                                for (int k = 0; k < PLAYERSPERGAME; k++) {
                                                    if (plantToAttack == gamePlantIDs[k]) {
                                                        inputLoop = false;
                                                    }
                                                }
                                                if (inputLoop) {
                                                    msg += "plantID not found.";
                                                }
                                                else if (plantToAttack == plantID) {
                                                    inputLoop = true;
                                                    msg += "attack someone else's plant lol";
                                                }
                                                if (inputLoop) {
                                                    playerOutStreams[j].writeUTF(msg);
                                                    break;
                                                }
                                                playerOutStreams[j].writeUTF("select a resource # to attack.");
                                                int resourceToAttack = getIntInRange(in, out, 1, 3, "Invalid resource number: choose 1-3");
                                                try {
                                                    decResource(3, 1, plantID); //decrease  scent
                                                    hmapAttackData.put(plantToAttack, resourceToAttack);
                                                } catch (SQLException ex) {
                                                    Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                                                } catch (NegativeResourceException ex) {
                                                    inputLoop = true;
                                                    playerOutStreams[j].writeUTF("not enough available resource. Make a new selection:");
                                                }
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
                                    } while (inputLoop);
                                } catch (IOException e) {
                                }  
                                System.out.println("end of playerloop");
                                incPlayerRoundFinished();
                            }).start();
                        }
                       while (playerRoundFinished < PLAYERSPERGAME) {   // wait till all players inputs are finished
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            System.out.println(" All " + playerRoundFinished + " players have input their moves at " + new Date());
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

                                   try {
                                       decResource(resourceToAttack, 4, plantToAttack);
                                   } catch (NegativeResourceException e) {
                                       stmt.executeUpdate("Update PlantResActive Set resQuantity = 0 where fk_plant_plRA = " + plantToAttack 
                                          + " and fk_resource_plRA = " + resourceToAttack + ";");
                                   }
                              }
                           }
                           //GROW
                           for (Integer gPlantID: growingPlants) {
                               System.out.println("growing plant " + gPlantID);
                               int typeOfPlant = getTypeOfPlant(gPlantID);
                               if (! hmapAttackData.containsKey(gPlantID)) {
                                       //SQL apply growth to each fk_plant_PlRA in plantResActive --> resQuantity = resQuantity + GROWCONSTANT*ln(score)
   //                                stmt.executeUpdate("Update PlantResActive Set resQuantity = resQuantity + " + 1 + " where fk_plant_plRA = " + gPlantID 
   //                                       + ";");
                                   for (int j = 1; j <= 3; j++) {
                                       double resQ = getResource(j, gPlantID);
                                       System.out.printf(gPlantID + ": old: %.1f", resQ);
                                       resQ = incResource(j, 1, typeOfPlant, gPlantID);  //applies modifiers
                                       System.out.printf("new: %.1f", resQ);
                                   }
                               }
                           }
                       } catch (SQLException ex) {
                           System.out.println("EXCEPTION SKIPPING GROWTH");
                           Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                       }
                       System.out.println("end of attack");
                       
                        try {
                            for (int plantID: gamePlantIDs) {
                                int plantSize = applyGrowth(plantID, season);
                                System.out.println("plantSize for plant " + plantID + ": " + plantSize);
                            }

                            for (int j = 0; j < PLAYERSPERGAME; j++) {
                                gamePlantGrowths[j] = getPlantSize(gamePlantIDs[j]) - gamePlantSizeInitial[j];
                            }
                        } catch (SQLException ex) {
                                Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                   }
                }
                
            }).start();
        } catch (IOException ex) {
            Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static int getPlantSize(int plantID) throws SQLException{
         Statement stmt = (Statement) con.createStatement();
        ResultSet rs = stmt.executeQuery("select size from PlantList where id_Plant = " + plantID + ";");
        rs.next();
        return rs.getInt(1);
    }
    
    private static int applyGrowth(int plantID, int sunlight) throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        
        int size = getPlantSize(plantID);
        double water = getResource(1, plantID);
        double soil = getResource(2, plantID);
        
        size += (((int)(water*10)) + ((int)(soil*10)))*sunlight;
        
        stmt.executeUpdate("Update PlantList Set size = " + size + " where id_Plant = " + plantID + ";");
        return size;
    }
    
    private static double getResource(int resource, int plantID) throws SQLException {
        Statement stmt = (Statement) con.createStatement();
        ResultSet rs = stmt.executeQuery("select resQuantity from PlantResActive where fk_plant_PlRA = " + plantID
        + " and fk_resource_PlRA = " + resource);
        rs.next();
        return Math.floor(rs.getDouble(1) * 10) / 10;
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
    
//    private static double getResource(int resourceID, int plantID) throws SQLException{
//        Statement stmt = (Statement) con.createStatement();
//        //get resource quantity from PlantResActive
//        ResultSet resQRs = stmt.executeQuery("select resQuantity from PlantResActive where PlantResActive.fk_plant_PlRa = " + plantID 
//            + " and PlantResActive.fk_resource_PlRA = " + resourceID + ";");
//        resQRs.next();
//        double resQuantity = resQRs.getDouble(1);
//        return resQuantity;
//    }
    
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
        
        stmt.executeUpdate("Update PlantResActive Set ResQuantity = " + (resourceAmount*modVal + resQuantity)  + " where id_plantResActive = " + resActiveID + ";");
        return resourceAmount*modVal + resQuantity;
    }
    
    private static double decResource(int resourceID, int resourceAmount, int plantID) throws SQLException, NegativeResourceException{
        Statement stmt = (Statement) con.createStatement();

        //get resource quantity from PlantResActive
        ResultSet resQRs = stmt.executeQuery("select id_plantResActive, resQuantity from PlantResActive where PlantResActive.fk_plant_PlRa = " + plantID 
            + " and PlantResActive.fk_resource_PlRA = " + resourceID + ";");
        resQRs.next();
        int resActiveID = resQRs.getInt(1);
        double resQuantity = resQRs.getDouble(2);
        if (resQuantity - resourceAmount < 0) {
            throw new NegativeResourceException();
        }
        
        stmt.executeUpdate("Update PlantResActive Set ResQuantity = " + (resQuantity - resourceAmount)  + " where id_plantResActive = " + resActiveID + ";");
        return resQuantity - resourceAmount;
    }
    
    
    
    //REWRITE THIS! insert IF NOT EXISTS
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
            Statement stmt = (Statement) con.createStatement();
//        // LOAD PLANTLIST //give plant temp unique name
//            
//            stmt.executeUpdate("insert into PlantList (plantName, fk_plantType_PlLi, size) \n values ('" + plantName
//            + "', " + plantType + ", 0" + ");");
//            
//
//            Statement stmt2 = (Statement) con.createStatement();
//            ResultSet plantIdrst = stmt2.executeQuery("select id_Plant from PlantList where PlantList.plantName = \"" + plantName + "\";");
//            plantIdrst.next();
//            int plantId = plantIdrst.getInt(1);
//            gamePlantIDs[currPlayer] = plantId;
//
//        //LOAD PLAYERPLANTS
//            stmt.executeUpdate("insert into PlayerPlants (fk_player_Plpl, fk_plant_Plpl) \n values (" + gamePlayerIDs[currPlayer]
//            + ", " + plantId + ");");
            if (findAndLoadPlantID(plantName, gamePlantIDs, plantType, gamePlayerIDs[currPlayer], currPlayer)) {
                //LOAD PLANTRESACTIVE
                for (int j = 1; j < 4; j++) {   //start with 3 of each resource
                    stmt.executeUpdate("insert into PlantResActive (fk_plant_PlRA, fk_resource_PlRA, resQuantity) \n values (" + gamePlantIDs[currPlayer]
                    + ", " + j + ", " + 5 + ");"); // CHEATING!!! our resources are numbered 1->3 in the database lol
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
                            
    }
    
    
    private static boolean findAndLoadPlantID(String plantName, Integer[] gamePlantIDs, int plantType, int playerID, int currentPlayer) throws SQLException {
        int plantID =-1; //just to keep compiler happy.
        plantName = plantName.trim();
        boolean plantFound = false;
        Statement stmt = (Statement) con.createStatement();
        
        //looks for plant with given name. plantFound set true if found.
        ResultSet plantIDRs = stmt.executeQuery("select fk_plant_Plpl from PlayerPlants where fk_Player_Plpl = " + playerID);
        
        Statement stmt2 = (Statement) con.createStatement();
        ResultSet plantNameAndTypeRs;
        
        while (plantIDRs.next()) {
            int tuplePlantID = plantIDRs.getInt(1);
            
            plantNameAndTypeRs = stmt2.executeQuery("select plantName, fk_plantType_PlLi from PlantList where id_plant = " + tuplePlantID);
            
            while (plantNameAndTypeRs.next()) {
                if (plantNameAndTypeRs.getString(1).trim().equals(plantName)) {
                    if (plantType == plantNameAndTypeRs.getInt(2)) {
                        plantFound = true;
                        plantID = tuplePlantID;
                    }
                }
            }
        }
        if (!plantFound) {
            stmt.executeUpdate("insert into PlantList(plantName, fk_plantType_PlLi, size) \n values (\"" + plantName + "\", " + plantType + ", " + 0 + ");");
            ResultSet idPlant = stmt.executeQuery("select id_plant from PlantList where plantName = \"" + plantName + "\";");
            idPlant.next();
            plantID = idPlant.getInt(1);
            stmt.executeUpdate("insert into PlayerPlants(fk_player_Plpl, fk_plant_Plpl) \n values (" + playerID 
                    + ", " + plantID + ");");
        }
        if (plantID == -1)
            throw new SQLException("fuck plantID not initialized");
        gamePlantIDs[currentPlayer] = plantID;
        System.out.println("plant ID:" + gamePlantIDs[currentPlayer]);
        return !plantFound;
    }
        

    
    private static Integer[] findAndLoadPlayerID(String playerName, Integer[] gamePlayerIDs, int currentPlayer) throws SQLException {
        playerName = playerName.trim();
        boolean playerFound = false;
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery("select playerName from Player");
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
    


