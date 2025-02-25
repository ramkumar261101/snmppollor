package ai.netoai.collector.snmp;

import ai.netoai.collector.comms.ActionScriptEvaluator;
import ai.netoai.collector.deviceprofile.Metric;
import ai.netoai.collector.settings.SettingsManager;
import ai.netoai.collector.settings.SnmpSettings;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;

import org.snmp4j.Snmp;

public class AsyncSnmpResponseListener implements ResponseListener {
    private static final Logger log = LoggerFactory.getLogger(AsyncSnmpResponseListener.class);
    private static final String tracePrefix = "[" + AsyncSnmpResponseListener.class.getSimpleName() + "]: ";
    private static final int _RESP_QUEUE_THRESHOLD_SIZE = 1000;
    private static ThreadPoolExecutor responseProcessor;
    private static SnmpSettings snmpSettings;

    public AsyncSnmpResponseListener() {
        SettingsManager setting = SettingsManager.getInstance();
        snmpSettings = new SnmpSettings();
    }

    @Override
    public void onResponse(ResponseEvent event) {
        try {
            ((Snmp) event.getSource()).cancel(event.getRequest(), this);
            if (event != null) {
                if (event.getUserObject() != null && event.getUserObject() instanceof PollerSnmpAsyncObj) {
                    ((PollerSnmpAsyncObj) event.getUserObject()).setRespRecTime(System.currentTimeMillis());
                }
            }
        } catch (Throwable t) {
            log.error(tracePrefix + "Failed processing event: " + event, t);
        }
    }


    private Double roundToDecimalPlaces(int decPlaces, double value) {
        try {
            if (value > -1 && value < 1)
                return value;

            BigDecimal bd = new BigDecimal(Double.toString(value));
            bd = bd.setScale(decPlaces, BigDecimal.ROUND_HALF_UP);
            return bd.doubleValue();
        } catch (NumberFormatException x) {
            return (value);
        }
    }
}

class PostfixEvaluator {
    private static final Logger log = LoggerFactory.getLogger(PostfixEvaluator.class);
    private Metric metric;
    private String expression;
    private String[] elements;
    private Stack<Double> stack;
    private String evalType;
    private static final List<String> operators = new ArrayList<>();

    static {
        operators.add("+");
        operators.add("-");
        operators.add("*");
        operators.add("/");
        operators.add("%");
        operators.add("EQ");
        operators.add("SWAP");
        operators.add("IF");
    }

    public PostfixEvaluator(String expr, String evalType) {
        stack = new Stack<>();
        this.expression = expr;
        this.evalType = evalType;
        if (this.evalType == null) {
            createElements();
        }
    }

    public void setMetric(Metric metric) {
        this.metric = metric;
    }

    private void createElements() {
        if (expression != null) {
            elements = expression.split(",");
        } else {
            elements = new String[0];
        }
    }

    public synchronized Double evaluate(Map<String, Object> valueMap) {
        if (log.isTraceEnabled())
            log.trace("PostfixEvaluator: ValueMap: " + valueMap);
        double finalVal = Double.NaN;
        try {

            if (evalType != null) {
                String script = "";
                if (metric.getScriptVariables() != null) {
                    String[] vars = metric.getScriptVariables().split(",");
                    for (String var : vars) {
                        if (valueMap.containsKey(var)) {
                            Object val = valueMap.get(var);
                            if (valueMap.get(var) instanceof String) val = "'" + val + "'";
                            script += "var " + var + " = " + val + ";";
                        } else {
                            return Double.NaN;
                        }
                    }
                    script += "(" + metric.getValue() + ")()";
                    Object resultVal = ActionScriptEvaluator.getInstance().evaluateScript(script);
                    if (resultVal != null && isDouble(resultVal.toString())) {
                        finalVal = Double.valueOf(resultVal.toString());
                    }
                    return finalVal;
                } else {
                    return Double.NaN;
                }
            }
            if (elements == null || elements.length == 0) {
                return finalVal;
            }

            for (String elem : elements) {
                if (isOperator(elem)) {
                    if (elem.equals("+")) {
                        stack.push(stack.pop() + stack.pop());
                    } else if (elem.equals("-")) {
                        Double op2 = stack.pop();
                        Double op1 = stack.pop();
                        stack.push(op1 - op2);
                    } else if (elem.equals("*")) {
                        stack.push(stack.pop() * stack.pop());
                    } else if (elem.equals("/")) {
                        Double op2 = stack.pop();
                        Double op1 = stack.pop();
                        stack.push(op1 / op2);
                    } else if (elem.equals("%")) {
                        Double op2 = stack.pop();
                        Double op1 = stack.pop();
                        stack.push(op1 % op2);
                    } else if (elem.equals("EQ")) {
                        double op2 = stack.pop();
                        double op1 = stack.pop();
                        stack.push(op1 == op2 ? 1.0 : 0);
                    } else if (elem.equals("SWAP")) {
                        double op2 = stack.pop();
                        double op1 = stack.pop();
                        stack.push(op2);
                        stack.push(op1);
                    } else if (elem.equals("IF")) {
                        double op3 = stack.pop();
                        double op2 = stack.pop();
                        double op1 = stack.pop();
                        stack.push(op1 != 0 ? op2 : op3);
                    } else {
                        throw new RuntimeException("PostfixEvaluator: Operator [" + elem + "] Not supported yet !!");
                    }
                } else {
                    Double val = null;
                    if (isDouble(elem)) {
                        val = Double.valueOf(elem);
                        stack.push(val);
                    } else if (valueMap.containsKey(elem)) {
                        stack.push((Double) valueMap.get(elem));
                    } else {
                        throw new RuntimeException("PostfixEvaluator: Operand [" + elem + "] Not a number or a known Variable, ValueMap: " + valueMap);
                    }
                }
            }

            finalVal = stack.pop();
            if (!stack.isEmpty()) {
                throw new RuntimeException("PostfixEvaluator: Stack not empty after evaluation invalid expression: [" + expression + "], ValueMap: " + valueMap);
            }
        } catch (Exception ex) {
            finalVal = Double.NaN;
        }
        return finalVal;
    }

    private static boolean isOperator(String elem) {
        return operators.contains(elem);
    }

    private static boolean isDouble(String elem) {
        try {
            Double.parseDouble(elem);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
