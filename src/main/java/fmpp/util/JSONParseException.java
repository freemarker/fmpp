package fmpp.util;

public class JSONParseException extends ExceptionCC {

    private static final long serialVersionUID = 1L;

    public JSONParseException(
            String message, String text, int position, String fileName) {
        super(StringUtil.createSourceCodeErrorMessage(
                message, text, position, fileName, 56));
    }

    public JSONParseException(
            String message, String text, int position, String fileName,
            Throwable cause) {
        super(StringUtil.createSourceCodeErrorMessage(
                message, text, position, fileName, 56),
                cause);
    }

}
