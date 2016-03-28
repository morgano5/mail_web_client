package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.domain.Role;
import au.id.villar.email.webClient.mail.MailFolder;
import au.id.villar.email.webClient.mail.MailboxService;
import au.id.villar.email.webClient.tokens.UserPasswordHolder;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.mail.MessagingException;

@Controller("/mail")
public class MailFolderController {

    private static final Logger LOG = Logger.getLogger(MailFolderController.class);

    private MailboxService service;

    @Autowired
    public void setService(MailboxService service) {
        this.service = service;
    }

    @Permissions(Role.MAIL_USER)
    @RequestMapping(value = "/mail/start", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody MailFolder getStartingFolder(
            @RequestParam(required = false) String folderName,
            @RequestParam(required = false, defaultValue = "0") int startingPageIndex,
            @RequestParam(required = false, defaultValue = "10") int pageLength,
            UserPasswordHolder userPassword) {
        try {
            return service.getMailbox(userPassword.getUsername(), userPassword.getPassword())
                    .getStartingFolder(folderName, startingPageIndex, pageLength);
        } catch (MessagingException e) {
            LOG.error("Error getting starting folder: " + e.getMessage(), e);
            throw new InternalServerErrorException();
        }
    }
}