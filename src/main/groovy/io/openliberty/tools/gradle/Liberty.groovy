/**
 * (C) Copyright IBM Corporation 2014, 2021.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.gradle

import org.gradle.api.*

import io.openliberty.tools.gradle.extensions.DeployExtension
import io.openliberty.tools.gradle.extensions.LibertyExtension
import io.openliberty.tools.gradle.extensions.ServerExtension
import io.openliberty.tools.gradle.extensions.arquillian.ArquillianExtension

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.Test

import java.util.Properties
import java.text.MessageFormat

class Liberty implements Plugin<Project> {

    final String JST_WEB_FACET_VERSION = '3.0'
    final String JST_EAR_FACET_VERSION = '6.0'

    void apply(Project project) {
        project.extensions.create('liberty', LibertyExtension)
        project.extensions.create('arquillianConfiguration', ArquillianExtension)

        project.configurations.create('libertyLicense')
        project.configurations.create('libertyRuntime')
        project.configurations.create('libertyFeature')
		project.configurations.create('featuresBom')
        project.configurations.create('libertyApp')
        if (project.configurations.find { it.name == 'compileOnly' }) {
            project.configurations.libertyFeature.extendsFrom(project.configurations.compileOnly)
        }

        //Used to set project facets in Eclipse
        project.pluginManager.apply('eclipse-wtp')
        project.tasks.getByName('eclipseWtpFacet').finalizedBy 'libertyCreate'

        new LibertyTaskFactory(project).createTasks()

        //Create expected server extension from liberty extension data
        project.afterEvaluate {
            setEclipseFacets(project)
            new LibertyTasks(project).applyTasks()

            //Checking serverEnv files for server properties
            Liberty.checkEtcServerEnvProperties(project)

            setEclipseClasspath(project)
            setDevProperties(project)
        }

        // Dev-mode needs to propagate these system properties from the gradle JVM
        // to the JVM that will be used to run the tests.
        def propagatedSystemProperties = [
            "liberty.hostname",
            "liberty.http.port",
            "liberty.https.port",
            "microshed_hostname",
            "microshed_http_port",
            "microshed_https_port",
            "wlp.user.dir"
        ];
        project.tasks.withType(Test) { testTask ->
            propagatedSystemProperties.each { propertyKey ->
                def propertyValue = System.getProperty(propertyKey);
                if (propertyValue != null) {
                    testTask.systemProperty(propertyKey, propertyValue);
                }
            }
        }
    }

    private void setEclipseFacets(Project project) {
        //Uplift the jst.web facet version to 3.0 if less than 3.0 so WDT can deploy properly to Liberty.
        //There is a known bug in the wtp plugin that will add duplicate facets, the first of the duplicates is honored.
        if(project.plugins.hasPlugin('war')) {
            setFacetVersion(project, 'jst.web', JST_WEB_FACET_VERSION)
        }

        if (project.plugins.hasPlugin('ear')) {
            setFacetVersion(project, 'jst.ear', JST_EAR_FACET_VERSION)
            project.getGradle().getTaskGraph().whenReady {
                Dependency[] deps = project.configurations.deploy.getAllDependencies().toArray()
                deps.each { Dependency dep ->
                    if (dep instanceof ProjectDependency) {
                        def projectDep = dep.getDependencyProject()
                        if (projectDep.plugins.hasPlugin('war')) {
                            setFacetVersion(projectDep, 'jst.web', JST_WEB_FACET_VERSION)
                        }
                    }
                }
            }
        }
    }

    private void setDevProperties(Project project) {
        // At plugin start up copy the command line property into the extension as if the ext. had been set in the build file.
        boolean container = project.findProperty('dev_mode_container') //null value sets boolean to false
        if(container) {
            project.liberty.dev.container = true
        }
    }

    protected void setFacetVersion(Project project, String facetName, String version) {
        if(project.plugins.hasPlugin('eclipse-wtp')) {
            project.tasks.getByName('eclipseWtpFacet').facet.file.whenMerged {
                def jstFacet = facets.find { it.type.name() == 'installed' && it.name == facetName && Double.parseDouble(it.version) < Double.parseDouble(version) }
                if (jstFacet != null) {
                    jstFacet.version = version
                }
            }
        }
    }
    
    protected void setEclipseClasspath(Project project) {
        if(project.plugins.hasPlugin('war')) {
            //Configuring the Eclipse classpath to use the same directory as the war plugin for its output
            //Using the default war/java plugin value
            File warTaskOutput = new File("build/classes/java/main")
            project.eclipse.classpath {
                defaultOutputDir = warTaskOutput
                file.whenMerged {
                    entries.each {
                        source ->
                            if (source.kind == 'src' && source.hasProperty('output') && source.output == 'bin/main') {
                                source.output = warTaskOutput
                            }
                    }
                }
            }
        }
    }

    public static void checkEtcServerEnvProperties(Project project) {
        if (project.liberty.outputDir == null) {
            Properties envProperties = new Properties()
            //check etc/server.env and set liberty.outputDir
            File serverEnvFile = new File(Liberty.getInstallDir(project), 'etc/server.env')
            if (serverEnvFile.exists()) {
                serverEnvFile.text = serverEnvFile.text.replace("\\", "/")
                envProperties.load(new FileInputStream(serverEnvFile))
                Liberty.setLibertyOutputDir(project, (String) envProperties.get("WLP_OUTPUT_DIR"))
            }
        }
    }

    private static void setLibertyOutputDir(Project project, String envOutputDir){
        if (envOutputDir != null) {
            project.liberty.outputDir = envOutputDir
        }
    }

    private static File getInstallDir(Project project) {
        if (project.liberty.installDir == null) {
           if (project.liberty.baseDir == null) {
               return new File(project.buildDir, 'wlp')
           } else {
               return new File(project.liberty.baseDir, 'wlp')
           }
        } else { // installDir is specified
            String installDirPath = checkAndAppendWlp(project)
            File installDir = new File(installDirPath)

            return installDir;
        }
    }

    private static String checkAndAppendWlp(Project project) {
        String installDir = project.liberty.installDir
        if (installDir.endsWith("wlp")) {
            return installDir
        } else { // not valid wlp dir
            project.getLogger().warn(MessageFormat.format("The installDir {0} path does not reference a wlp folder. Using path {0}/wlp instead.", installDir))
            return new File(installDir, 'wlp').toString()
        }
    }
}
