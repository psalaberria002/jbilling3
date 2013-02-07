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

package com.sapienter.jbilling.client.authentication.util;

import com.sapienter.jbilling.client.authentication.CompanyUserDetails;
import grails.plugins.springsecurity.Secured;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Helper class to handle the export of user attributes as session attributes.
 *
 * @author Brian Cowdery
 * @since 25-11-2010
 */
public class SecuritySession {

    private static final Logger LOG = Logger.getLogger(SecuritySession.class);

    public static final String USER_ID = "user_id";
    public static final String USER_MAIN_ROLE_ID = "main_role_id";
    public static final String USER_LANGUAGE_ID = "language_id";
    public static final String USER_CURRENCY_ID = "currency_id";
    public static final String USER_COMPANY_ID = "company_id";
    public static final String USER_LOCALE = "locale";

    private LocaleResolver localeResolver;

    public LocaleResolver getLocaleResolver() {
        return localeResolver;
    }

    public void setLocaleResolver(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    public SecuritySession() {
    }

    /**
     * Sets common attributes of the logged in user as session attributes.
     *
     * @param request http servlet request
     * @param response http servlet response
     * @param principal logged in user
     */
    public void setAttributes(HttpServletRequest request, HttpServletResponse response, CompanyUserDetails principal) {
        HttpSession session = request.getSession();

        session.setAttribute(USER_ID, principal.getUserId());
        session.setAttribute(USER_MAIN_ROLE_ID, principal.getMainRoleId());
        session.setAttribute(USER_LANGUAGE_ID, principal.getLanguageId());
        session.setAttribute(USER_CURRENCY_ID, principal.getCurrencyId());
        session.setAttribute(USER_COMPANY_ID, principal.getCompanyId());
        session.setAttribute(USER_LOCALE, principal.getLocale());

        // set locale for spring/grails
        if (localeResolver != null) {
            LOG.debug("Setting locale for Spring contexts: " + principal.getLocale());
            localeResolver.setLocale(request, response, principal.getLocale());

        } else {
            LOG.warn("Locale resolver not set or not available, cannot set user locale for Spring contexts!");
        }
    }

    /**
     * Clears the security session variables from the current HttpSession. This should be
     * done whenever an un-successful login attempt is made to ensure that no session attributes
     * leak over when switching users or performing complex authentication steps.
     *
     * @param request http servlet request
     * @param response http servlet response
     */
    public void clearAttributes(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();

        session.removeAttribute(USER_ID);
        session.removeAttribute(USER_MAIN_ROLE_ID);
        session.removeAttribute(USER_LANGUAGE_ID);
        session.removeAttribute(USER_CURRENCY_ID);
        session.removeAttribute(USER_COMPANY_ID);
        session.removeAttribute(USER_LOCALE);
    }
}
