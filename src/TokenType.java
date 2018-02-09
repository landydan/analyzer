public enum TokenType {

    KEYWORD("keyword"),
    SYMBOL("symbol"),
    INTEGER_CONSTANT("integerConstant"),
    STRING_CONSTANT("stringConstant"),
    IDENTIFIER("identifier");

    private String tag;

    private TokenType(String tag) {
        this.tag = tag;
    }

    public String getStartTag() {
        return "<" + this.tag + ">";
    }

    public String getEndTag() {
        return "</" + this.tag + ">";
    }


    public String doTag(String value) {
        return getStartTag() + value + getEndTag();
    }
}


