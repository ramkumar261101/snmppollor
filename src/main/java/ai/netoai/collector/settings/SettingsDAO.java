/**
 *
 */
package ai.netoai.collector.settings;

import ai.netoai.collector.Constants;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyVetoException;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class SettingsDAO {

    private static final Logger log = LoggerFactory.getLogger(SettingsDAO.class.getName());
    private static final String tracePrefix = "[" + SettingsDAO.class.getSimpleName() + "]: ";

    private static ComboPooledDataSource globalDS;

    public enum SettingType {
        INT("int", "integer", "INT", "INTEGER"),
        LONG("long", "LONG"),
        FLOAT("float", "FLOAT"),
        DOUBLE("double", "DOUBLE"),
        STRING("string", "STRING"),
        BOOLEAN("BOOLEAN", "boolean");

        private List<String> values = new ArrayList<>();

        SettingType(String... values) {
            this.values.addAll(Arrays.asList(values));
        }

        public List<String> getValues() {
            return this.values;
        }

        public static SettingType getEnum(String dataType) {
            for(SettingType type : values()) {
                if ( type.getValues().contains(dataType.toLowerCase()) ) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid ENUM value " + dataType + " for SettingsType");
        }

    }

    private static Configuration globalConfig;
    public static Configuration getConfig() {
        return globalConfig;

    }

    private synchronized static ComboPooledDataSource initDataSource() {
        Configuration cfg = getConfig();
        ComboPooledDataSource dataS = new ComboPooledDataSource();
        try {
            dataS.setDriverClass(cfg.getProperty("hibernate.connection.driver_class"));
        } catch (PropertyVetoException e) {
            log.error(tracePrefix + "Error while setting driver class", e);
        }
        dataS.setJdbcUrl(cfg.getProperty("hibernate.connection.url"));
        dataS.setUser(cfg.getProperty("hibernate.connection.username"));
        dataS.setPassword(cfg.getProperty("hibernate.connection.password"));
        dataS.setMaxPoolSize(Integer.parseInt(cfg.getProperty("hibernate.c3p0.max_size")));
        dataS.setMinPoolSize(Integer.parseInt(cfg.getProperty("hibernate.c3p0.min_size")));
        dataS.setMaxIdleTime(Integer.parseInt(cfg.getProperty("hibernate.c3p0.timeout")));
        dataS.setIdleConnectionTestPeriod(Integer.parseInt(cfg.getProperty("hibernate.c3p0.idle_test_period")));
        dataS.setUnreturnedConnectionTimeout(Integer.parseInt(cfg.getProperty("hibernate.c3p0.unreturnedConnectionTimeout")));
        dataS.setDebugUnreturnedConnectionStackTraces(Boolean.parseBoolean(cfg.getProperty("hibernate.c3p0.debugUnreturnedConnectionStackTraces")));
        log.info(tracePrefix + "Initializing DataSource with URL: " + cfg.getProperty("hibernate.connection.url"));
        return dataS;
    }

    private Connection getConnection() {
        ComboPooledDataSource ds = null;
        synchronized (this) {
            if (null == globalDS) {
                globalDS = initDataSource();
            }
            ds = globalDS;
        }

        try {
            log.trace(tracePrefix + "Connecting to: " + ds.getJdbcUrl());
            return ds.getConnection();
        } catch (SQLException e) {
            log.error(tracePrefix + "Failed fetching connection", e);
            return null;
        }
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "SELECT DISTINCT category FROM nms_settings";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString(1));
            }
        } catch (Exception ex) {
            log.error(tracePrefix + "Failed getting the categories for settings", ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    log.error("Failed", e);
                }
            }
        }
        return categories;
    }
    public String getTenantIdByAzureTenantId(String azureTenantId){
        String tenantId = null;
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "select tenantId from tenants.nms_settings where name = ? and value = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1,"azureTenantId");
            ps.setString(2, azureTenantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tenantId = rs.getString(1);
            }
            log.info("{}Fetched {} tenant id", tracePrefix, tenantId);
        } catch (Exception e) {
            log.error("Could not query tenant id", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    log.error("Failed", e);
                }
            }
        }
        return tenantId;
    }

    public Map<String, Object> getProperties(String category) {
        Map<String, Object> props = new HashMap<>();
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "SELECT name,dataType,value FROM nms_settings where category = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, category);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                props.put(rs.getString(1), convertToDataType(rs.getString(2), rs.getString(3)));
            }
        } catch (Exception ex) {
            log.error("{}Failed fetching settings for category: {}", tracePrefix, category, ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {

                }
            }
        }
        return props;
    }

    public Object getProperty(String category, String name) {
        Object value = null;
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "SELECT name,dataType,value FROM nms_settings where category = ? and name = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, category);
            ps.setString(2, name);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                value = convertToDataType(rs.getString(2), rs.getString(3));
//                props.put(rs.getString(1), convertToDataType(rs.getString(2), rs.getString(3)));
            }
        } catch (Exception ex) {
            log.error("{}Failed fetching settings for category: {}", tracePrefix, category, ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {

                }
            }
        }
        return value;
    }

    public List<SystemSetting> getAllProperties() {
        List<SystemSetting> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "SELECT category,name,dataType,value,aliasName,subCategory,additionalInformation FROM nms_settings";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                SystemSetting setting = new SystemSetting();
                setting.setCategory(rs.getString(1));
                setting.setName(rs.getString(2));
                setting.setDataType(rs.getString(3));
                setting.setValue(rs.getString(4));
                setting.setAliasName(rs.getString(5));
                setting.setSubCategory(rs.getString(6));
                setting.setAdditionalInformation(rs.getString(7));
                list.add(setting);
            }
        } catch (Exception ex) {

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {

                }
            }
        }
        return list;
    }





    public void storeSetting(String category, String name, String dataType, Object value,String subCategory,String additionalInformation)
            throws Exception {
        storeSetting(category,name,dataType,value,null,subCategory,additionalInformation);
    }
    public void storeSetting(String category, String name, String dataType, Object value, String aliasName,String subCategory,String additionalInformation)
            throws Exception {
        Connection conn = null;

        if (dataType == null) {
            if (value.getClass().isEnum()) {
                dataType = value.getClass().getName();
            } else {
                dataType = deriveDataType(value);
            }
        }

        try {
            conn = getConnection();
            StringBuilder sqlBuilder = new StringBuilder("INSERT INTO nms_settings(category,name,dataType,value,subCategory,additionalInformation");
            StringBuilder values = new StringBuilder( " VALUES(?,?,?,?,?,?");
            if(aliasName != null){
                sqlBuilder.append(",aliasName");
                values.append(",?");
            }

            sqlBuilder.append(")");
            values.append(")");
            sqlBuilder.append(values);
            PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString());
            ps.setString(1, category);
            ps.setString(2, name);
            ps.setString(3, dataType);
            ps.setString(4, constructString(value));
            ps.setString(5, subCategory);
            ps.setString(6, additionalInformation);
            int i = -1;
            if(aliasName != null) {
                ps.setString(7, aliasName);
                i = 8;
            }else {
                i = 7;
            }
            ps.executeUpdate();
        } catch (Exception ex) {
            log.error(tracePrefix + "Failed storing the property: " + name + ", Value: " + value, ex);
            throw new Exception("Failed storing the property: " + name + ", Value: " + value);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public void updateSetting(String category, String name, Object value)
            throws Exception {
        updateSetting(category, name, value, null);
    }
    public void updateSetting(String category, String name, Object value, String aliasName)
            throws Exception {
        Connection conn = null;

        try {
            conn = getConnection();
            String sql = "UPDATE nms_settings SET value = ? WHERE category = ? AND name = ?";
            if (aliasName != null) {
                sql = "UPDATE nms_settings SET value = ?, aliasName = ? WHERE category = ? AND name = ?";
            }
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, constructString(value));
            if (aliasName != null) {
                ps.setString(2, aliasName);
                ps.setString(3, category);
                ps.setString(4, name);
            } else {
                ps.setString(2, category);
                ps.setString(3, name);
            }
            ps.executeUpdate();
        } catch (Exception ex) {
            log.error("{}Failed updating the property: {}", tracePrefix, name, ex);
            throw new Exception("Failed updating the property: " + name + ", Value: " + value);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private String constructString(Object value) {
        if ( value == null ) {
            return "";
        }
        if (value.getClass().isEnum()) {
            return ((Enum) value).name();
        }
        return value.toString();
    }

    private String deriveDataType(Object value) {
        if (value instanceof Boolean) {
            return SettingType.BOOLEAN.name();
        } else if (value instanceof Integer) {
            return SettingType.INT.name();
        } else if (value instanceof Long) {
            return SettingType.LONG.name();
        } else if (value instanceof Float) {
            return SettingType.FLOAT.name();
        } else if (value instanceof Double) {
            return SettingType.DOUBLE.name();
        } else if (value instanceof String) {
            return SettingType.STRING.name();
        }
        throw new IllegalArgumentException(
                "Cannot encode dataType for value: " + value + " , Class: " + value.getClass());
    }

    private Object convertToDataType(String dataType, String value) {
        SettingType type = SettingType.getEnum(dataType);
        if (type != null) {
            switch (type) {
                case BOOLEAN:
                    return Boolean.parseBoolean(value);
                case DOUBLE:
                    return Double.parseDouble(value);
                case FLOAT:
                    return Float.parseFloat(value);
                case INT:
                    return NumberUtils.isCreatable(value) ? Integer.parseInt(value) : "";
                case LONG:
                    return NumberUtils.isCreatable(value) ? Long.parseLong(value) : "";
                case STRING:
                    return value;
            }
        }

        Class c = null;
        try {
            c = Class.forName(dataType);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not decode the dataType as a Custom Class: " + dataType);
        }

        if (c.isEnum()) {
            return Enum.valueOf(c, value);
        }
        throw new IllegalArgumentException("Cannot decode the value: " + value + ", as Type: " + dataType);
    }





    public void removeSettings(String category) {
        Connection conn = null;
        try {
            conn = getConnection();
            String sql = "DELETE FROM nms_settings where category = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, category);
            int deletedCount = ps.executeUpdate();
            log.info("{}Deleted {} settings in category: {}", tracePrefix, deletedCount, category);
        } catch (Exception ex) {
            log.error("{}Failed removing settings for category: {}", tracePrefix, category, ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {

                }
            }
        }
    }


}
