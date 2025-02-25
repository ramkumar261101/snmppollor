package ai.netoai.collector.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public abstract class GenericJavaBean implements Serializable {
    private static final Logger log = LoggerFactory
            .getLogger(GenericJavaBean.class);
    private static final String tracePrefix = "["
            + GenericJavaBean.class.getSimpleName() + "]: ";
    public static final String BEAN_FIELD = "beanType";
    private transient Set<String> profileValuesSetted = new HashSet<String>();

    public enum BeanType {
        NETWORKELEMENT, ENDPOINT, EVENT_ACTIVE, EVENT_ARCHIVE, METRIC_TBR_DATA, METRIC_CURRENT_DATA, PHYSICALLINK, GROUP_LINK
    }

    private BeanType beanType;

    public void setBeanType(BeanType beanType) {
        this.beanType = beanType;
    }

    public Object getBeanType() {
        return beanType;
    }

    @JsonIgnore
    public Map<String, PropertyDescriptor> getAllPropDescriptors() {
        Map<String, PropertyDescriptor> map = new HashMap<>();
        PropertyDescriptor[] pds = BeanInfoCache.getPropertyDescriptors(this.getClass());
        for (PropertyDescriptor pd : pds) {
            Transient trans = pd.getReadMethod().getAnnotation(Transient.class);
            if (!pd.getName().equals(BEAN_FIELD) && trans == null) {
                map.put(pd.getName(), pd);
            }
        }
        return map;
    }

    @JsonIgnore
    public Map<String, String> getPropertiesForOrient() {
        Map<String, String> propsMap = new HashMap<>();
        Map<String, PropertyDescriptor> pds = getAllPropDescriptors();
        if ( pds != null && !pds.isEmpty() ) {
            for(Map.Entry<String, PropertyDescriptor> e : pds.entrySet()) {
                PropertyDescriptor pd = e.getValue();
                String propName = pd.getName();
                String propType = getTypeForOrient(pd.getPropertyType());
                propsMap.put(propName, propType);
            }
        }
        return propsMap;
    }

    private String getTypeForOrient(Class clazz) {
        if ( clazz.getSimpleName().equalsIgnoreCase("string") || clazz.isEnum() ) {
            return "STRING";
        } else if ( clazz.getSimpleName().contains("int") || clazz.getSimpleName().contains("Int") ) {
            return "INTEGER";
        } else if ( clazz.getSimpleName().contains("long") || clazz.getSimpleName().contains("Long") ) {
            return "LONG";
        } else if ( clazz.getSimpleName().contains("double") || clazz.getSimpleName().contains("Double") ) {
            return "DOUBLE";
        } else if ( clazz.getSimpleName().contains("float") || clazz.getSimpleName().contains("Float") ) {
            return "FLOAT";
        } else if ( clazz.getSimpleName().equalsIgnoreCase("date") || clazz.getSimpleName().equalsIgnoreCase("timestamp") ) {
            return "DateTime";
        } else if ( clazz.getSimpleName().toLowerCase().contains("map") ) {
            return "EMBEDDEDMAP";
        } else if ( clazz.getSimpleName().contains("boolean") || clazz.getSimpleName().contains("Boolean") ) {
            return "BOOLEAN";
        } else if ( clazz.getSimpleName().contains("list") || clazz.getSimpleName().contains("List") ) {
            return "EMBEDDEDLIST";
        } else if ( clazz.getSimpleName().equalsIgnoreCase("byte[]")) {
            return "BINARY";
        }
        log.warn(tracePrefix + "OrientType not detected for Type: [" + clazz.getSimpleName() + "], treating it as String ...");
        return "STRING";
    }

    @JsonIgnore
    public Map<String, Annotation[]> getSearchableFields() {
        return BeanInfoCache.getSearchableFields(this.getClass());
    }

    @JsonIgnore
    public HashMap<String, Object> getAllProperties() {
        return getAllProperties(false);
    }

    @JsonIgnore
    public HashMap<String, Object> getAllProperties(boolean ignore) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        PropertyDescriptor[] descriptors = BeanInfoCache
                .getPropertyDescriptors(this.getClass());

        for (PropertyDescriptor d : descriptors) {
            if (ignore) {
                Ignore ign = d.getReadMethod().getAnnotation(Ignore.class);
                if (ign != null) continue;
            }
            String name = d.getName();
            Object value = null;
            try {

                Method m = d.getReadMethod();

                if (m != null)
                    value = m.invoke(this, new Object[0]);
            } catch (Exception x) {
                log.error("Failed", x);
            }

            if (value != null)
                map.put(name, value);
        }

        return (map);
    }

    public void setAllProperties(Map<String, Object> properties) {
        properties.forEach((key, value) -> {
            PropertyDescriptor pd = BeanInfoCache.getPropertyDescriptor(this.getClass(), key);

            if (pd != null && pd.getWriteMethod() != null) {
                try {
                    pd.getWriteMethod().invoke(this, new Object[]{value});
                } catch (Exception e) {
                    log.info(tracePrefix + "Failed setting the property: " + key, e);
                }
            }
        });
    }

    public static GenericJavaBean fromBytes(byte[] bytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            Object o = in.readObject();
            return (GenericJavaBean) o;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error("Failed", e);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            log.error("Failed", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return null;
    }

    public static byte[] toBytes(GenericJavaBean bean) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(bean);
            out.flush();
            byte[] bytes = bos.toByteArray();
            return bytes;
        } catch (Exception ex) {

        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return null;
    }

    public void setProperty(String key, Object value) {
        PropertyDescriptor pd = BeanInfoCache.getPropertyDescriptor(this.getClass(), key);
        if (pd != null && pd.getWriteMethod() != null) {
            try {
                if (pd.getPropertyType().isEnum()) {
                    Class clazz = pd.getPropertyType();
                    if (value != null) {
                        pd.getWriteMethod().invoke(this, Enum.valueOf(clazz, value.toString()));
                    }
                } else {
                    pd.getWriteMethod().invoke(this, new Object[]{value});
                }
                profileValuesSetted.add(key);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                log.error(tracePrefix + "Failed setting property: " + key + ", Value: " + value + ", ValueType: " + (value != null ? value.getClass() : "NULL"));
                log.error("Failed", e);
                throw new IllegalArgumentException(e);
            }
        }
    }

    public void logAllProperties(List<GenericJavaBean> beansList) {
        for (GenericJavaBean bean : beansList) {
            Field[] fields = bean.getClass().getDeclaredFields();
            for (Field field : fields) {
                String fieldName = field.getName();
                PropertyDescriptor pd = BeanInfoCache.getPropertyDescriptor(this.getClass(), fieldName);
                if (pd != null && pd.getReadMethod() != null) {
                    try {
                        Object value = pd.getReadMethod().invoke(this);
                        Class returnType = pd.getReadMethod().getReturnType();
                        log.info(tracePrefix + " Value for Field '" + fieldName + "' is - " + value + " and DataType is - " + returnType.getSimpleName());
                    } catch (Exception e) {
                        log.error("Failed", e);
                    }
                }
            }
            System.out.println(" ");
        }
    }

    public void logAllProperties() {
        PropertyDescriptor[] descriptors = BeanInfoCache.getPropertyDescriptors(this.getClass());
        for (PropertyDescriptor pd : descriptors) {
            if (pd != null && pd.getReadMethod() != null) {
                try {
                    String fieldName = pd.getName();
                    Object value = pd.getReadMethod().invoke(this);
                    Class returnType = pd.getReadMethod().getReturnType();
                    log.info(tracePrefix + " Value for Field '" + fieldName + "' is - " + value + " and DataType is - " + returnType.getSimpleName());
                } catch (Exception e) {
                    log.error("Failed", e);
                }
            }
        }
    }

    @JsonIgnore
    public Set getProfileValuesSet() {
        return profileValuesSetted;
    }

    public void clearProfileValuesSet() {
        profileValuesSetted.clear();
    }

    @JsonIgnore
    public static String toJson(GenericJavaBean bean) {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        try {
            return gson.toJson(bean);
        } catch (Exception ex) {
            log.error(tracePrefix + "Failed serializing " + bean + " to JSON", ex);
        }
        return null;
    }

    @JsonIgnore
    public static ConfigMessage fromJson(String json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().registerTypeAdapter(Serializable.class, new SerializableAdapter()).create();
        try {
            ConfigMessage bean = gson.fromJson(json, ConfigMessage.class);
            return bean;
        } catch (Exception ex) {
            log.error(tracePrefix + "Failed de-serializing " + json + " to bean", ex);
        }
        return null;
    }

}

class SerializableAdapter implements JsonDeserializer {

    @Override
    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        System.out.println(">>>> Json: " + json);
        if ( json.isJsonObject() ) {
            JsonObject jobj = json.getAsJsonObject();
            if ( jobj.has("ipRange") ) {
                // If the object has ipRange then this is DiscoveryTask
                return context.deserialize(json, DiscoveryTask.class);
            }
        }
        return null;
    }
}
