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

package com.sapienter.jbilling.client.authentication.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * LicenseExpiredException
 *
 * @author Brian Cowdery
 * @since 15/07/11
 */
public class LicenseExpiredException extends AuthenticationException {

    public LicenseExpiredException(String msg, Throwable t) {
        super(msg, t);
    }

    public LicenseExpiredException(String msg) {
        super(msg);
    }

    public LicenseExpiredException(String msg, Object extraInformation) {
        super(msg, extraInformation);
    }
}
