/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mysqltutorial;

import java.sql.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import static mysqltutorial.PlantGameServer.con;
import mysqltutorial.Player;



/**
 *
 * @author Taylor
 */
class BotanyDatabase{
    
    public static final String DB_NAME = "plantgamedb";
    private static final String ONE = "1";
    private static final String TWO = "2";
    private static final String THREE = "3";
    private static final String GAME_NAME = "DEFAULTGAME";
    private static Integer CURRENT_GAME = 1;
    private static final Integer NUM_TYPES = 3;
    
    static Connection con;

    
    public BotanyDatabase() throws SQLException{
        con = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + DB_NAME, "root", "");
        dbInit();
    }
    //main initialization method
    public static void dbInit() throws SQLException{       
               
        try{            
            //insertion 
            typeInit(1, "Orchid");
            typeInit(1, "Venus Flytrap");
            typeInit(1, "Monkshood");
            
            resRefInit(1, 2, "water", 1);
            resRefInit(1, 2, "soil", 1);
            resRefInit(1, 2, "scent", 1);
            
            
            //insertion columns fk plantType, fk resource, modval 
            //insertion values int, int, double
            //Orchid
            //ENHANCED MAJOR water
            //ENHANCED MINOR soil
            resModInit(1, 2, 3, 1, 1, 1.2);
            resModInit(1, 2, 3, 1, 2, 1.1);
            resModInit(1, 2, 3, 1, 3, 1.0);
            
            //Flytrap
            //EHANCED SCENTS
            resModInit(1, 2, 3, 2, 1, 1.0);
            resModInit(1, 2, 3, 2, 2, 1.0);
            resModInit(1, 2, 3, 2, 3, 1.1);
            
            //MonksHood
            //ENHANCED MAJOR soil
            //ENHANCED MINOR water
            resModInit(1, 2, 3, 3, 1, 1.1);
            resModInit(1, 2, 3, 3, 2, 1.2);
            resModInit(1, 2, 3, 3, 3, 1.0);
            
            
        }
        catch(SQLException ex){
            
        }
    }
    
    private static void typeInit(int col, String name) throws SQLException{
        PreparedStatement plantTypeInit = con.prepareStatement("insert into "
                + "PlantType (typename) values(?);");
        plantTypeInit.setString(col, name);
        plantTypeInit.executeUpdate();
    }
    
    private static void resRefInit(int col1, int col2, String name, int val)
            throws SQLException{
        PreparedStatement plantResRefInit = con.prepareStatement("insert into "
                + "PlantResReference(ResourceName, resVal) values (?, ?);");
        plantResRefInit.setString(col1, name);
        plantResRefInit.setInt(col2, val);
        plantResRefInit.executeUpdate();
        
    }
    
    private static void resModInit(int col1, int col2, int col3, 
            int val1, int val2, double val3) throws SQLException{
        PreparedStatement plantResModInit = con.prepareStatement("insert into "
                + "PlantTypeResMod(fk_plantType_PTRM, fk_resource_PTRM, modVal) "
                + "values (?, ?, ?);");
        plantResModInit.setInt(col1, val1);
        plantResModInit.setInt(col2, val2);
        plantResModInit.setDouble(col3, val3);
        plantResModInit.executeUpdate();
    }
    
    public static void gameInit(String name)throws SQLException{
        Statement stmt = con.createStatement();
            stmt.executeUpdate("insert into GameInstance (gameName) values (" + name + ");");
    }
    //Probably not gonna use
    //Not necessary if we use player objects
    private static TreeMap<Integer, String> getPlayers()throws SQLException{
        boolean isPlayer = false;
        TreeMap<Integer, String> map = null;
        Statement stmt = (Statement) con.createStatement();
        ResultSet playerRS = stmt.executeQuery("select * from Player;");
        while(playerRS.next()){
            //Map makes ID,Name (key, value) pairs easy
            map.put(playerRS.getInt(1), playerRS.getString(2));      
        }      
        return map;
    }
    
    public static void checkPlayerName(String name)throws SQLException, NameExistsException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet rs = stmt.executeQuery("Select playerName from Players where playerName = " + name + ";)");
        if(rs.next())            
            throw new NameExistsException();        
    }
    public static void checkPlantName(String name)throws SQLException, NameExistsException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet rs = stmt.executeQuery("Select plantName from PlantList where plantName = " + name + ";)");
        if(rs.next())            
            throw new NameExistsException();  
    }
    
    public static double getResQuant(Player player, int resID) throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet resQRs = stmt.executeQuery("select resQuantity from PlantResActive where PlantResActive.fk_plant_PlRa = " + player.plant.plantID 
            + " and PlantResActive.fk_resource_PlRA = " + resID + ";");
        //Why get next?
        //resQRs.next();
        double rQ= resQRs.getDouble(1);
        return rQ;
    }
    public static double incResQuant(Player player, int resID, int resQ)throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet ptrmRs = stmt.executeQuery("select modVal from PlantTypeResMod where PlantTypeResMod.fk_plantType_PTRM = " + player.plant.plantTypeID
            + " and PlantTypeResMod.fk_resource_PTRM = " + resID + ";");
        ptrmRs.next();
        Double modVal = ptrmRs.getDouble(1);

        //get resource quantity from PlantResActive
        ResultSet resQRs = stmt.executeQuery("select id_plantResActive from PlantResActive where PlantResActive.fk_plant_PlRa = " + player.plant.plantID 
            + " and PlantResActive.fk_resource_PlRA = " + resID + ";");
        resQRs.next();
        int resActiveID = resQRs.getInt(1);
        //double resQuantity = resQRs.getDouble(2);
        //playerOutStreams[j].writeUTF("previous quantity of resource " + resourceID + ": " + resQuantity);
        double newResQ = resQ*modVal + player.plant.resourceQuants[resID];
        stmt.executeUpdate("Update PlantResActive Set ResQuantity = " + (newResQ) + " where id_plantResActive = " + resActiveID + ";");
        return newResQ;
    }
    public static double decResQuant(Player player, int resID, int resQ) throws SQLException, NegativeResourceException{
        Statement stmt = (Statement) con.createStatement();

        //get resource quantity from PlantResActive
        ResultSet resQRs = stmt.executeQuery("select id_plantResActive from PlantResActive where PlantResActive.fk_plant_PlRa = " + player.plant.plantID
            + " and PlantResActive.fk_resource_PlRA = " + resID + ";");
        resQRs.next();
        int resActiveID = resQRs.getInt(1);
        double oldResQ = player.plant.resourceQuants[resID];
        if (oldResQ - resQ < 0) {
            throw new NegativeResourceException();
        }
        double newResQ = oldResQ - resQ; 
        stmt.executeUpdate("Update PlantResActive Set ResQuantity = " + (newResQ)  + " where id_plantResActive = " + resActiveID + ";");
        return newResQ;
    }
    public static int getPlayerID(int plantID)throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRS = stmt.executeQuery("Select fk_player_Plpl from PlayerPlants where PlayerPlants.fk_plants_PLpl = " + plantID + ";)");
        return myRS.getInt(1);
    }
    
    public static int applyGrowth(Player player, int sunlight) throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        
        int size = player.plant.size;
        double water = player.plant.resourceQuants[1];
        double soil = player.plant.resourceQuants[2];
        
        size += (((int)(water*10)) + ((int)(soil*10)))*sunlight;
        
        stmt.executeUpdate("Update PlantList Set size = " + size + " where id_Plant = " + player.plant.plantID + ";");
        return size;
    }
    
    //I kinda want to insert the plant along with the plant here, since we have the player id
    //Mine
    public static int insertPlayer(Player player) throws SQLException{
        //get list of players
        //int currID;
        //TreeMap<Integer, String> map = getPlayers();
        ResultSet id;
        Statement stmt = (Statement) con.createStatement();
        
            stmt.executeUpdate("insert into Player(playerName) values ('" + player.playerName + "')");
            id = stmt.executeQuery("select id_player from Player where Player.playerName = " + player.playerName + ";");                        
            return id.getInt(1); 
    }
    
     public static int getPlantSize(int plantID) throws SQLException{
         Statement stmt = (Statement) con.createStatement();
        ResultSet rs = stmt.executeQuery("select size from PlantList where id_Plant = " + plantID + ";");
        rs.next();
        return rs.getInt(1);
    }
    
    
        //Misha's
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
        //Misha's load plants
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
            //quick dirty fix
                //if()
            stmt.executeUpdate("insert into PlantList (plantName, fk_plantType_PlLi, size) \n values ('" + plantName
            + "', " + plantType + ", 0" + ");");
            

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
    
    public static boolean insertPlant(int plantType, String plantName) throws SQLException{
        
    }
    
    public static boolean insertPlant(int plantType, String playerName, String plantName) throws SQLException{
        //allow duplicate plant names?
        //don't see why not
        Statement
        stmt.executeUpdate("insert into PlantList (plantName, fk_plantType_PlLi) \n values ('" + plantName
                                + "', " + plantType + ");");
    }
    
    private static get playerID(){}
    
   private static int getPlantSize(int plantID) throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet rs = stmt.executeQuery("select size from PlantList where id_Plant = " + plantID + ";");
        rs.next();
        return rs.getInt(1);
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
    private static ResultSet getAllTypes() throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery("select * from PlantType;");
        return myRs;
    }
    
    private static ResultSet getAllTypeModVals(String plantType) throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery(
                "select fk_resource_PTRM, modval from plantTypeResMod where fk_plantType_PTRM = " +
                        plantType);
        return myRs;       
    }
    
    private static 
    private static String getNameOfPlant(int plantID) throws SQLException {
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery("select plantName from plantList where id_plant = " + plantID + ";");
        myRs.next();
        return myRs.getString(1);
    }
            
      
    
}