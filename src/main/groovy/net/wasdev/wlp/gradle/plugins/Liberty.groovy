/**
 * (C) Copyright IBM Corporation 2014, 2018.
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
package net.wasdev.wlp.gradle.plugins

import org.gradle.api.*

import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import net.wasdev.wlp.gradle.plugins.extensions.ServerExtension
import net.wasdev.wlp.gradle.plugins.tasks.StartTask
import net.wasdev.wlp.gradle.plugins.tasks.StopTask
import net.wasdev.wlp.gradle.plugins.tasks.StatusTask
import net.wasdev.wlp.gradle.plugins.tasks.CreateTask
import net.wasdev.wlp.gradle.plugins.tasks.RunTask
import net.wasdev.wlp.gradle.plugins.tasks.PackageTask
import net.wasdev.wlp.gradle.plugins.tasks.DumpTask
import net.wasdev.wlp.gradle.plugins.tasks.JavaDumpTask
import net.wasdev.wlp.gradle.plugins.tasks.DebugTask
import net.wasdev.wlp.gradle.plugins.tasks.DeployTask
import net.wasdev.wlp.gradle.plugins.tasks.UndeployTask
import net.wasdev.wlp.gradle.plugins.tasks.InstallFeatureTask
import net.wasdev.wlp.gradle.plugins.tasks.InstallLibertyTask
import net.wasdev.wlp.gradle.plugins.tasks.UninstallFeatureTask
import net.wasdev.wlp.gradle.plugins.tasks.CleanTask
import net.wasdev.wlp.gradle.plugins.tasks.InstallAppsTask
import net.wasdev.wlp.gradle.plugins.tasks.AbstractServerTask
import net.wasdev.wlp.gradle.plugins.tasks.CompileJSPTask
import org.gradle.api.tasks.bundling.War
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.logging.LogLevel
import java.util.Properties

import org.gradle.util.ConfigureUtil

class Liberty implements Plugin<Project> {

    final String JST_WEB_FACET_VERSION = '3.0'
    final String JST_EAR_FACET_VERSION = '6.0'

    void apply(Project project) {

        project.extensions.create('liberty', LibertyExtension)

        project.liberty.servers = project.container(ServerExtension)

        project.configurations.create('libertyLicense')
        project.configurations.create('libertyRuntime')

        setEclipseFacets(project)

        //Create expected server extension from liberty extension data
        project.afterEvaluate {
            if (project.liberty.server == null) {
                project.liberty.server = copyProperties(project.liberty)
            }
            //Checking serverEnv files for server properties
            Liberty.checkEtcServerEnvProperties(project)
            Liberty.checkServerEnvProperties(project.liberty.server)
            //Server objects need to be set per task after the project configuration phase
            setServersForTasks(project)

            if (!dependsOnApps(project.liberty.server)) {
                if (project.plugins.hasPlugin('war')) {
                    def tasks = project.tasks
                    tasks.getByName('libertyRun').dependsOn 'installApps'
                    tasks.getByName('libertyStart').dependsOn 'installApps'
                    tasks.getByName('libertyPackage').dependsOn 'installApps'
                }
            }
        }

        addTaskRule(project, 'Pattern: libertyCreate-<Server Name>', 'libertyCreate', CreateTask, {
            description 'Creates a WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'installLiberty'

            if (dependsOnFeature(server)) finalizedBy 'installFeature' + server.name
        })

        addTaskRule(project, 'Pattern: libertyStop-<Server Name>', 'libertyStop', StopTask, {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'libertyStop')
        })

        addTaskRule(project, 'Pattern: libertyStart-<Server Name>', 'libertyStart', StartTask, {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate-' + server.name

            if (dependsOnApps(server)) dependsOn 'installApps-' + server.name
        })

        addTaskRule(project, 'Pattern: installApps-<Server Name>', 'installApps', InstallAppsTask, {
            dependsOn 'libertyCreate-' + server.name, project.tasks.withType(War)

            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
            group 'Liberty'

            outputs.dir { new File(getServerDir(project), 'apps') }
            outputs.dir { new File(getServerDir(project), 'dropins') }
        })

        addTaskRule(project, 'Pattern: installFeature-<Server Name>', 'installFeature', InstallFeatureTask, {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'

            if (dependsOnFeature(server)) {
                dependsOn 'libertyCreate-' + server.name
            } else {
                dependsOn 'installLiberty'
            }
        })

        addTaskRule(project, 'Pattern: uninstallFeature-<Server Name>', 'uninstallFeature', UninstallFeatureTask, {
            description 'Uninstall a feature from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule(project, 'Pattern: compileJSP-<Server Name>', 'compileJSP', CompileJSPTask, {
            description 'Compile the JSP files in the src/main/webapp directory. '
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'installLiberty', 'compileJava'
        })

        addTaskRule(project, 'Pattern: libertyRun-<Server Name>', 'libertyRun', RunTask, {
            description = "Runs a Websphere Liberty Profile server under the Gradle process."
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyCreate-' + server.name
            if (dependsOnApps(server)) dependsOn 'installApps-' + server.name
        })

        addTaskRule(project, 'Pattern: libertyStatus-<Server Name>', 'libertyStatus', StatusTask, {
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule(project, 'Pattern: libertyPackage-<Server Name>', 'libertyPackage', PackageTask, {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.DEBUG
            group 'Liberty'
            dependsOn installDependsOn(server, 'installLiberty')
        })

        addTaskRule(project, 'Pattern: libertyDump-<Server Name>', 'libertyDump', DumpTask, {
            description 'Dumps diagnostic information from the Liberty Profile server into an archive.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule(project, 'Pattern: libertyJavaDump-<Server Name>', 'libertyJavaDump', JavaDumpTask, {
            description 'Dumps diagnostic information from the Liberty Profile server JVM.'
            logging.level = LogLevel.INFO
            group 'Liberty'
        })

        addTaskRule(project, 'Pattern: deploy-<Server Name>', 'deploy', DeployTask, {
            description 'Deploys a supported file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyStart-'+ server.name
        })

        addTaskRule(project, 'Pattern: undeploy-<Server Name>', 'deploy', DeployTask, {
            description 'Removes an application from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'libertyStart-'+ server.name
        })

        addTaskRule(project, 'Pattern: cleanDirs-<Server Name>', 'cleanDirs', CleanTask, {
            description 'Deletes files from some directories from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn 'cleanDirs-' + server.name
        })

        project.task('compileJSP') {
            description 'Compile the JSP files in the src/main/webapp directory. '
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'compileJSP')
        }

        project.task('installLiberty', type: InstallLibertyTask) {
            description 'Installs Liberty from a repository'
            logging.level = LogLevel.INFO
            group 'Liberty'
        }

        //This is a hard one
        project.task('libertyRun') {
            description = "Runs a Websphere Liberty Profile server under the Gradle process."
            logging.level = LogLevel.INFO
            group 'Liberty'
            // dependsOn 'libertyCreate'
            //
            // project.afterEvaluate {
            //     if (dependsOnApps(server)) dependsOn 'installApps'
            // }
        }

        project.task('libertyStatus') {
            description 'Checks if the Liberty server is running.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'libertyStatus')
        }

        project.task('libertyCreate') {
            description 'Creates a WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'

            project.afterEvaluate {
                List<String> libertyCreateTasks = getTaskList(project, 'libertyCreate')
                dependsOn libertyCreateTasks

                outputs.upToDateWhen {
                    tasksUpToDate(project, libertyCreateTasks)
                }
            }
        }

        project.task('libertyStart') {
            description 'Starts the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'libertyStart')
        }

        project.task('libertyStop') {
            description 'Stops the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'libertyStop')
        }

        project.task('libertyPackage') {
            description 'Generates a WebSphere Liberty Profile server archive.'
            logging.level = LogLevel.DEBUG
            group 'Liberty'
            dependsOn getTaskList(project, 'libertyPackage')
        }

        project.task('libertyDump') {
            description 'Dumps diagnostic information from the Liberty Profile server into an archive.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'libertyDump')
        }

        project.task('libertyJavaDump') {
            description 'Dumps diagnostic information from the Liberty Profile server JVM.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'libertyJavaDump')
        }

        project.task('libertyDebug', type: DebugTask) {
            description 'Runs the Liberty Profile server in the console foreground after a debugger connects to the debug port (default: 7777).'
            logging.level = LogLevel.INFO
            group 'Liberty'
            //This one is kinda tricky. We run it on 7777 unless that is configured so it can't be run at the same time for multiple servers
        }

        project.task('deploy') {
            description 'Deploys a supported file to the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'deploy')
        }

        project.task('undeploy') {
            description 'Removes an application from the WebSphere Liberty Profile server.'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'undeploy')
        }

        project.task('installFeature') {
            description 'Install a new feature to the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'installFeature')
        }

        project.task('uninstallFeature') {
            description 'Uninstall a feature from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'uninstallFeature')
        }

        project.task('cleanDirs') {
            description 'Deletes files from some directories from the WebSphere Liberty Profile server'
            logging.level = LogLevel.INFO
            group 'Liberty'
            dependsOn getTaskList(project, 'cleanDirs')
        }

        project.task('installApps') {
            description "Copy applications generated by the Gradle project to a Liberty server's dropins or apps directory."
            logging.level = LogLevel.INFO
            group 'Liberty'
            project.afterEvaluate {
                List<String> installAppsTasks = getTaskList(project, 'installApps')
                dependsOn project.tasks.withType(War), installAppsTasks
                outputs.upToDateWhen {
                    tasksUpToDate(project, installAppsTasks)
                }
            }
        }
    }

    void addTaskRule (Project project, String pattern, String name, Class taskType, Closure configureClosure) {
        project.tasks.addRule(pattern) { String taskName ->
            if (taskName.startsWith(name)) {
                project.task(taskName, type: taskType) {
                    server = project.liberty.servers.getByName(taskName - "${name}-")
                    ConfigureUtil.configure(configureClosure, it)
                }
            }
        }
    }

    boolean tasksUpToDate (Project project, List<String> taskList) {
        taskList.each {
            if(!project.tasks.getByName(it).getUpToDate()){
                return false
            }
        }
        return true
    }

    private void setEclipseFacets(Project project) {
        //Used to set project facets in Eclipse
        project.pluginManager.apply('eclipse-wtp')
        project.tasks.getByName('eclipseWtpFacet').finalizedBy 'libertyCreate'

        //Uplift the jst.web facet version to 3.0 if less than 3.0 so WDT can deploy properly to Liberty.
        //There is a known bug in the wtp plugin that will add duplicate facets, the first of the duplicates is honored.
        project.tasks.getByName('eclipseWtpFacet').facet.file.whenMerged {
            if(project.plugins.hasPlugin('war')) {
                setFacetVersion(project, 'jst.web', JST_WEB_FACET_VERSION)
            } else if(project.plugins.hasPlugin('ear')) {
                setFacetVersion(project, 'jst.ear', JST_EAR_FACET_VERSION)
            }
        }

        if (project.plugins.hasPlugin('ear')) {
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

    private ServerExtension copyProperties(LibertyExtension liberty) {
        def serverMap = new ServerExtension().getProperties()
        def libertyMap = liberty.getProperties()

        serverMap.keySet().each { String element ->
            if (element.equals("name")) {
                serverMap.put(element, libertyMap.get("serverName"))
            }
            else {
                serverMap.put(element, libertyMap.get(element))
            }
        }
        serverMap.remove('class')
        serverMap.remove('outputDir')

        return ServerExtension.newInstance(serverMap)
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

    public static void checkServerEnvProperties(ServerExtension server) {
        if (server.outputDir == null) {
            Properties envProperties = new Properties()
            //check server.env files and set liberty.server.outputDir
            if (server.configDirectory != null) {
                File serverEnvFile = new File(server.configDirectory, 'server.env')
                if (serverEnvFile.exists()) {
                    serverEnvFile.text = serverEnvFile.text.replace("\\", "/")
                    envProperties.load(new FileInputStream(serverEnvFile))
                    Liberty.setServerOutputDir(server, (String) envProperties.get("WLP_OUTPUT_DIR"))
                }
            } else if (server.serverEnv.exists()) {
                server.serverEnv.text = server.serverEnv.text.replace("\\", "/")
                envProperties.load(new FileInputStream(server.serverEnv))
                Liberty.setServerOutputDir(server, (String) envProperties.get("WLP_OUTPUT_DIR"))
            }
        }
    }

    private static void setLibertyOutputDir(Project project, String envOutputDir){
        if (envOutputDir != null) {
            project.liberty.outputDir = envOutputDir
        }
    }

    private static void setServerOutputDir(ServerExtension server, String envOutputDir){
        if (envOutputDir != null) {
            server.outputDir = envOutputDir
        }
    }

    private void setServersForTasks(Project project){
        project.tasks.withType(AbstractServerTask).each { task ->
            task.server = project.liberty.server
        }
    }

    private List<String> installDependsOn(ServerExtension server, String elseDepends) {
        List<String> tasks = new ArrayList<String>()
        boolean apps = dependsOnApps(server)
        boolean feature = dependsOnFeature(server)

        if (apps) tasks.add('installApps')
        if (feature) tasks.add('installFeature')
        if (!apps && !feature) tasks.add(elseDepends)
        return tasks
    }

    private List<String> getTaskList (Project project, String taskName) {
        List<String> tasks = new ArrayList<String>()
        project.liberty.servers.each {
            tasks.add(taskName + '-' + it.name)
        }

        return tasks
    }

    private boolean dependsOnApps(ServerExtension server) {
        return ((server.apps != null && !server.apps.isEmpty()) ||
                (server.dropins != null && !server.dropins.isEmpty()))
    }

    private boolean dependsOnFeature(ServerExtension server) {
        return (server.features.name != null && !server.features.name.isEmpty())
    }

    private static File getInstallDir(Project project) {
        if (project.liberty.installDir == null) {
           if (project.liberty.install.baseDir == null) {
               return new File(project.buildDir, 'wlp')
           } else {
               return new File(project.liberty.install.baseDir, 'wlp')
           }
        } else {
           return new File(project.liberty.installDir)
        }
    }
}
