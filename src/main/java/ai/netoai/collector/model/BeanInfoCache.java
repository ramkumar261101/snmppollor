package ai.netoai.collector.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BeanInfoCache {
    private static final Logger log = LoggerFactory
            .getLogger(BeanInfoCache.class);
    private static final String tracePrefix = "["
            + BeanInfoCache.class.getSimpleName() + "]: ";

    private static final Map<Class, NmsBeanInfo> cache = new HashMap<>();

    public static PropertyDescriptor[] getPropertyDescriptors(
            Class<? extends GenericJavaBean> bean) {

        NmsBeanInfo info = cache.get(bean);

        if (info == null) {
            //log.info(tracePrefix + "--------- Not present in cache");
            info = introspectAndCache(bean);
        } else {
            //log.info(tracePrefix + "+++++++++++ Present in cache");
        }

        return info.getPropDescs();

    }

    public static Map<String, Annotation[]> getSearchableFields(
            Class<? extends GenericJavaBean> class1) {

        NmsBeanInfo info = cache.get(class1);

        if (info == null) {
            //log.info(tracePrefix + "--------- Not present in cache");
            info = introspectAndCache(class1);
        } else {
            //log.info(tracePrefix + "+++++++++++ Present in cache");
        }

        return info.getSearchableFields();

    }

    public static PropertyDescriptor getPropertyDescriptor(
            Class<? extends GenericJavaBean> bean, String propName) {

        NmsBeanInfo info = cache.get(bean);

        if (info == null) {
            //log.info(tracePrefix + "--------- Not present in cache");
            info = introspectAndCache(bean);
        } else {
            //log.info(tracePrefix + "+++++++++++ Present in cache");
        }

        return info.getPropDescMap().get(propName);

    }

    private static NmsBeanInfo introspectAndCache(Class<? extends GenericJavaBean> bean) {
        NmsBeanInfo info = new NmsBeanInfo();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(bean, GenericJavaBean.class);
            info.setPropDescs(beanInfo.getPropertyDescriptors());
            for (PropertyDescriptor pd : info.getPropDescs()) {
                info.putInPropDesc(pd.getName(), pd);

                Method readMethod = pd.getReadMethod();
                if (readMethod != null) {
                    if (readMethod.getAnnotation(Searchable.class) != null) {
                        info.addSearchableField(pd.getName(), readMethod.getAnnotations());
                    }
                }
            }
        } catch (IntrospectionException e) {
            // TODO Auto-generated catch block
            log.error("Failed", e);
        }

        cache.put(bean, info);
        return info;
    }
    
    public static Map<String, String> searchableFieldAliases(Class<? extends GenericJavaBean> bean) {
    	NmsBeanInfo info = cache.get(bean) == null ? introspectAndCache(bean) : cache.get(bean);
    	return info.searchableFieldAliases();
    }
    
//    public static Map<String, String> searchableFieldAliases(String className) {
//    	return searchableFieldAliases(Node.nodeClassFor(className));
//    }
    
    public static Map<String, String> identifyingAttribs(Class<? extends GenericJavaBean> bean){
    	NmsBeanInfo info = cache.get(bean) == null ? introspectAndCache(bean) : cache.get(bean);
    	return info.getIdentifyingAttribs();
    }


}

class NmsBeanInfo {

    private PropertyDescriptor[] propDescs;
    private Map<String, PropertyDescriptor> propDescMap = new HashMap<>();
    private Map<String, Annotation[]> searchableFields = new HashMap<>();

    /**
     * @return the propDescs
     */
    public PropertyDescriptor[] getPropDescs() {
        return propDescs;
    }

    /**
     * @param propDescs the propDescs to set
     */
    public void setPropDescs(PropertyDescriptor[] propDescs) {
        this.propDescs = propDescs;
    }

    /**
     * @return the propDescMap
     */
    public Map<String, PropertyDescriptor> getPropDescMap() {
        return propDescMap;
    }

    /**
     * @param propDescMap the propDescMap to set
     */
    public void setPropDescMap(Map<String, PropertyDescriptor> propDescMap) {
        this.propDescMap = propDescMap;
    }

    public void putInPropDesc(String propName, PropertyDescriptor propDesc) {
        this.propDescMap.put(propName, propDesc);
    }

    public void addSearchableField(String field, Annotation[] annotations) {
        this.searchableFields.put(field, annotations);
    }

    public Map<String, Annotation[]> getSearchableFields() {
        return searchableFields;
    }
    
    public Map<String, String> searchableFieldAliases() {
    	Map<String, String> aliases = new HashMap<>();
    	searchableFields.forEach((field, annotations) -> {
    		for (Annotation annotation : annotations) {
				if (annotation.annotationType().equals(Searchable.class)){
					aliases.put(field, ((Searchable)annotation).alias() );
				}
			}
    	});
        return aliases;
    }
    
    public Map<String, String> getIdentifyingAttribs(){
    	Map<String, String> aliases = searchableFieldAliases();
    	Map<String, String> map = new HashMap<>();
    	searchableFields.forEach((field, annotations) -> {
    		for (Annotation annotation : annotations) {
				if (annotation.annotationType().equals(Id.class)){
					map.put(field,  aliases.get(field));
				}
			}
    	});
        return map;
    }


}
