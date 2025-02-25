package ai.netoai.collector.settings;

public class SystemSetting {

    private String category;
    private String name;
    private String dataType;
    private String value;
    private String description;
	private String aliasName;
    private String subCategory;
    private String additionalInformation;

    public SystemSetting() {

    }

    public SystemSetting(String category, String name, String dataType, String value) {
        this(category, name, dataType, value, null, null);
    }

    public SystemSetting(String category, String name, String dataType, String value, String subCategory, String additionalInformation) {
        this.category = category;
        this.name = name;
        this.dataType = dataType;
        this.value = value;
        this.subCategory = subCategory;
        this.additionalInformation = additionalInformation;
        if(this.aliasName == null){
            this.aliasName = nameToDescription(name);
        }
    }

    public SystemSetting(String category, String name, String dataType, String value, String aliasName) {
        this(category,name,dataType,value,aliasName, null, null);
        this.aliasName = aliasName;
        if(this.aliasName == null){
            this.aliasName = nameToDescription(name);
        }
    }

    public SystemSetting(String category, String name, String dataType, String value, String aliasName, String subCategory, String additionalInformation) {
        this(category,name,dataType,value,subCategory,additionalInformation);
        this.aliasName = aliasName;
        if(this.aliasName == null){
            this.aliasName = nameToDescription(name);
        }
    }

//    public SystemSetting(String subCategory,String additionalInformation) {
//        this.subCategory = subCategory;
//        this.additionalInformation=additionalInformation;
//
//    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setDescription(nameToDescription(name));
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }


    @Override
    public String toString() {
        return "SystemSetting{" +
                "category='" + category + '\'' +
                ", name='" + name + '\'' +
                ", dataType='" + dataType + '\'' +
                ", value='" + value + '\'' +
                ", aliasName='" + aliasName + '\'' +
                ", subCategory='" + subCategory + '\'' +
                ", additionalInformation='" + additionalInformation + '\'' +
                '}';
    }

    public static String nameToDescription(String str) {

        // Empty String
        String result = "";

        // Append first character(in lower case)
        // to result string
        char c = str.charAt(0);
        result = result + Character.toUpperCase(c);

        // Tarverse the string from
        // ist index to last index
        for (int i = 1; i < str.length(); i++) {

            char ch = str.charAt(i);

            // Check if the character is upper case
            // then append '_' and such character
            // (in lower case) to result string
            if (Character.isUpperCase(ch)) {
                result = result + ' ';
                result
                        = result
                        + Character.toUpperCase(ch);
            }

            // If the character is lower case then
            // add such character into result string
            else {
                result = result + ch;
            }
        }

        // return the result
        return result;
    }

    public static boolean isBackendModeWorker() {
        String backendMode = System.getenv("BACKEND_MODE");
        return backendMode != null && backendMode.equalsIgnoreCase("worker") ? true : false;
    }
}
