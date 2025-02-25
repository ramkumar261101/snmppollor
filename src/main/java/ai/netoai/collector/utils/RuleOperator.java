package ai.netoai.collector.utils;

public enum RuleOperator {

    MATCHES("Matches"),
    MATCHES_IGNORE_CASE("Matches Ignore Case"),
    NOT_MATCHES("Not Matches"),
    NOT_MATCHES_IGNORE_CASE("Not Matches Ignore Case"),
    EQUALS("Equals"),
    EQUALS_IGNORE_CASE("Equals Ignore Case"),
    GREATER_THAN("Greater Than"),
    GREATER_THAN_EQUALS("Greater Than Equals"),
    LESS_THAN("Less Than"),
    LESS_THAN_EQUALS("Less Than Equals"),
    IN("In"),
    BETWEEN("Between"),
    IS_NULL("Is Null"),
    NOT_NULL("Not Null"),
    NOT_EQUAL("Not Equal"),
    NOT_EQUAL_IGNORE_CASE("Not Equals Ignore Case"),
    // CONTAINS("Contains"),
    STARTS_WITH("Starts With"),
    STARTS_WITH_IGNORE_CASE("Starts With Ignore Case"),
    ENDS_WITH("Ends With"),
    ENDS_WITH_IGNORE_CASE("Ends With Ignore Case"),
    PATTERN("Pattern"),
    IS_EMPTY("Is Empty"),
    NOT_EMPTY("Not Empty"),
    RLIKE("RLIKE"),
    NOT_RLIKE("NOT RLIKE");

    private String Operator;

    private RuleOperator(String operator) {
        this.Operator = operator;
    }

    @Override
    public String toString() {
        return Operator;
    }

    public static RuleOperator asEnum(String value) {
        switch (value) {
        case "Equals":
            return RuleOperator.EQUALS;
            case "Equals Ignore Case":
            return RuleOperator.EQUALS_IGNORE_CASE;
        case "Matches":
            return RuleOperator.MATCHES;
        case "Matches Ignore Case":
        	return RuleOperator.MATCHES_IGNORE_CASE;
        case "Not Matches":
            return RuleOperator.NOT_MATCHES;
        case "Not Matches Ignore Case":
            return RuleOperator.NOT_MATCHES_IGNORE_CASE;    
        case "Greater Than":
            return RuleOperator.GREATER_THAN;
        case "Greater Than Equals":
            return RuleOperator.GREATER_THAN_EQUALS;
        case "Less Than":
            return RuleOperator.LESS_THAN;
        case "Less Than Equals":
            return RuleOperator.LESS_THAN_EQUALS;
        case "In":
            return RuleOperator.IN;
        case "Between":
            return RuleOperator.BETWEEN;
        case "Is Null":
            return RuleOperator.IS_NULL;
        case "Not Null":
            return RuleOperator.NOT_NULL;
        case "Not Equal":
            return RuleOperator.NOT_EQUAL;
            case "Not Equals Ignore Case":
                return RuleOperator.NOT_EQUAL_IGNORE_CASE;
            /*
         * case "Contains": return RuleOperator.CONTAINS;
         */
        case "Starts With":
            return RuleOperator.STARTS_WITH;
        case "Ends With":
            return RuleOperator.ENDS_WITH;
            case "Starts With Ignore Case":
                return RuleOperator.STARTS_WITH_IGNORE_CASE;
                case "Ends With Ignore Case":
                return RuleOperator.ENDS_WITH_IGNORE_CASE;
            case "RLIKE":
                return RuleOperator.RLIKE;
            case "NOT RLIKE":
                return RuleOperator.NOT_RLIKE;
        default:
            return RuleOperator.EQUALS;
        }
    }

    public static RuleOperator getOpposedOperator(RuleOperator op) {
        switch (op) {
        case EQUALS:
            return NOT_EQUAL;
        case NOT_EQUAL:
            return EQUALS;
        case MATCHES:
            return NOT_MATCHES;
        case NOT_MATCHES:
            return MATCHES;
        case GREATER_THAN:
            return LESS_THAN;
        case LESS_THAN:
            return GREATER_THAN;
        case GREATER_THAN_EQUALS:
            return LESS_THAN_EQUALS;
        case LESS_THAN_EQUALS:
            return GREATER_THAN_EQUALS;
        /*
         * case CONTAINS: return NOT_MATCHES;
         */
        case IS_NULL:
            return NOT_NULL;
        case NOT_NULL:
            return IS_NULL;
        default:
            return null;
        }
    }
}
