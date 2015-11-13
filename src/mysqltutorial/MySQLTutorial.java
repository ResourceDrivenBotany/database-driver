/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mysqltutorial;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mishakanai
 */
public class MySQLTutorial {

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
            
            
            
            while (myRs.next()) {
                System.out.println(myRs.getInt(1) + ", " + myRs.getString(2) + ", " + myRs.getInt(3));
            }
        } catch (SQLException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
