package ai.netoai.collector.utils;

import ai.netoai.collector.snmp.trap.SnmpTrap;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleExpression extends Expression {
    private static final long serialVersionUID = -1282146111546568123L;
    private static final Logger log = LoggerFactory.getLogger(SimpleExpression.class);
    private static final String tracePrefix = "[" + SimpleExpression.class.getSimpleName() + "]: ";
    protected String operand;
    protected String operandType;
    protected RuleOperator operator;
    protected Object value;
    private OperandObjectType operandObjectType = OperandObjectType.NONE;
    private boolean isExclude = false;
    private boolean ignoreCase = false;
    private boolean regExp = false;

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }
    
    public void setExclude(boolean isExclude) {
        this.isExclude = isExclude;
    }

    public boolean isExclude() {
        return this.isExclude;
    }

    public OperandObjectType getOperandObjectType() {
        return operandObjectType;
    }

    public void setOperandObjectType(OperandObjectType operandObjectType) {
        this.operandObjectType = operandObjectType;
    }

    /**
     * 
     */
    public SimpleExpression() {
        super();
    }

    @Override
    public boolean evaluate(SnmpTrap trap) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public enum OperandObjectType {
        CHARTVARIABLE, METRICCONFIGURATION, NONE;

    }

    public SimpleExpression(String operand, String operandType, RuleOperator operator, Object value) {
        this(operand, operandType, operator, value, null, null);
    }

    public SimpleExpression(String operand, String operandType, RuleOperator operator, Object value, String beginChars, String endChars) {
        super();
        this.operand = operand;
        this.operandType = operandType;
        this.operator = operator;
        this.value = value;
    }


    /**
     * @return the operandType
     */
    public String getOperandType() {
        return operandType;
    }

    /**
     * @param operandType
     *            the operandType to set
     */
    public void setOperandType(String operandType) {
        this.operandType = operandType;
    }

    /**
     * @return the operand
     */
    public String getOperand() {
        return operand;
    }

    /**
     * @param operand
     *            the operand to set
     */
    public void setOperand(String operand) {
        this.operand = operand;
    }

    /**
     * @return the operator
     */
    public RuleOperator getOperator() {
        return operator;
    }

    /**
     * @param operator
     *            the operator to set
     */
    public void setOperator(RuleOperator operator) {
        this.operator = operator;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param value
     *            the value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean evaluate(Map<Object, Object> map) {
        boolean result = false;
        Object value = map.get(operand);

        
        if (operator == RuleOperator.MATCHES) {
            if(value == null) {
                return false;
            }
            int index=value.toString().indexOf(this.value.toString());
            if(index == -1) {
            	result = false;
            }else {
            	result = true;
            }
        } else if (operator == RuleOperator.MATCHES_IGNORE_CASE) {
        	 // Making the matches case insensitive
            if(value == null) {
                return false;
            }
            result = value.toString().toLowerCase().matches(".*" + Pattern.quote(this.value.toString().toLowerCase()) + ".*"); 
            
        } else if (operator == RuleOperator.NOT_MATCHES) {
            if(value == null) {
                return false;
            }
            int index=value.toString().indexOf(this.value.toString());
            if(index == -1) {
            	result = true;
            }else {
            	result = false;
            }
        } else if (operator == RuleOperator.NOT_MATCHES_IGNORE_CASE) {
            if(value == null) {
                return false;
            }
            result = !value.toString().toLowerCase().matches(".*" + Pattern.quote(this.value.toString().toLowerCase()) + ".*");
           
        } else if (operator == RuleOperator.IN) {
            if(value == null) {
                return false;
            }
            List<Object> list = (List<Object>) this.value;
            for (Object str : list) {
                if (value instanceof Double && str instanceof Integer) {
                    str = Double.valueOf(str.toString());
                }
                if (value instanceof Comparable && str instanceof Comparable
                            && value.getClass().isAssignableFrom(str.getClass())) {
                    result = ((Comparable) str).compareTo(value) == 0;
                } else {
                    if ( value instanceof String || str instanceof String ) {
                        result = str.toString().equalsIgnoreCase(value.toString());
                    }
                }
                if (result) {
                    break;
                }
            }
        } else if (operator == RuleOperator.STARTS_WITH_IGNORE_CASE) {
            // Making the starts-with case insensitive
            if(value == null) {
                return false;
            }
            String s = value.toString().toLowerCase();
            String exprValue = this.value.toString().toLowerCase();
            if (s.startsWith(exprValue)) {
                result = true;
            }
        } else if (operator == RuleOperator.ENDS_WITH_IGNORE_CASE) {
            if(value == null) {
                return false;
            }
            // Making the ends-with case insensitive
            String s = value.toString().toLowerCase();
            String exprValue = this.value.toString().toLowerCase();
            if (s.endsWith(exprValue)) {
                result = true;
            }
        }else if(operator == RuleOperator.STARTS_WITH){
            if(value == null) {
                return false;
            }
            String s = value.toString();
            String exprValue=this.value.toString();
            if(s.startsWith(exprValue)) {
                result=true;
            }
        }else if(operator == RuleOperator.ENDS_WITH){
            if(value == null) {
                return false;
            }
            String s = value.toString();
            String exprValue=this.value.toString();
            if(s.endsWith(exprValue)) {
                result=true;
            }
        } else if (operator == RuleOperator.NOT_NULL) {
              result = value ==  null ? false:true;
        }
        else if(operator == RuleOperator.IS_NULL) {
            if(value == null) {
                return true;
            }
        }else if(operator == RuleOperator.EQUALS_IGNORE_CASE) {
            if (value == null) {
                return false;
            }
            String s = value.toString();
            String exprValue = this.value.toString();
            if (s.toLowerCase().equals(exprValue.toLowerCase())) {
                result = true;
            }
        }else if(operator == RuleOperator.NOT_EQUAL_IGNORE_CASE) {
        if (value == null) {
            return false;
        }
        String s = value.toString();
        String exprValue = this.value.toString();
        if (!s.toLowerCase().equals(exprValue.toLowerCase())) {
            result = true;
        }
    } else if(operator == RuleOperator.RLIKE){
            if (value == null) {
                return false;
            }
            return regexMatch(this.value.toString(), value.toString());
        } else if(operator == RuleOperator.NOT_RLIKE){
            if (value == null) {
                return false;
            }
            return !regexMatch(this.value.toString(), value.toString());
        } else if(operator == RuleOperator.NOT_EMPTY) {
            if (value == null) {
                return false;
            }
            return  StringUtils.isNotEmpty(value.toString());
        } else if(operator == RuleOperator.IS_EMPTY) {
            if (value == null) {
                return false;
            }
            return StringUtils.isEmpty(value.toString());
        } else if(operator == RuleOperator.BETWEEN) {
            if (value == null) {
                return false;
            }
            if (this.value instanceof String) {
            }
            if ((this.operandType.toLowerCase().equalsIgnoreCase("timestamp") || this.operandType.toLowerCase().equalsIgnoreCase("date"))
                    && !StringUtils.isNumeric(value.toString())) {
                Timestamp ts = Timestamp.valueOf(value.toString());
                value = ts.getTime();
            }
            List<String> list = (List<String>) this.value;
            if (value instanceof Comparable && list.get(0) instanceof Comparable && list.get(1) instanceof Comparable
                    && NumberUtils.isNumber(value.toString()) && NumberUtils.isNumber(list.get(0)) && NumberUtils.isNumber(list.get(1))) {
                Double d1 = Double.valueOf(list.get(0));
                Double d2 = Double.valueOf(list.get(1));
                Double d3 = Double.valueOf(value.toString());
                return ((Comparable) d3).compareTo(d1) >= 0 && ((Comparable) d3).compareTo(d2) <= 0 ;
            }
        }
        else {
            RuleOperator ruleoperator = RuleOperator.valueOf(operator.name().toUpperCase());
            if (this.operandType.toLowerCase().equalsIgnoreCase("timestamp") || this.operandType.toLowerCase().equalsIgnoreCase("date")) {
                if (value == null) {
                    return false;
                }
                if (!StringUtils.isNumeric(value.toString())) {
                    Timestamp ts = Timestamp.valueOf(value.toString());
                    value = ts.getTime();
                }
            }
            result = CompareUtil.compare(ruleoperator, value, this.value);
        }
        return result;
    }


    private boolean regexMatch(String regex, String strToMatch) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(strToMatch);
        boolean match = matcher.find();
        return match;
    }



    public boolean isRegExp() {
        return regExp;
    }

    public void setRegExp(boolean regExp) {
        this.regExp = regExp;
    }

    public String toString() {
        return ExpressionUtil.toJson(this);
    }

	@Override
	public boolean evaluate(Object bean, Class<?> type) {
		// TODO Auto-generated method stub
		return false;
	}

}