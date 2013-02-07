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

package com.sapienter.jbilling.client.util

import grails.orm.HibernateCriteriaBuilder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

/**
 * Sortable 
 *
 * @author Brian Cowdery
 * @since 08/06/11
 */
class SortableCriteria {

    static def sort(GrailsParameterMap params, builder) {
        def sort = params.sort?.tokenize(',')?.collect { it.trim() }

        if (params.alias) {
            // explicit alias definitions
            params.alias.each { alias, aliasPath ->
                builder.createAlias(aliasPath, alias)
            }

        } else {
            // try and automatically add aliases for sorted associations
            def associations = sort.findAll{ it.contains('.') }
            associations.collect{ it.substring(0, it.indexOf('.')) }.unique().each {
                builder.createAlias(it, it)
            }
        }

        // add order by clauses
        sort.each {
            builder.order(it, params.order)
        }
    }

}

