package au.id.villar.email.webClient.mail;

import javax.mail.MessagingException;
import javax.mail.Part;
import java.util.Map;

public class ContentType {

    public static final ContentType DEFAULT = new ContentType("text/plain; charset=\"us-ascii\"");

    public final String type;
    public final String charset;

    public ContentType(Part part) throws MessagingException {
        this(getContentTypeRawValue(part));
    }

    public ContentType(String rawValue) {
        String type = Utils.getMainValue(rawValue);
        Map<String, String> parameters = Utils.getParameters(rawValue);
        String charset = parameters.get("charset");
        this.type = type.toLowerCase();
        this.charset = charset != null ? charset : (this.type.startsWith("text/") ? "us-ascii" : null);
    }

    public String toHeaderValue() {
        return type + (charset != null ? "; charset=\"" + charset + '"' : "");
    }

    private static String getContentTypeRawValue(Part part) throws MessagingException {
        String[] rawValues = part.getHeader("Content-type");
        return rawValues == null || rawValues.length == 0? "text/plain; charset=\"us-ascii\"": rawValues[0];
    }

}
