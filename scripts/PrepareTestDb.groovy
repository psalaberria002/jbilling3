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

includeTargets << grailsScript("Init")

target(prepareTestDb: "Import the test postgresql database.") {

    // load the client reference DB if it exists
    def testDb = new File("${basedir}/sql/jbilling_test.sql")
    def referenceDb = new File("${basedir}/data.sql")
    def file = referenceDb.exists() ? referenceDb : testDb

    // optionally accept database name and user name arguments
    parseArguments();
    def cleanDb = argsMap.cleanDb;
    def username = argsMap.user ? argsMap.user : "jbilling"
    def database = argsMap.db ? argsMap.db : "jbilling_test"

    if (cleanDb) {
        println "Dropping a database: ${database}..."
        // call postgresl to drop database
        exec(executable: "dropdb", failonerror: false) {
            arg(line: "-U ${username} -e ${database}")
        }
        println "Done."

        println "Creating a database: ${database}..."
        // call postgresl to create database
        exec(executable: "createdb", failonerror: true) {
            arg(line: "-U ${username} -O ${username} -E UTF-8 -e ${database}")
        }
        println "Done."
    }

    println "Importing file '${file.name}' into the ${database} database (user: ${username})"
    // call postgresl to load the database
    exec(executable: "psql", failonerror: false) {
        arg(line: "-U ${username} -f ${file.path} ${database}")
    }

    println "Done."
}

setDefaultTarget(prepareTestDb)
