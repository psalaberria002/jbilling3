/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.client.authentication;

import com.sapienter.jbilling.client.authentication.util.SecuritySession;
import com.sapienter.jbilling.client.authentication.util.UsernameHelper;
import org.apache.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * An extension of the base spring security {@link UsernamePasswordAuthenticationFilter} that appends
 * the user entered company ID to the username for authentication.
 *
 * Similar to the {@link UsernamePasswordAuthenticationFilter}, the web form parameter names can be
 * configured via spring bean properties. 
 *
 * Default configuration:
 *      passwordParameter = "j_password"
 *      usernameParameter = "j_username"
 *      clientIdParameter = "j_client_id"
 *
 *
 *
 * @author Brian Cowdery
 * @since 04-10-2010
 */
public class CompanyUserAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger LOG = Logger.getLogger(CompanyUserAuthenticationFilter.class);

    public static final String FORM_CLIENT_ID_KEY = "j_client_id";

    private String clientIdParameter;
    private SecuritySession securitySession;

    public final String getClientIdParameter() {
        return clientIdParameter == null ? FORM_CLIENT_ID_KEY : clientIdParameter;
    }

    public void setClientIdParameter(String clientIdParameter) {
        this.clientIdParameter = clientIdParameter;
    }

    public SecuritySession getSecuritySession() {
        return securitySession;
    }

    public void setSecuritySession(SecuritySession securitySession) {
        this.securitySession = securitySession;
    }

    /**
     * Returns the form submitted user name as colon delimited string containing
     * the user name and client id of the user to authenticate, e.g., "bob:1"
     *
     * @param request HTTP servlet request
     * @return username string
     */
    @Override
    protected String obtainUsername(HttpServletRequest request) {
        String username = request.getParameter(getUsernameParameter());
        String companyId = request.getParameter(getClientIdParameter());

        return UsernameHelper.buildUsernameToken(username, companyId);
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            Authentication result) throws IOException, ServletException {

        if (securitySession != null) {
            securitySession.setAttributes(request, response, (CompanyUserDetails) result.getPrincipal());
        }

        super.successfulAuthentication(request, response, result);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {

        LOG.debug("User " + failed.getAuthentication().getPrincipal() + " authentication failed!");

        if (securitySession != null) {
            securitySession.clearAttributes(request, response);
        }

        super.unsuccessfulAuthentication(request, response, failed);
    }
}
