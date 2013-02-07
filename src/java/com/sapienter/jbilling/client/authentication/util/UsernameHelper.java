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

/**
 * Helper class to manage building username tokens for the {@link com.sapienter.jbilling.client.authentication.CompanyUserAuthenticationFilter} which
 * expects a username that also includes the client ID.
 *
 * @author Brian Cowdery
 * @since 24-11-2010
 */
public class UsernameHelper {

    public static final String VALUE_SEPARATOR = ";";

    
    public static String buildUsernameToken(String username, Integer companyId) {
        return buildUsernameToken(username, companyId.toString());
    }

    public static String buildUsernameToken(String username, String companyId) {
        StringBuilder token = new StringBuilder();
        token.append(username);
        token.append(VALUE_SEPARATOR);
        token.append(companyId);

        return token.toString();
    }
}
