/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mysqltutorial;

/**
 *
 * @author taylor
 */
class Player {
    
    final int NUM_RESOURCES = 3;
    String playerName;
    int playerID;
    Plant plant;
   
    //PlayerPlant list is irrelevent
    int fkPlayerPlantPlayer;    
    //PlayerPlant list is irrelevent
    
    
    private Player(String name, int ID, int fk){
        this.playerName = name;
        this.playerID = ID;
        this.fkPlayerPlantPlayer = fk;
        plant = new Plant();
    }
    
    //setters
    
    void setPlayerName(String name){
        this.playerName = name;
    }
    
    void setPlayerID(int ID){
        this.playerID = ID;
    }
    
    void setfkPlayerPlant(int fk){
        this.fkPlayerPlantPlayer = fk;
    }
    
    String getPlayerName(){
        return playerName;
    }
    
    int getPlayerID(){
        return playerID;
    }
    
    int setfkPlayerPlant(){
        return fkPlayerPlantPlayer;
    }
    
    class Plant{
        //need to change how players and their plants are inserted
        //in order to use this constructor
        private Plant(String name, int plantID, int typeID){
        this.plantName = name;
        this.plantID = plantID;
        this.plantTypeID = typeID;
        }
        private Plant(){}
        String plantName;
        int plantID; //from Plantlist
        
        //PlayerPlant list is irrelevent
        int fkPlayerPlantPlant;
        //PlayerPlant list is irrelevent
        
        int plantTypeID;
        int initialSize;
        int size;
        int growth;
        int[] resourcesIDS = {1, 2, 3};
        
        //for in-game tracking, should be sync with database
        double[] resourceQuants = new double[NUM_RESOURCES];
        
        void setID(int id){
            this.plantID = id;
        }
        
        void setSize(int size){
            this.size = size;
        }
        
        void setTypeID(int id){
            this.plantTypeID = id;
        }
                
        void setResVals(double val1, double val2, double val3){
            this.resourceQuants[0] = val1;
            this.resourceQuants[1] = val2;
            this.resourceQuants[2] = val3;
        }
        
        void setGrowth(){
        this.growth = this.size-this.initialSize;
        }
                
    }
}
