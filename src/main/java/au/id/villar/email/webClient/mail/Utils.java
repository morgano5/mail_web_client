package au.id.villar.email.webClient.mail;

class Utils {

    private Utils() {}

    static boolean isMultipart(String contentType) {
        return contentType != null && contentType.trim().toLowerCase().startsWith("multipart/");
    }

}
