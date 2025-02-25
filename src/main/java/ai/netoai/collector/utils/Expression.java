package ai.netoai.collector.utils;

import ai.netoai.collector.snmp.trap.SnmpTrap;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,include = JsonTypeInfo.As.PROPERTY,property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleExpression.class, name="ai.netoai.collector.utils.SimpleExpression"),
        @JsonSubTypes.Type(value = LogicalExpression.class, name="ai.netoai.collector.utils.LogicalExpression")
})

public abstract class Expression implements Serializable {
	
	public static enum Dialect{
		MYSQL, ORACLE, ORIENT;
	}
	
	private Dialect dialect;
    protected boolean orientExp;
    private static final long serialVersionUID = 940607164269146919L;
    protected ArrayList<Integer> excludeDays;

    /**
     * Evaluates expression on the row passed as Map of key value pairs.
     * 
     * @param map
     *            - a row's key value pairs.
     * @return true if the passed row matches this expression.
     */
    public abstract boolean evaluate(Map<Object, Object> map);
    
    public abstract boolean evaluate(SnmpTrap trap);
    /**
     * Evaluates expression on the bean being passed
     * 
     * @param bean
     *            - an object of type 'type' being passed
     * @param type
     *            - type of the object
     * @return true if the passed bean matches this expression.
     */
    public abstract boolean evaluate(Object bean, Class<?> type);

//    public abstract boolean evaluate(MISObject dob);

    public Expression deepClone() {
        return ExpressionUtil.fromJson(ExpressionUtil.toJson(this));
    }

    public boolean isOrientExp() {
        return orientExp;
    }

    public void setOrientExp(boolean orientExp) {
        this.orientExp = orientExp;
    }

	public Dialect getDialect() {
		return dialect;
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}
        
        public ArrayList<Integer> getExcludeDays() {	
	    return excludeDays;	
	}	
		
	public void setExcludeDays(ArrayList<Integer> excludeDays) {	
	    this.excludeDays = excludeDays;	
	}
    
}

