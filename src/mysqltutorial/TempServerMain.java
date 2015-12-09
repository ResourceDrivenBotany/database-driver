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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static mysqltutorial.PlantGameServer.con;
import mysqltutorial.BotanyDatabase;
import static mysqltutorial.PlantGameServer.playersNum;
import mysqltutorial.Player;

/**
 *
 * @author taylo
 */
public class TempServerMain {
    
    final static String DEFAULTGAME = "\"TheGame\"";
    
    static final int PLAYERSPERGAME = 2;
    static int playersNum = -1;
    static int playersReady = 0;
    
    private final static int SPRING = 8;
    private final static int SUMMER = 10;
    private final static int FALL = 6;
    private final static int WINTER = 4;
    
    private static final Integer NUM_TYPES = 3;
    private static final Integer MIN_TYPE = 1;
    private static final Integer MAX_TYPE = NUM_TYPES;
    private static final Integer MIN_RES_VAL = 1;
    private static final Integer MAX_RES_VAL = 3;

    public static synchronized int incrementPlayersNum() {
        playersNum++;
        return playersNum;
    }
    public static synchronized int incrementPlayersReady() {
        playersReady++;
        return playersReady;
    }
    
    private static int getIntInRange(DataInputStream in, DataOutputStream out, int low, int high, String message) throws IOException {
        int fromClient = getIntLoop(in, out, "failed integer input, Try again.");
        while (fromClient < low || fromClient > high) {
        out.writeUTF(message);
        fromClient = getIntLoop(in, out, "failed integer input, Try again.");
        }
        return fromClient;
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
    
    public static void main(String[] args){
        
        BotanyDatabase db;
        
        
        try {
           con = DriverManager.getConnection("jdbc:mysql://localhost:3306/plantgamedb", "root", "");

           
        } catch (SQLException ex) {
            Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
           //pTRM_Initialize();   //initialize plantTypeResMod table
            BotanyDatabase.dbInit();
            
        } catch (SQLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //CREATE DEFAULT GAME
        try {
            //Statement stmt = (Statement) con.createStatement();
            //stmt.executeUpdate("insert into GameInstance (gameName) \n values (" + DEFAULTGAME + ");");
            BotanyDatabase.gameInit(DEFAULTGAME);
            
        } catch (SQLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Integer[] gamePlayerIDs = new Integer[PLAYERSPERGAME];
        //Integer[] gamePlantIDs = new Integer[PLAYERSPERGAME];
        //Integer[] gamePlantTypes = new Integer[PLAYERSPERGAME];
        
        //example player array
        Player[] gamePlayers = new Player[PLAYERSPERGAME];
        //example player array
        
        DataInputStream[] playerInStreams = new DataInputStream[PLAYERSPERGAME];
        DataOutputStream[] playerOutStreams = new DataOutputStream[PLAYERSPERGAME];
        
        //Integer[] gamePlantSizeInitial = new Integer[PLAYERSPERGAME];
        
        //Integer[] gamePlantGrowths = new Integer[PLAYERSPERGAME];
        //for (int i = 0; i < PLAYERSPERGAME; i++) {
         //   gamePlantGrowths[i] = 0;
       // }
        
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(8000);
            for (int i = 0; i < PLAYERSPERGAME; i++) {
                new Thread(() -> {  //finds connections, creates playerIDs in gamePlayerIDs

                    try {
                         Socket socket = serverSocket.accept();

                        DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());
                        DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream());

                        final int currPlayer = incrementPlayersNum();
                        
                        //Strictly for new players.
                        //

                        if (currPlayer < PLAYERSPERGAME) {
                            playerInStreams[currPlayer] = inputFromClient;
                            playerOutStreams[currPlayer] = outputToClient;

                            // Read in from client
                            outputToClient.writeUTF("Enter player's name! \n(will be created in database if doesn't exist):");
                            String playerName = inputFromClient.readUTF();                     
                            outputToClient.writeUTF("Enter plant's name! \n(will be created in database if doesn't exist):");
                            String plantName = inputFromClient.readUTF();
                            
                            //returns true if name already exists
                            try{
                            BotanyDatabase.checkPlayerName(playerName);                            
                            BotanyDatabase.checkPlantName(plantName);
                            }
                            catch(SQLException ex) {                            
                            }
                            
                            gamePlayers[currPlayer].playerName = playerName.trim();
                            gamePlayers[currPlayer].plant.plantName = plantName.trim();
                            
                            outputToClient.writeUTF("Enter your plantType Number. Available plantTypes: ");
                            try {
                                //int numberOfTypes = printPlantTypesAvailable(outputToClient);
                                int plantType = getIntInRange(inputFromClient, outputToClient, MIN_TYPE, MAX_TYPE, "Invalid Type. Enter an integer:");
                                gamePlayers[currPlayer].plant.plantTypeID = plantType;
                                //gamePlantTypes[currPlayer] = plantType;
                                
                                //print local variables
                                System.out.println("playername: " + playerName + ", plantName: " + plantName + ", plantType " + plantType);
                                //print array
                                System.out.println("playername: " + gamePlayers[currPlayer].playerName + ", plantName: " 
                                        + gamePlayers[currPlayer].plant.plantName + ", plantType " + gamePlayers[currPlayer].plant.plantTypeID);
                            
                                //add player ID to gamePlayerIDs[]: creates player in database if not found
                                //findAndLoadPlayerID(playerName, gamePlayerIDs, currPlayer);
                                //loadPlayerPlantData(currPlayer, gamePlayerIDs, gamePlantIDs, plantName, plantType);
                                
                                gamePlayers[currPlayer].playerID = BotanyDatabase.insertPlayer(gamePlayers[currPlayer]);
                                
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
                        //gamePlantSizeInitial[i] = getPlantSize(gamePlantIDs[i]);
                        gamePlayers[i].plant.size = BotanyDatabase.getPlantSize(gamePlayers[i].plant.plantID);
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

                       for (int j = 0; j < PLAYERSPERGAME; j++) { //players in game
                           DataInputStream in = playerInStreams[j];
                           DataOutputStream out = playerOutStreams[j];
                           //int playerID = gamePlayerIDs[j];
                           int playerID = gamePlayers[j].playerID;
                           //int plantID = gamePlantIDs[j];
                           int plantID = gamePlayers[j].plant.plantID;
                           //int plantTypeID = gamePlantTypes[j];
                           int plantTypeID = gamePlayers[j].plant.plantTypeID;
                           double modVal;
                           int resActiveID;
                           int resQuantity;

                           ///vvv increment resource
                           try {   //read from Client and perform gameplay operations
                               out.writeUTF("The season is " + seasonName + "with sunlight " + season + "/10");
                                //try {
                                    out.writeUTF("The following plants are in the game: ");
                                    for (int k = 0; k < PLAYERSPERGAME; k++) {
                                        //out.writeUTF("Plant: " + getNameOfPlant(gamePlantIDs[k]) + ", size = " + getPlantSize(gamePlantIDs[k]) + ", growth in this game: " + gamePlantGrowths[k]);
                                        gamePlayers[k].plant.setGrowth();
                                        out.writeUTF("Plant: " + gamePlayers[k].plant.plantName + ", size = " + gamePlayers[k].plant.size + ", growth in this game: " + gamePlayers[k].plant.growth);
                                   }
                                //} catch (SQLException ex) {
                                 //   Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                                //    out.writeUTF("error reading plant list");
                                //}
                              //thinking we gonna change this, increment 1 each time. Incrementing up to 10 is just sorta weird.
                              //doesn't leave much point to actual gameplay
                              //keep it simple
                              out.writeUTF("please enter resource [1. water, 2. soil, 3. scent]\n" 
                                      + "and integer quantity to increment 1-10");
                              int resourceID = getIntInRange(in, out, MIN_RES_VAL, MAX_RES_VAL, "Resources are numbered 1-3. Try again:");
                              System.out.println("debugging1");
                              int resourceAmount = getIntInRange(in, out, 1, 10, "enter integer 1-10:");
                              System.out.println("resourceID: " + resourceID + ", resourceAmount: " + resourceAmount);

                              Statement stmt;

                               try {   //get modval for chosen resource
                                   //stmt = (Statement) con.createStatement();
                                   //ResultSet resQRs = stmt.executeQuery("select resQuantity from PlantResActive where PlantResActive.fk_plant_PlRa = " + plantID 
                                   //    + " and PlantResActive.fk_resource_PlRA = " + resourceID + ";");
                                   gamePlayers[j].plant.resourceQuants[resourceID] = BotanyDatabase.getResQuant(gamePlayers[j], resourceID);
                                   //resQRs.next();
                                  // double  rQ= resQRs.getDouble(1);
                                   playerOutStreams[j].writeUTF("previous quantity of resource " + resourceID + ": " + String.format("%.1f", gamePlayers[j].plant.resourceQuants[resourceID]));  
                                   //double newResQuantity = incResource(resourceID, resourceAmount, plantTypeID, plantID);
                                   gamePlayers[j].plant.resourceQuants[resourceID] = BotanyDatabase.incResQuant(gamePlayers[j], resourceID, resourceAmount);
                                   //playerOutStreams[j].writeUTF("new quantity of resource " + resourceID + ": " + newResQuantity);  
                                   playerOutStreams[j].writeUTF("new quantity of resource " + resourceID + ": " +  gamePlayers[j].plant.resourceQuants[resourceID]); 


                                   String playersList = "";
                                   for (int k = 0; k < PLAYERSPERGAME; k++) {
                                       //int currPlayerID = gamePlayerIDs[k];
                                       int currPlayerID = gamePlayers[k].playerID;
                                       //String nameOfPlayer =  getNameOfPlayer(currPlayerID);
                                       String nameOfPlayer = gamePlayers[k].playerName;
                                       playersList += "\nPlayer name: " + nameOfPlayer + ((playerID == gamePlayers[k].playerID)? "(you)": "");    // if playerID = 
                                       playersList += "\n\tPlayer ID: " + gamePlayers[k].playerID + ", PlantID: " + gamePlayers[k].plant.plantID;
                                   }
                                   playerOutStreams[j].writeUTF(playersList);
                               } catch (SQLException ex) {
                                   Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                               }


                               boolean inputLoop;
                               do {
                                    inputLoop= false;
                                   playerOutStreams[j].writeUTF("enter attack (1) + plantToAttack + resource#, defend (2) or grow (3)");
                                   switch (getIntInRange(in, out, MIN_RES_VAL, MAX_RES_VAL, "select attack(1), defense(2) or grow(3)")) {
                                       case 1:
                                           playerOutStreams[j].writeUTF("select a plantID to attack."); 
                                           int plantToAttack = in.readInt();    //throw exceptions for incorrect ID or ID same as Self
                                           try{
                                           int playerToAttack = BotanyDatabase.getPlayerID(plantToAttack);
                                           }
                                           catch(SQLException ex){                                               
                                           }
                                           inputLoop = true;
                                           String msg = "Invalid plant to attack: ";
                                           for (int k = 0; k < PLAYERSPERGAME; k++) {
                                               //if (plantToAttack == gamePlantIDs[k]) {
                                               if(plantToAttack == gamePlayers[k].plant.plantID){
                                                   inputLoop = false;
                                               }
                                           }
                                           if (inputLoop) {
                                               msg += "plantID not found.";
                                           }
                                           else if (plantToAttack == plantID) {
                                               inputLoop = true;
                                               msg += "No arboreal suicide allowed.";
                                           }
                                           if (inputLoop) {
                                               playerOutStreams[j].writeUTF(msg);
                                               break;
                                           }
                                           playerOutStreams[j].writeUTF("select a resource # to attack.");
                                           int resourceToAttack = getIntInRange(in, out, MIN_RES_VAL, MAX_RES_VAL, "Invalid resource number: choose 1-3");
                                           try {
                                               //decResource(3, 1, plantID); //decrease  scent
                                               gamePlayers[j].plant.resourceQuants[3] = BotanyDatabase.decResQuant(gamePlayers[j], 3, 1);
                                               hmapAttackData.put(plantToAttack, resourceToAttack);
                                           } catch (SQLException ex) {
                                               Logger.getLogger(PlantGameServer.class.getName()).log(Level.SEVERE, null, ex);
                                           } catch (NegativeResourceException ex) {
                                               inputLoop = true;
                                               playerOutStreams[j].writeUTF("not enough available resources. Make a new selection:");
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
                               int playerToAttack = 0;
                               try{
                                playerToAttack = BotanyDatabase.getPlayerID(plantToAttack);
                                }
                                catch(SQLException ex){                                               
                                }
                               if (! defendingPlants.contains(plantToAttack)) {
                                   resourceToAttack = (int) mentry.getValue();

                                   try {//why 4?
                                       //decResource(resourceToAttack, 4, plantToAttack);
                                       gamePlayers[playerToAttack].plant.resourceQuants[resourceToAttack] = BotanyDatabase.decResQuant(gamePlayers[playerToAttack], 4, plantToAttack);
                                   } catch (NegativeResourceException e) {
                                       stmt.executeUpdate("Update PlantResActive Set resQuantity = 0 where fk_plant_plRA = " + plantToAttack 
                                          + " and fk_resource_plRA = " + resourceToAttack + ";");
                                   }
                              }
                           }
                           //GROW
                           for (Integer gPlantID: growingPlants) {
                               System.out.println("growing plant " + gPlantID);
                               //int typeOfPlant = getTypeOfPlant(gPlantID);
                               int gPlayerID = BotanyDatabase.getPlayerID(gPlantID);
                               int typeOfPlant = gamePlayers[gPlayerID].plant.plantTypeID;
                               if (! hmapAttackData.containsKey(gPlantID)) {
                                       //SQL apply growth to each fk_plant_PlRA in plantResActive --> resQuantity = resQuantity + GROWCONSTANT*ln(score)
   //                                stmt.executeUpdate("Update PlantResActive Set resQuantity = resQuantity + " + 1 + " where fk_plant_plRA = " + gPlantID 
   //                                       + ";");
                                   for (int j = 1; j <= 3; j++) {
                                       System.out.printf(gPlantID + ": old: %.1f", gamePlayers[gPlayerID].plant.resourceQuants[j]);
                                       //resQ = incResource(j, 1, typeOfPlant, gPlantID);  //applies modifiers
                                       gamePlayers[gPlayerID].plant.resourceQuants[j] = BotanyDatabase.incResQuant(gamePlayers[gPlayerID], j, 1);                                       
                                       System.out.printf("new: %.1f", gamePlayers[gPlayerID].plant.resourceQuants[j]);
                                   }
                               }
                           }
                       } catch (SQLException ex) {
                           System.out.println("EXCEPTION SKIPPING GROWTH");
                           Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
                       }
                       System.out.println("end of attack");
                       
                        try {
                            //for (int plantID: gamePlantIDs) {
                            for(int c = 0; c < PLAYERSPERGAME; c++ ){
                                //int plantSize = applyGrowth(plantID, season);
                                gamePlayers[c].plant.size = BotanyDatabase.applyGrowth(gamePlayers[c], season);
                                gamePlayers[c].plant.setGrowth();
                                System.out.println("plantSize for plant " + gamePlayers[c].plant.plantID + ": " + gamePlayers[c].plant.size);
                            }

                           // for (int j = 0; j < PLAYERSPERGAME; j++) {
                                //gamePlantGrowths[j] = getPlantSize(gamePlantIDs[j]) - gamePlantSizeInitial[j];
                            //}
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
}
   
