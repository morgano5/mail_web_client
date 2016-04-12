package au.id.villar.email.webClient.web;

import au.id.villar.email.webClient.mail.Mailbox;
import au.id.villar.email.webClient.users.Role;
import au.id.villar.email.webClient.mail.MailFolder;
import au.id.villar.email.webClient.mail.MailboxService;
import au.id.villar.email.webClient.tokens.UserPasswordHolder;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.portlet.ModelAndView;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
            @RequestParam(required = false, defaultValue = "") String fullFolderName,
            @RequestParam(required = false, defaultValue = "0") int startingPageIndex,
            @RequestParam(required = false, defaultValue = "10") int pageLength,
            UserPasswordHolder userPassword) {
        try {
            return service.getMailbox(userPassword.getUsername(), userPassword.getPassword())
                    .getStartingFolder(fullFolderName, startingPageIndex, pageLength);
        } catch (MessagingException | IOException e) {
            LOG.error("Error getting starting folder: " + e.getMessage(), e);
            throw new InternalServerErrorException();
        }
    }

    @Permissions(Role.MAIL_USER)
    @RequestMapping(value = "/mail/folder", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody MailFolder getFolder(
            @RequestParam(required = false) String fullFolderName,
            @RequestParam(required = false, defaultValue = "0") int startingPageIndex,
            @RequestParam(required = false, defaultValue = "10") int pageLength,
            UserPasswordHolder userPassword) {
        try {
            return service.getMailbox(userPassword.getUsername(), userPassword.getPassword())
                    .getFolder(fullFolderName, startingPageIndex, pageLength);
        } catch (MessagingException | IOException e) {
            LOG.error("Error getting folder: " + e.getMessage(), e);
            throw new InternalServerErrorException();
        }
    }

    @Permissions(Role.MAIL_USER)
    @RequestMapping(value = "/mail/subFolders", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody MailFolder[] getSubFolders(
            @RequestParam(required = false) String fullFolderName, UserPasswordHolder userPassword) {
        try {
            return service.getMailbox(userPassword.getUsername(), userPassword.getPassword())
                    .getSubFolders(fullFolderName);
        } catch (MessagingException | IOException e) {
            LOG.error("Error getting subfolders: " + e.getMessage(), e);
            throw new InternalServerErrorException();
        }
    }

    @Permissions(Role.MAIL_USER)
    @RequestMapping(value = "/mail/messages/{mailReference}", method = RequestMethod.GET)
    public void getContent(@PathVariable("mailReference") String mailReference, UserPasswordHolder userPassword,
            ModelAndView modelAndView, HttpServletRequest request, HttpServletResponse response) {
        modelAndView.clear();
        Mailbox mailbox = service.getMailbox(userPassword.getUsername(), userPassword.getPassword());
        new MailContentProcessor(mailbox).transferMainContent(mailReference, response);
    }
}
