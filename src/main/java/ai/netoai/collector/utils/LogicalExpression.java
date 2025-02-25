package ai.netoai.collector.utils;

import ai.netoai.collector.snmp.trap.SnmpTrap;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogicalExpression extends Expression {
    private static final long serialVersionUID = 3701242098201651165L;
    private LogicalOperator logicalOperator;
    private List<Expression> expressions = new ArrayList<>();

    public LogicalExpression() {

    }

    public LogicalExpression(LogicalOperator op) {
        this.logicalOperator = op;
    }

    public LogicalExpression(LogicalOperator op, Expression... expressions) {
        setLogicalOperator(op);
        if (expressions != null) {
            for (Expression ex : expressions) {
                addExpression(ex);
            }
        }
    }

    /**
     * @return the logicalOperator
     */
    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    /**
     * @param logicalOperator
     *            the logicalOperator to set
     */
    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    /**
     * @return the expressions
     */
    public List<Expression> getExpressions() {
        return expressions;
    }

    /**
     * @param expressions
     *            the expressions to set
     */
    public void setExpressions(List<Expression> expressions) {
        this.expressions = expressions;
        if (getDialect() != null && CollectionUtils.isNotEmpty(expressions)) {
            expressions.forEach(exp -> exp.setDialect(getDialect()));
        }
    }

    public void addExpression(Expression ex) {
        if (ex != null) {
            this.expressions.add(ex);
            if (getDialect() != null){
                ex.setDialect(getDialect());
            }
        }
    }

    @Override
    public boolean evaluate(Map<Object, Object> map) {
        boolean result = false;
        if (logicalOperator == LogicalOperator.AND) {
            for (Expression exp : expressions) {
                if (!exp.evaluate(map)) {
                    result = false;
                    break;
                }
                result = true;
            }
        } else if (logicalOperator == LogicalOperator.OR) {
            for (Expression exp : expressions) {
                if (!result) {
                    if (exp.evaluate(map)) {
                        result = true;
                    }
                }
            }
        } else if (logicalOperator == LogicalOperator.NOT) {
            for (Expression exp : expressions) {
                if (exp.evaluate(map)) {
                    result = true;
                }
            }
            result = !result;
        }
        return result;
    }

	@Override
	public boolean evaluate(Object bean, Class<?> type) {
		// TODO Auto-generated method stub
		return false;
	}

//    @Override
//    public boolean evaluate(Object bean, Class<?> type) {
//        boolean result = false;
//        if (logicalOperator == LogicalOperator.AND) {
//            for (Expression exp : expressions) {
//                if (!exp.evaluate(bean, type)) {
//                    result = false;
//                    break;
//                }
//                result = true;
//            }
//        } else if (logicalOperator == LogicalOperator.OR) {
//            for (Expression exp : expressions) {
//                if (!result) {
//                    if (exp.evaluate(bean, type)) {
//                        result = true;
//                    }
//                }
//            }
//        }
//        return result;
//    }

    @Override
    public boolean evaluate(SnmpTrap trap) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void setDialect(Dialect dialect) {
        super.setDialect(dialect);
        if (dialect != null && CollectionUtils.isNotEmpty(expressions)) {
            expressions.forEach(exp -> {
                exp.setDialect(dialect);
            });
        }
    }
    
    @Override
    public ArrayList<Integer> getExcludeDays() {
        return excludeDays;
    }

}
