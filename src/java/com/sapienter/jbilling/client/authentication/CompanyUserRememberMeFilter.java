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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Remember me authentication filter extended to set session attributes from the logged
 * in user on successful authentication. 
 *
 * @author Brian Cowdery
 * @since 25-11-2010
 */
public class CompanyUserRememberMeFilter extends RememberMeAuthenticationFilter {

    private SecuritySession securitySession;

    public SecuritySession getSecuritySession() {
        return securitySession;
    }

    public void setSecuritySession(SecuritySession securitySession) {
        this.securitySession = securitySession;
    }

    @Override
    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              Authentication result) {

        if (securitySession != null) {
            securitySession.setAttributes(request, response, (CompanyUserDetails) result.getPrincipal());
        }

        super.onSuccessfulAuthentication(request, response, result);
    }

    @Override
    protected void onUnsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                                AuthenticationException failed) {

        if (securitySession != null) {
            securitySession.clearAttributes(request, response);
        }

        super.onUnsuccessfulAuthentication(request, response, failed);
    }
}
