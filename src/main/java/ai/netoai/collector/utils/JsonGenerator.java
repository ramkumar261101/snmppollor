package ai.netoai.collector.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class JsonGenerator implements Serializable {

    private static final long serialVersionUID = -2801250544388298586L;
    private static final Logger log = LoggerFactory.getLogger(JsonGenerator.class);
    private static final String tracePrefix = "[" + JsonGenerator.class.getSimpleName() + "]: ";

    public static String getJSONString(Object obj) {
        return getJSONString(obj, true);
    }

    public static String getJSONStringDates(Object obj) {
        return getJSONStringRetainDates(obj, true);
    }

    public static String getJSONString(Object obj, boolean indent) {
        return getJSONString(obj, indent, false);
    }

    public static String getJSONString(Object obj, boolean indent, boolean includeNulls) {
        ObjectMapper mapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
//        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        if ( !includeNulls ) {
            // Do not include null values when include nulls is false.
            mapper.setSerializationInclusion(Include.NON_NULL);
        }
        if (indent) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, obj);
        } catch (JsonGenerationException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        } catch (JsonMappingException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        } catch (IOException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        }
        return writer.toString();
    }

    public static String getJSONStringMethodAccessor(Object obj, boolean indent, boolean includeNulls) {
        ObjectMapper mapper = new ObjectMapper();
//        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        if ( !includeNulls ) {
            // Do not include null values when include nulls is false.
            mapper.setSerializationInclusion(Include.NON_NULL);
        }
        if (indent) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, obj);
        } catch (IOException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        }
        return writer.toString();
    }

    public static String getJSONStringDateFormat(Object obj, boolean indent, String dateFormat) {
        ObjectMapper mapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setDateFormat(new SimpleDateFormat(dateFormat));
        mapper.setSerializationInclusion(Include.NON_NULL);
        if (indent) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, obj);
        } catch (JsonGenerationException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        } catch (JsonMappingException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        } catch (IOException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        }
        return writer.toString();
    }

    public static String getJSONStringRetainDates(Object obj, boolean indent) {
        ObjectMapper mapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setSerializationInclusion(Include.NON_NULL);
        if (indent) {
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, obj);
        } catch (JsonGenerationException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        } catch (JsonMappingException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        } catch (IOException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        }
        return writer.toString();
    }
    
    public static String getJSONStringIgnoreEmptyArray(Object obj) {
        ObjectMapper mapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        StringWriter writer = new StringWriter();
        try {
            mapper.writeValue(writer, obj);
        } catch (JsonGenerationException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        } catch (JsonMappingException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        } catch (IOException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to StringWriter", e);
        }
        return writer.toString();
    }

    public static void writeJson(Object obj, PrintWriter printWriter, boolean epochConversion) {
        ObjectMapper mapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        if (epochConversion) {
            mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        }
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(Include.NON_NULL);
        SimpleModule dateModule = new SimpleModule();
        dateModule.addSerializer(new StdSerializer<java.sql.Date>(java.sql.Date.class) {
            @Override
            public void serialize(java.sql.Date date, com.fasterxml.jackson.core.JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                if (date == null){
                    jsonGenerator.writeNull();
                }else{
                    jsonGenerator.writeNumber(date.getTime());
                }
            }
        });
        dateModule.addSerializer(new StdSerializer<Timestamp>(Timestamp.class) {
            @Override
            public void serialize(Timestamp timestamp, com.fasterxml.jackson.core.JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
                if (timestamp == null){
                    jsonGenerator.writeNull();
                }else{
                    jsonGenerator.writeNumber(timestamp.getTime());
                }
            }
        });
        mapper.registerModule(dateModule);
        try {
            mapper.writeValue(printWriter, obj);
        } catch (JsonGenerationException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to PrintWriter", e);
        } catch (JsonMappingException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to PrintWriter", e);
        } catch (IOException e) {
            log.error(tracePrefix + "Failed writing Object as JSON to PrintWriter", e);
        } catch (Exception e) {
            log.error(tracePrefix + "Failed writing Object as JSON to PrintWriter", e);
        }
    }

    public static void writeJson(Object obj, PrintWriter printWriter) {
        writeJson(obj, printWriter, false);
    }

//    public static MISObject fromJSONString(String jsonStr) {
//        Gson gson = new Gson();
//        MISObject dob = gson.fromJson(jsonStr, MISObject.class);
//        return dob;
//    }

    public static HashMap<String, List<Map<String, Object>>> parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            HashMap<String, List<Map<String, Object>>> readValue = mapper.readValue(jsonStr, HashMap.class);
            for (Map.Entry<String, List<Map<String, Object>>> e : readValue.entrySet()) {
                log.info(tracePrefix + "Key Level1: " + e.getKey());
                for (Map<String, Object> map : e.getValue()) {
                    System.out.println("======================================");
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        System.out.println(entry.getKey() + " => " + entry.getValue());
                    }
                    System.out.println("======================================");
                }
            }
            return readValue;
        } catch (IOException e) {
        	log.error(tracePrefix+" Error found ",e);
        }
        return null;
    }

    public static Map<String, String> parseJsonAsMap(String jsonStr) {
        Gson gson = new Gson();
        Map<String, String> fromJson = gson.fromJson(jsonStr, Map.class);
        return fromJson;
    }

    public static <T> T parseJsonToObject(String json, Class type) {
        ObjectMapper mapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Date.class, new DateHandler());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(Include.NON_NULL);
        T object = null;
        try {
            object = (T) mapper.readValue(json, type);
        } catch (IOException e) {
            log.error(tracePrefix + "Error while parsing json string", e);
            throw new RuntimeException("Couldn't parse the input Json string :" + json);
        }
        return object;
    }

    public static <T> T parseJsonToObject(final TypeReference<T> type, final String json) {
        ObjectMapper mapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Date.class, new DateHandler());
        mapper.registerModule(module);
        mapper.setSerializationInclusion(Include.NON_NULL);
//        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        T data = null;
        try {
            log.debug(tracePrefix + "json : " + json);
            data = mapper.readValue(json, type);
        } catch (Exception e) {
            log.error(tracePrefix + "Error while parsing json string", e);
            throw new RuntimeException("Couldn't parse the input Json string :" + json);
        }
        return data;
    }

    public static <T> T parseJsonToObjectsRetainDates(final TypeReference<T> type, final String json) {
        ObjectMapper mapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        T data = null;
        try {
            data = mapper.readValue(json, type);
        } catch (Exception e) {
            log.error(tracePrefix + "Error while parsing json string", e);
            throw new RuntimeException("Couldn't parse the input Json string :" + json);
        }
        return data;
    }
    
    public static List<?> parseJsonAsList(String jsonStr, Class type){
    	Gson gson = new Gson();
    	List<?> itemsList = (List<?>) gson.fromJson(jsonStr, type);
    	return itemsList;
    }
    
    public static class DateHandler extends StdDeserializer<Date> {
        
        public DateHandler() {
            this(null);
        }
        
        public DateHandler(Class clazz) {
            super(clazz);
        }
        
        @Override
        public Date deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            String data = jp.getText();
            if (data != null && NumberUtils.isParsable(data)) {
                long value = new Double(data).longValue();
                return value > 0 ? new Date(value) : null;
            }
            return null;
        }
        
    }

     public static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Base64.getDecoder().decode(json.getAsString());
        }

        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
        }
    }
    

}
