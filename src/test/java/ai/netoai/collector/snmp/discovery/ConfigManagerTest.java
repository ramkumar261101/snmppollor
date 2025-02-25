package ai.netoai.collector.snmp.discovery;

import ai.netoai.collector.deviceprofile.ConfigManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public class ConfigManagerTest {
    
    public ConfigManagerTest() {
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
    public void testToplogyConfig(){
        ConfigManager cm = ConfigManager.getInstance();
        cm.start("src/test/resources/config");
    }
}
