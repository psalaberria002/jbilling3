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

package com.sapienter.jbilling.client.user

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import com.sapienter.jbilling.server.user.UserWS
import org.codehaus.groovy.grails.web.metaclass.BindDynamicMethod
import com.sapienter.jbilling.server.entity.CreditCardDTO
import com.sapienter.jbilling.server.user.CreditCardBL
import com.sapienter.jbilling.common.Constants
import com.sapienter.jbilling.server.user.ContactWS
import com.sapienter.jbilling.server.user.db.CompanyDTO
import com.sapienter.jbilling.server.entity.AchDTO
import org.apache.log4j.Logger
import org.springframework.security.authentication.encoding.PasswordEncoder
import com.sapienter.jbilling.server.util.Context

/**
 * UserHelper 
 *
 * @author Brian Cowdery
 * @since 04/04/11
 */
class UserHelper {

    private static def log = Logger.getLogger(this)

    /**
     * Constructs a UserWS object and populates it with data from the given parameter map. The user
     * and all associated objects (ContactWS, CreditCarDTO, AchDTO, custom contact fields etc.) are
     * also bound as needed.
     *
     * @param user user object to bind parameters to
     * @param params parameters to bind
     * @return bound UserWS object
     */
    static def UserWS bindUser(UserWS user, GrailsParameterMap params) {
        bindData(user, params, 'user')

        // default main role to TYPE_CUSTOMER if not set
        if (!user.mainRoleId) {
            user.setMainRoleId(Constants.TYPE_CUSTOMER)
        }

        log.debug("User ${user}")

        // bind credit card object if parameters present
        if (!params.deleteCreditCard && params.creditCard.any { key, value -> value }) {
            def creditCard = new CreditCardDTO()
            bindData(creditCard, params, 'creditCard')
            bindExpiryDate(creditCard, params)

            user.setCreditCard(creditCard)
//            if (!creditCard.number.startsWith('*')) {
//                // update credit card only if not obscured
//            } else {
//                // or only if we have an ID for the existing card
//                // in this case, pull the original number from the users existing card
//                if (creditCard.id) {
//                    def existingCard =  new CreditCardBL(creditCard.id).getEntity()?.getOldDTO();
//                    if (existingCard) {
//                        //if changed the cc.hasChanged =  true
//                        creditCard.hasChanged = true
//                        creditCard.name = existingCard.getName()
//                        creditCard.expiry = bindExpiryDate(existingCard,params)
//                        creditCard.number = existingCard.getNumber()
//                        user.setCreditCard(creditCard)
//                    }
//                }
//            }

            log.debug("Credit card ${creditCard}")
        }

        // bind ach object if parameters present
        if (!params.deleteAch && params.ach.any { key, value -> value }) {
            def ach = new AchDTO()
            bindData(ach, params, 'ach')
            user.setAch(ach)

            log.debug("Ach ${ach}")
        }

        return user
    }

    /**
     * Binds user contacts. The given UserWS object will be populated with the primary contact type, and the
     * given list will be populated with all the contacts.
     *
     * @param user user object to bind primary contact to
     * @param contacts list to populate with remaining secondary contacts
     * @param company company
     * @param params parameters to bind
     * @return list of bound contact types
     */
    static def Object[] bindContacts(UserWS user, List contacts, CompanyDTO company, GrailsParameterMap params) {
        def contactTypes = company.contactTypes
        def primaryContactTypeId = params.int('primaryContactTypeId')

        // bind primary user contact and custom contact fields
        def primaryContact = new ContactWS()
        bindData(primaryContact, params, "contact-${primaryContactTypeId}")
        primaryContact.type = primaryContactTypeId

        // manually bind primary contact "include in notifications" flag
        primaryContact.include = params."contact-${primaryContactTypeId}".include != null ? 1 : 0

        if (params.contactField) {
            primaryContact.fieldIDs = new Integer[params.contactField.size()]
            primaryContact.fieldValues = new Integer[params.contactField.size()]
            params.contactField.eachWithIndex { id, value, i ->
                primaryContact.fieldIDs[i] = id.toInteger()
                primaryContact.fieldValues[i] = value
            }
        }

        user.setContact(primaryContact)
        contacts << primaryContact

        log.debug("Primary contact (type ${primaryContactTypeId}): ${primaryContact}")

        // bind secondary contact types
        contactTypes.findAll { it.id != primaryContactTypeId }.each {
            // bind if contact object if parameters present
            if (params["contact-${it.id}"].any { key, value -> value }) {
                def otherContact = new ContactWS()
                bindData(otherContact, params, "contact-${it.id}")
                otherContact.type = it.id

                // manually bind secondary contact "include in notifications" flag
                otherContact.include = params."contact-${it.id}".include != null ? 1 : 0

                contacts << otherContact;
            }
        }

        log.debug("Secondary contacts: ${contacts}")

        return contacts;
    }


    /**
     * Binds the password parameters to the given new user object, ensuring that the password entered is
     * valid and that if the user already exists that the old password is verified before changing.
     *
     * @param newUser user to bind password to
     * @param oldUser existing user (may be null)
     * @param params parameters to bind
     */
    static def bindPassword(UserWS newUser, UserWS oldUser, GrailsParameterMap params, flash) {
        if (oldUser) {
            // validate that the entered confirmation password matches the users existing password
            if (params.newPassword) {
                PasswordEncoder passwordEncoder = Context.getBean(Context.Name.PASSWORD_ENCODER)
                if (!passwordEncoder.isPasswordValid(oldUser.password, params.oldPassword, null)) {
                    flash.error = 'current.password.doesnt.match.existing'
                    return
                }
            }
        }

        // verify passwords
        if (params.newPassword == params.verifiedPassword) {
            if (params.newPassword) newUser.setPassword(params.newPassword)

        } else {
            flash.error = 'passwords.dont.match'
        }
    }

    private static def bindExpiryDate(CreditCardDTO creditCard, GrailsParameterMap params) {
        Integer expiryMonth = params.int('expiryMonth')
        Integer expiryYear = params.int('expiryYear')

        if (expiryMonth && expiryYear)  {
            Calendar calendar = Calendar.getInstance()
            calendar.clear()
            calendar.set(Calendar.MONTH, expiryMonth - 1)
            calendar.set(Calendar.YEAR, expiryYear)
            calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            
            creditCard.expiry = calendar.getTime()
        }
    }

    private static def bindData(Object model, modelParams, String prefix) {
        def args = [ model, modelParams, [exclude:[], include:[]]]
        if (prefix) args << prefix

        new BindDynamicMethod().invoke(model, 'bind', (Object[]) args)
    }

    static def getDisplayName(user, contact) {
        if (contact?.firstName || contact?.lastName) {
            return "${contact.firstName} ${contact.lastName}".trim()

        } else if (contact?.organizationName) {
            return "${contact.organizationName}".trim()
        }

        return user?.userName
    }
}
