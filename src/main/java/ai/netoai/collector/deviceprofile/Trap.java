package ai.netoai.collector.deviceprofile;

import ai.netoai.collector.utils.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class Trap {

	private String id;
	private String condition;
	private Class beanType;
	private Map<String, Object> properties = new HashMap<>();
	
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	/**
	 * @return the condition
	 */
	public String getCondition() {
		return condition;
	}
	/**
	 * @param condition the condition to set
	 */
	public void setCondition(String condition) {
		this.condition = condition;
	}
	/**
	 * @return the beanType
	 */
	public Class getBeanType() {
		return beanType;
	}
	/**
	 * @param beanType the beanType to set
	 */
	public void setBeanType(Class beanType) {
		this.beanType = beanType;
	}
	/**
	 * @return the properties
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}
	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}
        
        private Expression getConditionExpr(JsonElement jsonElement, LogicalOperator operator) {
            Expression expr = null;
            String operan = null;
            if( jsonElement.isJsonArray() && operator != null ) {
                LogicalExpression logExpr = new LogicalExpression(operator);
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                jsonArray.forEach(action -> {
                    logExpr.addExpression(getConditionExpr(action, null));
                });
                return logExpr;
            } else {
                JsonObject jsonObj = jsonElement.getAsJsonObject();
                if(jsonObj.entrySet().size() == 0) return null;
                if(jsonObj.entrySet().size() > 1) {
                    JsonArray jsonArray = new JsonArray();
                    for(Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.add(entry.getKey(), entry.getValue());
                        jsonArray.add(jsonObject);
                    }
                    return getConditionExpr(jsonArray, LogicalOperator.AND);
                }
                Map.Entry<String, JsonElement> entry  = jsonObj.entrySet().iterator().next();
                LogicalOperator oper = null;
                for ( LogicalOperator dOper : LogicalOperator.values() ) {
                    if(dOper.toString().equalsIgnoreCase(entry.getKey())) {
                        oper = LogicalOperator.valueOf(entry.getKey().toUpperCase()); 
                        break;
                    }
                }
                if ( oper != null ) {
                    return getConditionExpr(entry.getValue(), oper);
                }
                Object val = entry.getValue().getAsString();
                String operandType = "String";
                RuleOperator ruleOper = RuleOperator.EQUALS;
                if ( val != null && entry.getKey().toLowerCase().contains("oid") ) {
//                    val = new OID(entry.getValue().getAsString());
                      val = entry.getValue().getAsString();
                    ruleOper = RuleOperator.MATCHES;
                } else if ( val != null && isNumeric(entry.getValue().getAsString()) ) {
                    operandType = "Integer";
                    val = entry.getValue().getAsInt();
                }
                return new SimpleExpression(entry.getKey(), operandType, ruleOper, val);
            }
        }
        
        public Expression getConditionExpr() {
            Expression expr = null;
            if ( getCondition() != null && !getCondition().isEmpty() ) {
                JsonElement jsonElmnt = new Gson().fromJson(getCondition(), JsonElement.class);
                return getConditionExpr(jsonElmnt, null);
            }
            return expr;
        }
	
        private boolean isNumeric(String str) {  
              try {  
                double d = Double.parseDouble(str);  
              } catch ( NumberFormatException nfe ) {  
                return false;  
              }  
              return true;  
        }
	
}
