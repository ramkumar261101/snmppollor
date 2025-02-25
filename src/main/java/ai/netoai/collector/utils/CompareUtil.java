package ai.netoai.collector.utils;

import org.apache.commons.lang3.math.NumberUtils;

public class CompareUtil {
	
	    @SuppressWarnings({ "unchecked", "rawtypes" })
		private static boolean compareEquals(Object value1, Object value2) {
	        if (value2 == null || value1 == null) {
	            return (value1 == value2);
	        } else if (value2 == value1) {
	            return true;
	        } else if (value1 instanceof Comparable && value2 instanceof Comparable
	                && value1.getClass().isAssignableFrom(value2.getClass())) {
	            return ((Comparable) value2).compareTo(value1) == 0;
	        } else {
	        	if ( value1 instanceof String || value2 instanceof String ) {
	        		return value1.toString().equals(value2.toString());
	        	}
	            return value2.equals(value1);
	        }
	    }
		@SuppressWarnings({ "unchecked", "rawtypes" })
		private static int compareValue(Object value1,Object value2) {
	        if (null == value1) {
	            return null == value2 ? 0 : -1;
	        } else if (null == value2) {
	            return 1;
	        } else if (value1 instanceof Comparable && value2 instanceof Comparable
	                && NumberUtils.isNumber(value1.toString()) && NumberUtils.isNumber(value2.toString())) {
	        	Double d1 = Double.valueOf(value1.toString());
	        	Double d2 = Double.valueOf(value2.toString());
	            return ((Comparable) d1).compareTo(d2);
	        }
	        throw new IllegalArgumentException("Could not compare the arguments: "
	                + value1 + "(" + value1.getClass() + "), " + value2 + "(" + value2.getClass() + ")");
	    }
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
        private static boolean compareNotEquals(Object value1, Object value2) {
            if (value2 == null || value1 == null) {
                return (value1 != value2);
            } else if (value1 instanceof Comparable && value2 instanceof Comparable
                    && NumberUtils.isNumber(value1.toString()) && NumberUtils.isNumber(value2.toString())) {
                Double d1 = Double.valueOf(value1.toString());
                Double d2 = Double.valueOf(value2.toString());
                int val=((Comparable) d1).compareTo(d2);
                if(val == 0) {
                     return false;
                }else{
                    return true;
                }
            } else {
                if ( value1 instanceof String || value2 instanceof String ) {
                    return !value1.toString().equals(value2.toString());
                }
                return !value2.equals(value1);
            }
        }
		
		@SuppressWarnings({ "incomplete-switch" })
		public static boolean compare(RuleOperator operator,Object value1,Object value2){
			switch (operator) {
		        case EQUALS:
		            return compareEquals(value1,value2);
		        case GREATER_THAN:
		            return compareValue(value1, value2) > 0;
		        case LESS_THAN:
		            return compareValue(value1, value2) < 0;
		        case GREATER_THAN_EQUALS:
		            return compareValue(value1, value2) >= 0;
		        case LESS_THAN_EQUALS:
		            return compareValue(value1, value2) <= 0;
		        case NOT_EQUAL:
                    return compareNotEquals(value1, value2);
		        }
		        return false;
		}
	  
}
