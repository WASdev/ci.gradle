/**
 * (C) Copyright IBM Corporation 2014, 2017.
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
package net.wasdev.wlp.gradle.plugins.tasks

import net.wasdev.wlp.gradle.plugins.extensions.DeployExtension
import net.wasdev.wlp.gradle.plugins.extensions.LibertyExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class AbstractTask extends DefaultTask {

    private final String HEADER = "# Generated by liberty-gradle-plugin"

    protected void executeServerCommand(Project project, String command, Map<String, String> params) {
        project.ant.taskdef(name: 'server', 
                            classname: 'net.wasdev.wlp.ant.ServerTask', 
                            classpath: project.buildscript.configurations.classpath.asPath)
        params.put('operation', command)
        project.ant.server(params)
    }

    protected Map<String, String> buildLibertyMap(Project project) {
        Map<String, String> result = new HashMap();
        result.put('serverName', project.liberty.serverName)

        def installDir = getInstallDir(project)
        result.put('installDir', installDir)

        def userDir = getUserDir(project, installDir)
        result.put('userDir', userDir)

        if (project.liberty.outputDir != null) {
            result.put('outputDir', project.liberty.outputDir)
        }          
        if (project.liberty.timeout != null && !project.liberty.timeout.isEmpty()) {
            result.put('timeout', project.liberty.timeout)
        }

        return result;
    }

    protected File getInstallDir(Project project) {
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

    protected File getUserDir(Project project) {
        return getUserDir(project, getInstallDir(project))
    }

    protected File getUserDir(Project project, File installDir) {
        return (project.liberty.userDir == null) ? new File(installDir, 'usr') : new File(project.liberty.userDir)
    }

    /**
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected void copyConfigFiles() throws IOException {
        String serverDirectory
        /*if(project.liberty.outputDir == null){
            project.liberty.outputDir = getInstallDir(project).toString() + "/usr/servers/" + project.liberty.serverName
        }*/
        
        if(project.liberty.outputDir !=null && !project.liberty.outputDir.isEmpty()){
            serverDirectory = project.liberty.outputDir
        }
        else{ 
            serverDirectory = getInstallDir(project).toString() + "/usr/servers/" + project.liberty.serverName
        }
        
        
        String serverXMLPath = null
        String jvmOptionsPath = null
        String bootStrapPropertiesPath = null
        String serverEnvPath = null

        if (project.liberty.configDirectory != null) {
            if(project.liberty.configDirectory.exists()){
                // copy configuration files from configuration directory to server directory if end-user set it
                def copyAnt = new AntBuilder()
                copyAnt.copy(todir: serverDirectory) {
                    fileset(dir: project.liberty.configDirectory.getCanonicalPath())
                }

                File configDirServerXML = new File(project.liberty.configDirectory, "server.xml")
                if (configDirServerXML.exists()) {
                    serverXMLPath = configDirServerXML.getCanonicalPath()
                }

                File configDirJvmOptionsFile = new File(project.liberty.configDirectory, "jvm.options")
                if (configDirJvmOptionsFile.exists()) {
                    jvmOptionsPath = configDirJvmOptionsFile.getCanonicalPath()
                }

                File configDirBootstrapFile = new File(project.liberty.configDirectory, "bootstrap.properties")
                if (configDirBootstrapFile.exists()) {
                    bootStrapPropertiesPath = configDirBootstrapFile.getCanonicalPath()
                }

                File configDirServerEnv = new File(project.liberty.configDirectory, "server.env")
                if (configDirServerEnv.exists()) {
                    serverEnvPath = configDirServerEnv.getCanonicalPath()
                }
            }
            else{
                println('WARNING: The configDirectory attribute was configured but the directory is not found: ' + project.liberty.configDirectory.getCanonicalPath())
            }
        } 

        // handle server.xml if not overwritten by server.xml from configDirectory
        if (serverXMLPath == null || serverXMLPath.isEmpty()) {
            // copy configuration file to server directory if end-user set it.
            if (project.liberty.configFile != null && project.liberty.configFile.exists()) {
                Files.copy(project.liberty.configFile.toPath(), new File(serverDirectory, "server.xml").toPath(), StandardCopyOption.REPLACE_EXISTING)
                serverXMLPath = project.liberty.configFile.getCanonicalPath()
            }
        }

        // handle jvm.options if not overwritten by jvm.options from configDirectory
        if (jvmOptionsPath == null || jvmOptionsPath.isEmpty()) {
            File optionsFile = new File(serverDirectory, "jvm.options")
            if(project.liberty.jvmOptions != null && !project.liberty.jvmOptions.isEmpty()){
                writeJvmOptions(optionsFile, project.liberty.jvmOptions)
                jvmOptionsPath = "inlined configuration"
            } else if (project.liberty.jvmOptionsFile != null && project.liberty.jvmOptionsFile.exists()) {
                Files.copy(project.liberty.jvmOptionsFile.toPath(), optionsFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                jvmOptionsPath = project.liberty.jvmOptionsFile.getCanonicalPath()
            }
        }
        
        // handle bootstrap.properties if not overwritten by bootstrap.properties from configDirectory
        if (bootStrapPropertiesPath == null || bootStrapPropertiesPath.isEmpty()) {
            File bootstrapFile = new File(serverDirectory, "bootstrap.properties")
            if(project.liberty.bootstrapProperties != null && !project.liberty.bootstrapProperties.isEmpty()){
                writeBootstrapProperties(bootstrapFile, project.liberty.bootstrapProperties)
                bootStrapPropertiesPath = "inlined configuration"
            } else if (project.liberty.bootstrapPropertiesFile != null && project.liberty.bootstrapPropertiesFile.exists()) {
                Files.copy(project.liberty.bootstrapPropertiesFile.toPath(), bootstrapFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                bootStrapPropertiesPath = project.liberty.bootstrapPropertiesFile.getCanonicalPath()
            }
        }

        // handle server.env if not overwritten by server.env from configDirectory
        if (serverEnvPath == null || serverEnvPath.isEmpty()) {
            if (project.liberty.serverEnv != null && project.liberty.serverEnv.exists()) {
                Files.copy(project.liberty.serverEnv.toPath(), new File(serverDirectory, "server.env").toPath(), StandardCopyOption.REPLACE_EXISTING)
                serverEnvPath = project.liberty.serverEnv.getCanonicalPath()
            }
        }
        
        // log info on the configuration files that get used
        if (serverXMLPath != null && !serverXMLPath.isEmpty()) {
            logger.info("Update server configuration file server.xml from " + serverXMLPath)
        }
        if (jvmOptionsPath != null && !jvmOptionsPath.isEmpty()) {
            logger.info("Update server configuration file jvm.options from " + jvmOptionsPath)
        }
        if (bootStrapPropertiesPath != null && !bootStrapPropertiesPath.isEmpty()) {
            logger.info("Update server configuration file bootstrap.properties from " + bootStrapPropertiesPath)
        }
        if (serverEnvPath != null && !serverEnvPath.isEmpty()) {
            logger.info("Update server configuration file server.env from " + serverEnvPath)
        }
    }
    
    private void writeBootstrapProperties(File file, Map<String, String> properties) throws IOException {
        makeParentDirectory(file)
        PrintWriter writer = null
        try {
            writer = new PrintWriter(file, "UTF-8")
            writer.println(HEADER)
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                writer.print(entry.getKey())
                writer.print("=")
                writer.println((entry.getValue() != null) ? entry.getValue().replace("\\", "/") : "")
            }
        } finally {
            if (writer != null) {
                writer.close()
            }
        }
    }
    
    private void writeJvmOptions(File file, List<String> options) throws IOException {
        makeParentDirectory(file)
        PrintWriter writer = null
        try {
            writer = new PrintWriter(file, "UTF-8")
            writer.println(HEADER)
            for (String option : options) {
                writer.println(option)
            }
        } finally {
            if (writer != null) {
                writer.close()
            }
        }
    }
    
    private void makeParentDirectory(File file) {
        File parentDir = file.getParentFile()
        if (parentDir != null) {
            parentDir.mkdirs()
        }
    }
}
