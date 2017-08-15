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
        result.put('serverName', project.liberty.server.name)

        def installDir = getInstallDir(project)
        result.put('installDir', installDir)

        def userDir = getUserDir(project, installDir)
        result.put('userDir', userDir)

        if (project.liberty.server.outputDir != null) {
            result.put('outputDir', project.liberty.server.outputDir)
        }
        if (project.liberty.server.timeout != null && !project.liberty.server.timeout.isEmpty()) {
            result.put('timeout', project.liberty.server.timeout)
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

    protected List<String> buildCommand (String operation) {
        List<String> command = new ArrayList<String>()
        boolean isWindows = System.properties['os.name'].toLowerCase().indexOf("windows") >= 0
        String installDir = getInstallDir(project).toString()

        if (isWindows) {
            command.add(installDir + "\\bin\\server.bat")
        } else {
            command.add(installDir + "/bin/server")
        }
        command.add(operation)
        command.add(project.liberty.server.name)

        return command
    }

    protected File getServerDir(Project project){
        String serverDirectory
        if(project.liberty.server.outputDir !=null && !project.liberty.server.outputDir.isEmpty()){
            serverDirectory = project.liberty.server.outputDir
        }
        else{
            serverDirectory = getInstallDir(project).toString() + "/usr/servers/" + project.liberty.server.name
        }

        return new File(serverDirectory)
    }

    protected void setDefaults(Project project){
        if(project.liberty.server.configFile.toString().equals('default')){
            project.liberty.server.configFile = new File(project.projectDir.toString() + '/src/main/liberty/config/server.xml')
        }
        if(project.liberty.server.bootstrapPropertiesFile.toString().equals('default')){
            project.liberty.server.bootstrapPropertiesFile = new File(project.projectDir.toString() + '/src/main/liberty/config/bootstrap.properties')
        }
        if(project.liberty.server.jvmOptionsFile.toString().equals('default')){
            project.liberty.server.jvmOptionsFile = new File(project.projectDir.toString() + '/src/main/liberty/config/jvm.options')
        }
        if(project.liberty.server.serverEnv.toString().equals('default')){
            project.liberty.server.serverEnv = new File(project.projectDir.toString() + '/src/main/liberty/config/server.env')
        }
    }

    /**
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected void copyConfigFiles() throws IOException {

        String serverDirectory = getServerDir(project).toString()
        String serverXMLPath = null
        String jvmOptionsPath = null
        String bootStrapPropertiesPath = null
        String serverEnvPath = null

        setDefaults(project)

        if (project.liberty.server.configDirectory != null) {
            if(project.liberty.server.configDirectory.exists()){
                // copy configuration files from configuration directory to server directory if end-user set it
                def copyAnt = new AntBuilder()
                copyAnt.copy(todir: serverDirectory) {
                    fileset(dir: project.liberty.server.configDirectory.getCanonicalPath())
                }

                File configDirServerXML = new File(project.liberty.server.configDirectory, "server.xml")
                if (configDirServerXML.exists()) {
                    serverXMLPath = configDirServerXML.getCanonicalPath()
                }

                File configDirJvmOptionsFile = new File(project.liberty.server.configDirectory, "jvm.options")
                if (configDirJvmOptionsFile.exists()) {
                    jvmOptionsPath = configDirJvmOptionsFile.getCanonicalPath()
                }

                File configDirBootstrapFile = new File(project.liberty.server.configDirectory, "bootstrap.properties")
                if (configDirBootstrapFile.exists()) {
                    bootStrapPropertiesPath = configDirBootstrapFile.getCanonicalPath()
                }

                File configDirServerEnv = new File(project.liberty.server.configDirectory, "server.env")
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
            if (project.liberty.server.configFile != null && project.liberty.server.configFile.exists()) {
                Files.copy(project.liberty.server.configFile.toPath(), new File(serverDirectory, "server.xml").toPath(), StandardCopyOption.REPLACE_EXISTING)
                serverXMLPath = project.liberty.server.configFile.getCanonicalPath()
            }
        }

        // handle jvm.options if not overwritten by jvm.options from configDirectory
        if (jvmOptionsPath == null || jvmOptionsPath.isEmpty()) {
            File optionsFile = new File(serverDirectory, "jvm.options")
            if(project.liberty.server.jvmOptions != null && !project.liberty.server.jvmOptions.isEmpty()){
                writeJvmOptions(optionsFile, project.liberty.server.jvmOptions)
                jvmOptionsPath = "inlined configuration"
            } else if (project.liberty.server.jvmOptionsFile != null && project.liberty.server.jvmOptionsFile.exists()) {
                Files.copy(project.liberty.server.jvmOptionsFile.toPath(), optionsFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                jvmOptionsPath = project.liberty.server.jvmOptionsFile.getCanonicalPath()
            }
        }

        // handle bootstrap.properties if not overwritten by bootstrap.properties from configDirectory
        if (bootStrapPropertiesPath == null || bootStrapPropertiesPath.isEmpty()) {
            File bootstrapFile = new File(serverDirectory, "bootstrap.properties")
            if(project.liberty.server.bootstrapProperties != null && !project.liberty.server.bootstrapProperties.isEmpty()){
                writeBootstrapProperties(bootstrapFile, project.liberty.server.bootstrapProperties)
                bootStrapPropertiesPath = "inlined configuration"
            } else if (project.liberty.server.bootstrapPropertiesFile != null && project.liberty.server.bootstrapPropertiesFile.exists()) {
                Files.copy(project.liberty.server.bootstrapPropertiesFile.toPath(), bootstrapFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                bootStrapPropertiesPath = project.liberty.server.bootstrapPropertiesFile.getCanonicalPath()
            }
        }

        // handle server.env if not overwritten by server.env from configDirectory
        if (serverEnvPath == null || serverEnvPath.isEmpty()) {
            if (project.liberty.server.serverEnv != null && project.liberty.server.serverEnv.exists()) {
                Files.copy(project.liberty.server.serverEnv.toPath(), new File(serverDirectory, "server.env").toPath(), StandardCopyOption.REPLACE_EXISTING)
                serverEnvPath = project.liberty.server.serverEnv.getCanonicalPath()
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
    //selects a server from the servers extension or selects all servers if the input is empty or null if any are defined in the build file
    //private void selectServer(String serverId){
    //    if(project.liberty.servers)
    //}

    private void makeParentDirectory(File file) {
        File parentDir = file.getParentFile()
        if (parentDir != null) {
            parentDir.mkdirs()
        }
    }

}
