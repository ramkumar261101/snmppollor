/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.netoai.collector.entity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author lokesh
 */
public class MysqlTest {
    
    public MysqlTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testMysqlFailover() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306,localhost:3307/designer", "root", "");
        PreparedStatement ps = connection.prepareStatement("select firstName,lastName,Username from user_info");
        ResultSet rs = ps.executeQuery();
        while(rs.next()) {
            System.out.print(rs.getString(1) + "\t" + rs.getString(2) + "\t" + rs.getString(3));
            System.out.println("");
        }
        connection.close();
    }
}
