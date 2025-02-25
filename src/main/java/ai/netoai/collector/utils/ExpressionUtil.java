package ai.netoai.collector.utils;


import com.google.common.collect.Lists;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ai.netoai.collector.utils.RuleOperator.*;

/**
 * ExpressionUtil.java
 * 
 */
public class ExpressionUtil {

    protected static final Logger log = LoggerFactory.getLogger(ExpressionUtil.class);
    protected static final String tracePrefix = "[" + ExpressionUtil.class.getSimpleName() + "]: ";
    public static String toJson(Expression obj) {
        if (obj == null) {
            return null;
        }
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        Gson gson = gsonBuilder.create();
        return gson.toJson(obj);
    }

    public static Expression fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        Expression exp = null;
        Gson gson = new GsonBuilder().create();
        JsonElement elem = new JsonParser().parse(json);
        JsonObject topObj = elem.getAsJsonObject();
        JsonElement logicalElem = topObj.get("logicalOperator");
        if (logicalElem == null) {
            // This should be a SimpleExpression
            exp = parseSimpleExpression(topObj, gson);
        } else {
            // This is a Logical expression
            exp = parseLogicalExpression(topObj, gson);
        }
        return exp;
    }

    /**
     * @param topObj
     * @param gson
     * @return
     */
    private static Expression parseLogicalExpression(JsonObject topObj, Gson gson) {
        JsonElement logicalElem = topObj.get("logicalOperator");
        JsonElement orientExp = topObj.get("orientExp");
        JsonElement excludeDays = topObj.has("excludeDays") ? topObj.get("excludeDays") : null;
        LogicalOperator op = LogicalOperator.valueOf(logicalElem.getAsString());
        LogicalExpression exp = new LogicalExpression(op);
		if (orientExp != null) {
			exp.setOrientExp(orientExp.getAsBoolean());
		}
        JsonElement elem = topObj.get("expressions");
        JsonArray arr = elem.getAsJsonArray();
        for (int i = 0; i < arr.size(); i++) {
            JsonElement je = arr.get(i);
            JsonObject obj = je.getAsJsonObject();
            JsonElement je2 = obj.get("logicalOperator");
            Expression childExpr = null;
            if (je2 == null) {
                childExpr = parseSimpleExpression(obj, gson);
            } else {
                childExpr = parseLogicalExpression(obj, gson);
            }
            exp.addExpression(childExpr);
        }
        if (excludeDays != null) {
            exp.setExcludeDays(new Gson().fromJson(excludeDays, new TypeToken<List<Integer>>() {
            }.getType()));
        }
        return exp;
    }

    public static Expression fromJsonExpr(String json, LogicalExpression le) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        Expression exp = null;
        Gson gson = new GsonBuilder().create();
        JsonElement elem = new JsonParser().parse(json);
        JsonObject topObj = elem.getAsJsonObject();
        JsonElement logicalElem = topObj.get("logicalOperator");
        if (logicalElem == null) {
            // This should be a SimpleExpression
            exp = parseSimpleExpression(topObj, gson);
        } else {
            // This is a Logical expression
            exp = replaceExprs(topObj, gson, le);
        }
        return exp;
    }

    /**
     * @param topObj
     * @param gson
     * @return
     */
    private static Expression replaceExprs(JsonObject topObj, Gson gson, LogicalExpression newExpr) {
        JsonElement logicalElem = topObj.get("logicalOperator");
        JsonElement orientExp = topObj.get("orientExp");
        JsonElement excludeDays = topObj.has("excludeDays") ? topObj.get("excludeDays") : null;
        LogicalOperator op = LogicalOperator.valueOf(logicalElem.getAsString());
        LogicalExpression exp = new LogicalExpression(op);
        if (orientExp != null) {
            exp.setOrientExp(orientExp.getAsBoolean());
        }
        JsonElement elem = topObj.get("expressions");
        JsonArray arr = elem.getAsJsonArray();
        for (int i = 0; i < arr.size(); i++) {
            JsonElement je = arr.get(i);
            JsonObject obj = je.getAsJsonObject();
            JsonElement je2 = obj.get("logicalOperator");
            Expression childExpr = null;
            if (je2 == null) {
                if (obj.get("value") != null &&
                        obj.get("value").toString().trim().contains("$current_user_empgrps")) {
                    childExpr = newExpr;
                } else {
                    childExpr = parseSimpleExpression(obj, gson);
                }
            } else {
                childExpr = replaceExprs(obj, gson, newExpr);
            }
            exp.addExpression(childExpr);
        }
        if (excludeDays != null) {
            exp.setExcludeDays(new Gson().fromJson(excludeDays, new TypeToken<List<Integer>>() {
            }.getType()));
        }
        return exp;
    }

    /**
     * @param topObj
     */
    private static Expression parseSimpleExpression(JsonObject topObj, Gson gson) {
        Expression ex = gson.fromJson(topObj.toString(), SimpleExpression.class);
        JsonElement excludeDays = topObj.has("excludeDays") ? topObj.get("excludeDays") : null;
        SimpleExpression exp = (SimpleExpression) ex;
        if (exp.getOperator() == null && topObj.get("operator") != null) {
            exp.setOperator(RuleOperator.asEnum(topObj.get("operator").toString()));
        } else if (exp.getOperator() == null) {
            exp.setOperator(null);
        }
        if (excludeDays != null) {
            exp.setExcludeDays(new Gson().fromJson(excludeDays, new TypeToken<List<Integer>>() {
            }.getType()));
        }
        if (exp.getOperator().equals(IN) && topObj.get("value").isJsonArray()) {
            exp.setValue(JsonGenerator.parseJsonToObject(topObj.get("value").toString(), Object.class));
        }
        return ex;
    }

    public static boolean evaluate(Expression exp, Map<Object, Object> map) {
        return exp.evaluate(map);
    }


    public static SimpleExpression newSimpleExpression(String operandName, String type, RuleOperator operator,
            Object value) {
        return new SimpleExpression(operandName, type, operator, value);
    }
    

    
    public static Expression generateBooleanFilters(Expression exp) {
        LogicalExpression logExp = new LogicalExpression(LogicalOperator.OR);
        if (exp instanceof SimpleExpression) {
            if (!((SimpleExpression) exp).getOperandType().equalsIgnoreCase("boolean")) {
                return exp;
            }
            if ( !((SimpleExpression) exp).getOperand().equalsIgnoreCase("productive") ) {
                return exp;
            }
            logExp.addExpression(exp);
            SimpleExpression expression = (SimpleExpression) ExpressionUtil.fromJson(ExpressionUtil.toJson(exp));
            boolean value = Boolean.parseBoolean(expression.getValue() != null ? expression.getValue().toString() : "false");
            expression.setOperandType("String");
            expression.setValue(value ? "T" : "F");
            if ( expression.getOperator().name().equalsIgnoreCase(RuleOperator.NOT_EQUAL.name()) ) {
                // Changing the logical operator in case if the boolean simple operator is !=
                logExp.setLogicalOperator(LogicalOperator.AND);
            }
            logExp.addExpression(expression);
        } else if (exp instanceof LogicalExpression) {
            logExp.setLogicalOperator(((LogicalExpression) exp).getLogicalOperator());
            for(Expression e : ((LogicalExpression) exp).getExpressions()) {
                logExp.addExpression(generateBooleanFilters(e));
            }
        }
        return logExp;
    }

    private static SimpleExpression convertToHumanReadable(SimpleExpression exp) {
        log.info("{}Converting to human readable format, Op: {}, OpType: {}, Operator: {}, Vaue: {}", tracePrefix, exp.getOperand(), exp.getOperandType(), exp.getOperator(), exp.getValue());
        if ( exp.getOperandType().equalsIgnoreCase("Date")
                || exp.getOperandType().equalsIgnoreCase("Timestamp") ) {
            Object valObj = exp.getValue();
            Object newVal = null;
            if ( valObj instanceof List ) {
                List objList = (List) valObj;
                if ( objList != null && objList.size() > 1 ) {
                    long start = Long.parseLong(objList.get(0).toString());
                    long end = Long.parseLong(objList.get(1).toString());
                    List<String> valList = new ArrayList<>();
                    valList.add(new Date(start).toString());
                    valList.add(new Date(end).toString());
                    newVal = valList;
                    exp.setOperandType("String");
                    exp.setValue(newVal);
                }
            } else if ( valObj instanceof String ) {
                boolean converted = false;
                if ( !converted && NumberUtils.isCreatable(valObj.toString()) ) {
                    long val = Long.parseLong(valObj.toString());
                    exp.setOperandType("String");
                    exp.setValue(new Date(val).toString());
                }
            } else if ( valObj instanceof Long ) {
                exp.setOperandType("String");
                exp.setValue(new Date((Long)valObj).toString());
            }
        }
        return exp;
    }

    public static Set<String> getColNamesInExpression(String exp) {
        Set<String> list = new HashSet<>();
        if (exp == null || exp.trim().isEmpty() || exp.trim().equals("{}")) {
            return list;
        }
        Expression expr = ExpressionUtil.fromJson(exp);
        if (expr != null) {
            list.addAll(getOperandNames(expr));
        }
        return list;
    }

    /*
     * Gives back column names present in expression values. E.g. for `colA` =
     * `colB`, `colB` will be returned
     */
    public static Set<String> getColNamesInExpressionVals(String exp) {
        Set<String> list = new HashSet<>();
        if (exp == null || exp.trim().isEmpty() || exp.trim().equals("{}")) {
            return list;
        }
        Expression expr = ExpressionUtil.fromJson(exp);
        if (expr != null) {
            list.addAll(getColumnsFromValues(expr));
        }
        return list;
    }

    /*
     * Gives back all the column names present in the specified expression (and
     * its sub-expressions). The column names can be present either as operand
     * or expr values.
     */
    public static Set<String> getAllColsInOperandsVals(String exp) {
        Set<String> list = new HashSet<>();
        list.addAll(getColNamesInExpression(exp));
        list.addAll(getColNamesInExpressionVals(exp));
        return list;
    }

    private static Set<String> getColumnsFromValues(Expression expr) {
        Set<String> cols = new HashSet<>();
        if (expr instanceof LogicalExpression) {
            LogicalExpression le = (LogicalExpression) expr;
            List<Expression> exprs = le.getExpressions();
            for (Expression e : exprs) {
                cols.addAll(getColumnsFromValues(e));
            }
        } else {
            SimpleExpression exp = (SimpleExpression) expr;
            Object val = exp.getValue();
            if (val != null) {
                String valStr = val.toString();
                Pattern pattern = Pattern.compile("`.+?`");
                Matcher matcher = pattern.matcher(valStr);
                while (matcher.find()) {
                    final String col = valStr.substring(matcher.start(), matcher.end());
                    cols.add(col);
                }
            }
        }
        return cols;
    }

    public static List<String> getOperandNames(Expression expr) {
        List<String> list = new ArrayList<>();
        if (expr instanceof LogicalExpression) {
            LogicalExpression le = (LogicalExpression) expr;
            List<Expression> exprs = le.getExpressions();
            for (Expression e : exprs) {
                list.addAll(getOperandNames(e));
            }
        } else {
            SimpleExpression exp = (SimpleExpression) expr;
            list.add(exp.getOperand());
        }
        return list;
    }

    public static Map<String, Object> getColNamesWithValuesInExpression(String exp) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (exp == null || exp.trim().isEmpty() || exp.trim().equals("{}")) {
            return map;
        }
        Expression expr = ExpressionUtil.fromJson(exp);
        if (expr != null) {
            map.putAll(getOperandNamesWithValues(expr));
        }
        return map;
    }

    private static Map<String, Object> getOperandNamesWithValues(Expression expr) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (expr instanceof LogicalExpression) {
            LogicalExpression le = (LogicalExpression) expr;
            List<Expression> exprs = le.getExpressions();
            for (Expression e : exprs) {
                map.putAll(getOperandNamesWithValues(e));
            }
        } else {
            SimpleExpression exp = (SimpleExpression) expr;
            map.put(exp.getOperand(), exp.getValue());
        }
        return map;
    }

    public static List<SimpleExpression> getAllSimpleExprs(Expression expr) {
        List<SimpleExpression> list = new ArrayList<>();
        if (expr instanceof SimpleExpression) {
            list.add((SimpleExpression) expr);
            return list;
        }
        LogicalExpression le = (LogicalExpression) expr;
        List<Expression> exprs = le.getExpressions();
        for (Expression e : exprs) {
            list.addAll(getAllSimpleExprs(e));
        }
        return list;
    }
    public static List<SimpleExpression> flattenExpr(Expression expr) {
        List<SimpleExpression> simpleExpressions = new ArrayList<>();
        flattenExpr(expr, simpleExpressions);
        return simpleExpressions;
    }

    private static void flattenExpr(Expression expr, List<SimpleExpression> simpleExpressions) {
        if (expr instanceof LogicalExpression) {
            LogicalExpression le = (LogicalExpression) expr;
            le.getExpressions().forEach(e->flattenExpr(e, simpleExpressions));
        } else if (expr instanceof SimpleExpression) {
            simpleExpressions.add((SimpleExpression) expr);
        }
    }

    public static boolean checkIfLogicalOR(Expression expr) {
        if (expr instanceof LogicalExpression) {
            LogicalExpression le = (LogicalExpression) expr;
            if (le.getLogicalOperator() == LogicalOperator.OR) {
                return true;
            }
            List<Expression> exprs = le.getExpressions();
            for (Expression e : exprs) {
                if (checkIfLogicalOR(e)) {
                    return true;
                }
            }
        }
        return false;
    }


    public static String applySinglQuotes(Object value) {
        String result = value != null ? value.toString() : null;
        if (value != null && !value.toString().isEmpty()) {
            result = "'" + value + "'";
        }
        return result;
    }

    public static String applyQuotes(String value) {
        String result = value;
        if (value != null && !value.isEmpty() && !value.trim().startsWith("`")) {
            if (result.contains("`")){
                result = result.replaceAll("`", "``");
            }
            result = "`" + result + "`";
        }
        return result;
    }

    public static String removeQuotes(String value) {
        String result = value;
        if (value != null && value.startsWith("`") && value.endsWith("`")) {
            result = value.substring(1, value.length() - 1);
        }
        return result;
    }
    

    
    public static void enquoteOperands(Expression expression){
        if (expression instanceof LogicalExpression){
            ((LogicalExpression)expression).getExpressions().forEach(exp -> enquoteOperands(exp));            
        }else {
            enquoteOperandsSe((SimpleExpression)expression);
        }
    }

    private static void enquoteOperandsSe(SimpleExpression expression) {
        expression.setOperand(applyQuotes(expression.getOperand()));        
    }
}
