/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.netoai.collector.snmp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lokesh
 */
public class PostfixEvaluatorTest {

    private static final Logger log = LoggerFactory.getLogger(PostfixEvaluatorTest.class);
    private String expression;
    private String[] elements;
    private Stack<Double> stack;
    private static final List<String> operators = new ArrayList<>();

    public enum ConversionFunction {
        PER_POLL_PER_SECOND, PER_POLL, NONE;

        public static ConversionFunction asEnum(String value) {
            if (value.equalsIgnoreCase("per_poll_per_second")) {
                return PER_POLL_PER_SECOND;
            } else if (value.equalsIgnoreCase("per_poll")) {
                return PER_POLL;
            } else if (value.equalsIgnoreCase("none")) {
                return NONE;
            }
            return null;
        }

        @Override
        public String toString() {
            switch(this) {
                case NONE:
                    return "none";
                case PER_POLL:
                    return "per_poll";
                case PER_POLL_PER_SECOND:
                    return "per_poll_per_second";
                default:
                    return "na";    
            }
        }

    }

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

    public static void main(String[] args) {
        PostfixEvaluatorTest eval = new PostfixEvaluatorTest("ifHCInOctets,8,*,ifSpeed,/,100,*");
        Map<String, Double> map = new HashMap<>();
        map.put("ifHCInOctets", Double.parseDouble("71426843099"));
        map.put("ifSpeed", Double.parseDouble("1000000000"));
        System.out.println("Output: " + eval.evaluate(map) + " -> " + ConversionFunction.PER_POLL_PER_SECOND.toString());
    }

    public PostfixEvaluatorTest(String expr) {
        stack = new Stack<>();
        this.expression = expr;
        createElements();
    }

    private void createElements() {
        if (expression != null) {
            elements = expression.split(",");
        } else {
            elements = new String[0];
        }
    }

    public synchronized Double evaluate(Map<String, Double> valueMap) {
        if (log.isTraceEnabled()) {
            log.trace("PostfixEvaluator: ValueMap: " + valueMap);
        }
        double finalVal = Double.NaN;
        try {
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
                        stack.push(valueMap.get(elem));
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
