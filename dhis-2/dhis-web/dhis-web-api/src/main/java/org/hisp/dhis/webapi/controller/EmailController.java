package org.hisp.dhis.webapi.controller;



import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.email.Email;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
@Controller
@RequestMapping( value = EmailController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class EmailController
{
    public static final String RESOURCE_PATH = "/email";
    private static final String SMTP_ERROR = "SMTP server not configured";

    //--------------------------------------------------------------------------
    // Dependencies
    //--------------------------------------------------------------------------

    @Autowired
    private EmailService emailService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private WebMessageService webMessageService;

    @RequestMapping( value = "/test", method = RequestMethod.POST )
    public void sendTestEmail( HttpServletResponse response, HttpServletRequest request ) throws WebMessageException
    {
        checkEmailSettings();

        if ( !currentUserService.getCurrentUser().hasEmail() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Could not send test email, no email configured for current user" ) );
        }

        OutboundMessageResponse emailResponse = emailService.sendTestEmail();

        emailResponseHandler( emailResponse, request, response );
    }

    @RequestMapping( value = "/notification", method = RequestMethod.POST )
    public void sendSystemNotificationEmail( @RequestBody Email email,
        HttpServletResponse response, HttpServletRequest request ) throws WebMessageException
    {
        checkEmailSettings();

        boolean systemNotificationEmailValid = systemSettingManager.systemNotificationEmailValid();

        if ( !systemNotificationEmailValid )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Could not send email, system notification email address not set or not valid" ) );
        }

        OutboundMessageResponse emailResponse = emailService.sendSystemEmail( email );

        emailResponseHandler( emailResponse, request, response );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SEND_EMAIL')" )
    @RequestMapping( value = "/notification", method = RequestMethod.POST, produces = "application/json" )
    public void sendEmailNotification( @RequestParam Set<String> recipients, @RequestParam String message,
        @RequestParam ( defaultValue = "DHIS 2" ) String subject,
        HttpServletResponse response, HttpServletRequest request ) throws WebMessageException
    {
        checkEmailSettings();

        OutboundMessageResponse emailResponse = emailService.sendEmail( subject, message, recipients );

        emailResponseHandler( emailResponse, request, response );
    }

    // ---------------------------------------------------------------------
    // Supportive methods
    // ---------------------------------------------------------------------

    private void emailResponseHandler( OutboundMessageResponse emailResponse, HttpServletRequest request,
       HttpServletResponse response )
    {
        if ( emailResponse.isOk() )
        {
            String msg = !StringUtils.isEmpty( emailResponse.getDescription() ) ?
                emailResponse.getDescription() : EmailResponse.SENT.getResponseMessage();
            webMessageService.send( WebMessageUtils.ok( msg ), response, request );
        }
        else
        {
            String msg = !StringUtils.isEmpty( emailResponse.getDescription() ) ?
                emailResponse.getDescription() : EmailResponse.FAILED.getResponseMessage();
            webMessageService.send( WebMessageUtils.error( msg ), response, request );
        }
    }

    private void checkEmailSettings() throws WebMessageException
    {
        if ( !emailService.emailConfigured() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( SMTP_ERROR ) );
        }
    }
}
