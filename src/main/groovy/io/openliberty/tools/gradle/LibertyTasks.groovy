/*
 * (C) Copyright IBM Corporation 2018, 2021.
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

import io.openliberty.tools.gradle.extensions.ServerExtension
import io.openliberty.tools.gradle.tasks.AbstractServerTask

import org.gradle.api.Project
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.bundling.War
import org.gradle.api.Task

public class LibertyTasks {
    Project project

    LibertyTasks (Project project) {
        this.project = project
    }

    public void applyTasks() {
        project.compileJSP {
            dependsOn 'installLiberty', 'compileJava'
        }

        project.libertyRun {
            dependsOn 'libertyCreate'

            if (dependsOnApps(project.liberty.server)) {
                dependsOn 'deploy'
            }
        }

        project.libertyStatus {
            dependsOn 'libertyCreate'
        }

        project.libertyCreate {
            dependsOn 'installLiberty'   
            // Run install features if configured
            finalizedBy 'installFeature'      
        }

        project.libertyStart {
            dependsOn 'libertyCreate'

            if (dependsOnApps(project.liberty.server)) {
                dependsOn 'deploy'
            }
        }

        project.libertyPackage {
            dependsOn installDependsOn(project.liberty.server)
        }

        project.undeploy {
            dependsOn 'libertyStart'
        }

        project.installFeature {
            dependsOn 'libertyCreate'
			if (project.configurations.featuresBom.dependencies) {
				dependsOn 'prepareFeature'
			}
        }

        project.cleanDirs {
            dependsOn 'libertyStop'
        }
		
		project.prepareFeature {
			dependsOn 'installLiberty'		
		}

        project.deploy {
            if (AbstractServerTask.findSpringBootVersion(project) != null) {
                if (springBootVersion?.startsWith('2')) {
                    dependsOn 'bootJar'
                } else { //version 1.5.x
                    dependsOn 'bootRepackage'
                }
            }
            dependsOn project.tasks.withType(War), 'libertyCreate'
        }

        project.configureArquillian {
            dependsOn 'deploy', 'processTestResources'
            skipIfArquillianXmlExists = project.arquillianConfiguration.skipIfArquillianXmlExists
            arquillianProperties = project.arquillianConfiguration.arquillianProperties
        }

        if (!dependsOnApps(project.liberty.server)) {
            if (project.plugins.hasPlugin('war') || project.plugins.hasPlugin('ear')) {
                def tasks = project.tasks
                tasks.getByName('libertyRun').dependsOn 'deploy'
                tasks.getByName('libertyStart').dependsOn 'deploy'
                tasks.getByName('libertyPackage').dependsOn 'deploy'
            }
        }

        checkServerEnvProperties(project.liberty.server)
        //Server objects need to be set per task after the project configuration phase
        setServersForTasks()
    }

    private void setServersForTasks(){
        project.tasks.withType(AbstractServerTask).each { task ->
            task.server = project.liberty.server
        }
    }

    protected List<String> installDependsOn(ServerExtension server) {
        List<String> tasks = new ArrayList<String>()
        boolean apps = dependsOnApps(server)

        if (apps) { 
            tasks.add('deploy')
        }
        tasks.add('installFeature')
        return tasks
    }

    protected boolean dependsOnApps(ServerExtension server) {
        return ((server.deploy.apps != null && !server.deploy.apps.isEmpty()) || (server.deploy.dropins != null && !server.deploy.dropins.isEmpty()))
    }

    public void checkServerEnvProperties(ServerExtension server) {
        if (server.outputDir == null) {
            Properties envProperties = new Properties()
            //check server.env files and set liberty.server.outputDir
            if (server.serverEnvFile != null && server.serverEnvFile.exists()) {
                server.serverEnvFile.text = server.serverEnvFile.text.replace("\\", "/")
                envProperties.load(new FileInputStream(server.serverEnvFile))
                setServerOutputDir(server, (String) envProperties.get("WLP_OUTPUT_DIR"))
            } else if (server.configDirectory != null) {
                File serverEnvFile = new File(server.configDirectory, 'server.env')
                if (serverEnvFile != null && serverEnvFile.exists()) {
                    serverEnvFile.text = serverEnvFile.text.replace("\\", "/")
                    envProperties.load(new FileInputStream(serverEnvFile))
                    setServerOutputDir(server, (String) envProperties.get("WLP_OUTPUT_DIR"))
                }
            }
        }
    }

    private void setServerOutputDir(ServerExtension server, String envOutputDir){
        if (envOutputDir != null) {
            server.outputDir = envOutputDir
        }
    }
}
