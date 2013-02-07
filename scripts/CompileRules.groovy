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

/*
    Runtime classpath for compiling drools rules, cannot contain the
    drools jars due to a classloader bug in the rulebase task.
 */
droolsCompileClasspath = {
    commonClasspath.delegate = delegate
    commonClasspath.call()

    def dependencies = grailsSettings.runtimeDependencies
    if (dependencies) {
        for (File f in dependencies) {
            if (f && !f.name.matches("drools-.*")) {
                pathelement(location: f.absolutePath)
            }
        }
    }

    pathelement(location: "${pluginClassesDir.absolutePath}")
    pathelement(location: "${classesDir.absolutePath}")
}

getRulesSourceFiles = {
    def drlFiles = resolveResources("file:${basedir}/descriptors/rules/*.drl").toList()
    def xlsFiles = resolveResources("file:${basedir}/descriptors/rules/*.xls").toList();

    return drlFiles + xlsFiles
}

getPkgFileName = { f ->
    return f.file.name.replaceAll("(\\.drl|\\.xls)", ".pkg")
}

target(compileRules: "Compiles DROOLS binary rules packages.") {
    ant.taskdef(name: "rulebase", classname: "org.drools.contrib.DroolsCompilerAntTask")
    ant.path(id: "drools.compile.classpath", droolsCompileClasspath)

    delete(dir: "./resources/rules")
    mkdir(dir: "./resources/rules")

    getRulesSourceFiles().each { f ->
        def pkgName = getPkgFileName(f)
        println "Compiling rules file ${f.file.name} to ${pkgName} ..."

        rulebase(srcdir: "./descriptors/rules/",
                 tofile: "./resources/rules/${pkgName}",
                 classpathref: "drools.compile.classpath",
                 binformat: "package") {

            include(name: f.file.name)
        }
    }
}

setDefaultTarget(compileRules)
