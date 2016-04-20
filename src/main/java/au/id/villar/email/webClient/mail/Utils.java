package au.id.villar.email.webClient.mail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class Utils {

    private Utils() {}

    static String getMainValue(String headerValue) {
        int semiColonPos = headerValue.indexOf(';');
        return headerValue.substring(0, semiColonPos != -1? semiColonPos: headerValue.length()).trim().toLowerCase();
    }

    static Map<String, String> getParameters(String headerValue) {
        int semiColonPos = headerValue.indexOf(';');

        if(semiColonPos == -1) {
            return Collections.emptyMap();
        }


        Map<String, String> parameterMap = new HashMap<>();
        String strParameters = headerValue.substring(semiColonPos + 1);

        // TODO rewrite this part
        String[] parameters = strParameters.split("[ \\t]*;[ \\t]*");
        for (String parameter : parameters) {
            parameter = parameter.trim();
            int equalsPos = parameter.indexOf('=');
            String name = parameter.substring(0, equalsPos).trim();
            parameter = parameter.substring(equalsPos + 1).trim();
            parameter = parameter.startsWith("\"") ? parameter.substring(1, parameter.length() - 1) : parameter;
            parameterMap.put(name, parameter);
        }
        // ----------------------

        return parameterMap;
    }
}
