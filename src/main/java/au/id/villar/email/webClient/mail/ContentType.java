package au.id.villar.email.webClient.mail;

import javax.mail.MessagingException;
import javax.mail.Part;

public class ContentType {

    public final String type;
    public final String charset;

    public ContentType(Part part) throws MessagingException {

        String[] rawValues = part.getHeader("Content-type");
        if(rawValues == null || rawValues.length == 0) {
            type = "text/plain";
            charset = "us-ascii";
            return;
        }

        String rawValue = rawValues[0];
        int semiColonPos = rawValue.indexOf(';');
        String type = rawValue.substring(0, semiColonPos != -1? semiColonPos: rawValue.length()).trim().toLowerCase();
        String charset = null;
        if(semiColonPos != -1) {

            String strParameters = rawValue.substring(semiColonPos + 1);

            // TODO rewrite this part
            String[] parameters = strParameters.split("[ \\t]*;[ \\t]*");
            for (String parameter : parameters) {
                parameter = parameter.trim();
                if (!parameter.startsWith("charset")) continue;
                parameter = parameter.substring(parameter.indexOf('=') + 1);
                parameter = parameter.startsWith("\"") ? parameter.substring(1, parameter.length() - 1) : parameter;
                charset = parameter;
                break;
            }
            // ----------------------
        }

        this.type = type.toLowerCase();
        this.charset = charset != null ? charset : (this.type.startsWith("text/") ? "us-ascii" : null);
    }

    public String toHeaderValue() {
        return type + (charset != null ? "; charset=\"" + charset + '"' : "");
    }

}
