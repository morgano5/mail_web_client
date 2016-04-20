package au.id.villar.email.webClient.mail;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    private Utils() {}

    public static PartInfo getSinglePartInfo(Part part, String path) throws IOException, MessagingException {
        return new PartInfo(part, path, PartInfo.Level.PART_ONLY);
    }

    public static Map<String, String> hrefMappings(Part part, String path)
            throws IOException, MessagingException {

        Part parent = part instanceof BodyPart ? ((BodyPart)part).getParent().getParent(): null;
        if(parent == null) {
            return Collections.emptyMap();
        }

        int length = path.lastIndexOf(',');
        if(length == -1) length = path.length();
        List<PartInfo> relatedParts =
                new PartInfo(parent, path.substring(0, length), PartInfo.Level.PART_AND_CHILDREN).parts;

        Map<String, String> hrefMappings = new HashMap<>(relatedParts.size());
        for(PartInfo partInfo: relatedParts) {
            if(partInfo.attachment || partInfo.contentId == null) continue;
            hrefMappings.put(partInfo.contentId, partInfo.path);
        }

        return hrefMappings;
    }

    public static String formattedInfo(Part part, String path) throws IOException, MessagingException {
        return new PartInfo(part, path, PartInfo.Level.DEEP).formattedInfo();
    }

}
