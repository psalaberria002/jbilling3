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

includeTargets << grailsScript("War")

includeTargets << new File("${basedir}/scripts/CopyResources.groovy")
includeTargets << new File("${basedir}/scripts/CompileDesigns.groovy")
includeTargets << new File("${basedir}/scripts/CompileReports.groovy")
includeTargets << new File("${basedir}/scripts/CompileRules.groovy")
includeTargets << new File("${basedir}/scripts/Jar.groovy")

resourcesDir = "${basedir}/resources"
descriptorsDir = "${basedir}/descriptors"
configDir = "${basedir}/grails-app/conf"
sqlDir = "${basedir}/sql"
javaDir = "${basedir}/src/java"
targetDir = "${basedir}/target"

releaseName = "${grailsAppName}-${grailsAppVersion}"
packageName = "${targetDir}/${releaseName}.zip"

target(prepareRelease: "Builds the war and all necessary resources.") {
    copyResources()
    compileDesigns()
    compileReports()
    compileRules()
    jar()
    war()
}

target(packageRelease: "Builds the war and packages all the necessary config files and resources in a release zip file.") {
    depends(prepareRelease)

    // zip up resources into a release package
    delete(dir: targetDir, includes: "${grailsAppName}-*.zip")

    zip(filesonly: false, update: false, destfile: packageName) {
        zipfileset(dir: resourcesDir, prefix: "resources")
        zipfileset(dir: targetDir, includes: "${grailsAppName}.jar", prefix: "resources/api")
        zipfileset(dir: javaDir, includes: "jbilling.properties.sample", fullpath: "jbilling.properties")
        zipfileset(dir: configDir, includes: "Config.groovy", fullpath: "${grailsAppName}-Config.groovy")
        zipfileset(dir: configDir, includes: "DataSource.groovy", fullpath: "${grailsAppName}-DataSource.groovy")
        zipfileset(dir: targetDir, includes: "${grailsAppName}.war")
        zipfileset(dir: sqlDir, includes: "jbilling_test.sql")
    }

    println "Packaged release to ${packageName}"
}

setDefaultTarget(packageRelease)
