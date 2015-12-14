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
    private static final Integer CURR_GAME_NUM = 1;
    private static final Integer NUM_TYPES = 3;
    private static final Integer BASE_SIZE = 0;
    private static final Integer DEFAULT_RES_QUANT = 10;
    
    static Connection con;

    
    public BotanyDatabase() throws SQLException{
        con = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + DB_NAME, "root", "");
        dbInit();
    }
    //main initialization method
    public static void dbInit() throws SQLException{       
               
        try{            
            //insertion 
            typeInit(1, "Flower");
            typeInit(1, "Vine");
            typeInit(1, "Orchid");
            
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
            resModInit(1, 2, 3, 2, 3, 1.2);
            
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
    
    public static boolean initialized() throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs;
        myRs = stmt.executeQuery("select * from PlantTypeResMod");
        return myRs.next();
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
    
    public static boolean checkPlayerName(String name)throws SQLException, NameExistsException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet rs = stmt.executeQuery("Select playerName from Players where playerName = " + name + ";)");
        if(rs.next())            
            return true;
        //throw new NameExistsException();
        else
            return false;            
    }
    public static boolean checkPlantName(String name, Player player)throws SQLException, NameExistsException{
        //what we have: player ID, player name, plant name
        int id;
        Statement stmt = (Statement) con.createStatement();
        ResultSet rs = stmt.executeQuery("Select id_Plant from PlantList where PlantList.plantName = " + name + ";)");
        if(!rs.next()) //if no plants exist with that name
            return false;
        id = rs.getInt(1);
        ResultSet rs2 = stmt.executeQuery("Select fk_plant_Plpl from PlayerPlants where fk_player_Plpl = " + player.getPlayerID() + " and fk_plant_Plpl = " + id);
        if(rs.next()) //if plant name exists AND belongs to current player            
            return true;
        else
            return false;
    }
    
    public static int insertNewPlayer(Player player) throws SQLException{
        ResultSet id;
        Statement stmt = (Statement) con.createStatement();
        
            stmt.executeUpdate("insert into Player(playerName) values ('" + player.getPlayerName() + "')");
            id = stmt.executeQuery("select id_player from Player where Player.playerName = " + player.getPlayerName() + ";");                        
            id.next();
            return id.getInt(1); 
    }
    
    public static int findExistingPlayer(Player player) throws SQLException{
        ResultSet id;
        Statement stmt = (Statement) con.createStatement();
        
        id =stmt.executeQuery("select id_player from Player where Player.playerName = " + player.getPlayerName() + ";");
        id.next();
        return id.getInt(1);
    }
    public static int insertNewPlant(Player player)throws SQLException{
        ResultSet id;
        int plantID;
        Statement stmt = (Statement) con.createStatement();
        stmt.executeUpdate("insert into PlantList (plantName, fk_plantType_PlLi, size) values ('" + player.plant.getPlantName()
            + "', " + player.plant.getTypeID() + ", " + BASE_SIZE + ");");
        id = stmt.executeQuery("select id_plant from PlantList where plantName = " + player.plant.getPlantName() + ";");
        id.next();
        plantID = id.getInt(1);
        stmt.executeUpdate("insert into PlayerPlants(fk_player_Plpl, fk_plant_Plpl) values (" + player.getPlayerID() 
                    + ", " + plantID + ");");
        return plantID;
        
    }
    public static void loadPlayer(Player player)throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        stmt.executeUpdate("insert into Masterlist (fk_game_Mas, fk_player_Mas) values (" 
                + CURR_GAME_NUM + ", " + player.getPlayerID() + ");");
        
        
    }
    public static int findExistingPlant(Player player)throws SQLException{
        Statement stmt = (Statement) con.createStatement();
         ResultSet plantID = stmt.executeQuery("select fk_plant_Plpl, id_Plant from PlayerPlants, PlantList where PlayerPlants.fk_Player_Plpl = " + player.getPlayerID() + " and PlantList.plantName = " + player.plant.getPlantName());
         if(plantID.next())
             return plantID.getInt(2);         
         else
            return 0; //just in case, should never return zero since plant name was already checked for existence 
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
    
    public static double getResQuant(Player player, int resID) throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet resQRs = stmt.executeQuery("select resQuantity from PlantResActive where PlantResActive.fk_plant_PlRa = " + player.plant.getPlantID() 
            + " and PlantResActive.fk_resource_PlRA = " + resID + ";");
        //Why get next?
        resQRs.next();
        double rQ= resQRs.getDouble(1);
        return rQ;
    }
    
    public static double getResModVal(Player player, int resID)throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet rsVals = stmt.executeQuery("select modVal from PlantTypeResMod where PlantTypeResMod.fk_plantType_PTRM = " + player.plant.getTypeID()
            + " and PlantTypeResMod.fk_resource_PTRM = " + resID + ";");
        rsVals.next();
        return rsVals.getDouble(1);
    }
    
    public static double incResQuant(Player player, int resID, int resQ)throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet ptrmRs = stmt.executeQuery("select modVal from PlantTypeResMod where PlantTypeResMod.fk_plantType_PTRM = " + player.plant.getTypeID()
            + " and PlantTypeResMod.fk_resource_PTRM = " + resID + ";");
        ptrmRs.next();
        Double modVal = getResModVal(player, resID);

        //get resource quantity from PlantResActive
        ResultSet resQRs = stmt.executeQuery("select id_plantResActive from PlantResActive where PlantResActive.fk_plant_PlRa = " + player.plant.getPlantID() 
            + " and PlantResActive.fk_resource_PlRA = " + resID + ";");
        resQRs.next();
        int resActiveID = resQRs.getInt(1);
        //double resQuantity = resQRs.getDouble(2);
        //playerOutStreams[j].writeUTF("previous quantity of resource " + resourceID + ": " + resQuantity);
        double newResQ = resQ*modVal + player.plant.getResQuant(resID);
        stmt.executeUpdate("Update PlantResActive Set ResQuantity = " + newResQ + " where id_plantResActive = " + resActiveID + ";");
        return newResQ;
    }
    
    public static double decResQuant(Player player, int resID, int resQ) throws SQLException, NegativeResourceException{       
        Statement stmt = (Statement) con.createStatement();
        //get resource quantity from PlantResActive
        ResultSet resQRs = stmt.executeQuery("select id_plantResActive from PlantResActive where PlantResActive.fk_plant_PlRa = " + player.plant.getPlantID()
            + " and PlantResActive.fk_resource_PlRA = " + resID + ";");
        resQRs.next();
        int resActiveID = resQRs.getInt(1);
        double oldResQ = player.plant.getResQuant(resID);
        if (oldResQ - resQ < 0) {
            throw new NegativeResourceException();
        }
        double newResQ = oldResQ - resQ; 
        stmt.executeUpdate("Update PlantResActive Set ResQuantity = " + newResQ  + " where id_plantResActive = " + resActiveID + ";");
        return newResQ;
    }
    
    public static int getPlayerID(int plantID)throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRS = stmt.executeQuery("Select fk_player_Plpl from PlayerPlants where PlayerPlants.fk_plants_PLpl = " + plantID + ";)");
        return myRS.getInt(1);
    }
    
    public static int applyGrowth(Player player, int sunlight) throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        
        int size = player.plant.getSize();
        double water = player.plant.getResQuant(1); //id 1 = index 0 = water
        double soil = player.plant.getResQuant(2); //id 2 = index 1 = soil
        
        size += (((int)(water*10)) + ((int)(soil*10)))*sunlight;
        
        stmt.executeUpdate("Update PlantList Set size = " + size + " where id_Plant = " + player.plant.getPlantID() + ";");
        return size;
    }
    
    //I kinda want to insert the plant along with the plant here, since we have the player id
    //Mine

     public static int getPlantSize(int plantID) throws SQLException{
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
    public static String[] getAllTypes() throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet nameRS = stmt.executeQuery("select * from plantType");
        String[] types = new String[NUM_TYPES*2];
        int index = 0;
        while(nameRS.next()){ 
            types[index] = Integer.toString(nameRS.getInt(2));
            types[++index] = nameRS.getString(1);           
            index++;
        }
        return types;        
    }
    
    public static String getTypeModVals(int plantType) throws SQLException{
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery(
                "select fk_resource_PTRM, modval from plantTypeResMod where fk_plantType_PTRM = " +
                        plantType);
        String info = "\tResource num: " + myRs.getInt(1) + ", Resource modifier: " + String.format("%1f", myRs.getDouble(2));
        return info;      
    }
    
    private static String getNameOfPlant(int plantID) throws SQLException {
        Statement stmt = (Statement) con.createStatement();
        ResultSet myRs = stmt.executeQuery("select plantName from plantList where id_plant = " + plantID + ";");
        myRs.next();
        return myRs.getString(1);
    }
    
    
      
    
}
