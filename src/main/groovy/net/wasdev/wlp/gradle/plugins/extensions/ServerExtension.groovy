/**
 * (C) Copyright IBM Corporation 2017.
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

package net.wasdev.wlp.gradle.plugins.extensions

import org.gradle.util.ConfigureUtil

class ServerExtension{
    //Server properties
    String name = "defaultServer"
    String outputDir

    File configDirectory
    File configFile = new File("default")
    File bootstrapPropertiesFile = new File("default")
    File jvmOptionsFile = new File("default")
    File serverEnv = new File("default")

    Map<String, String> bootstrapProperties
    List<String> jvmOptions

    boolean clean = false
    String timeout
    String template

    int verifyTimeout = 30
    String applications

    def numberOfClosures = 0

    DeployExtension deploy = new DeployExtension()
    UndeployExtension undeploy = new UndeployExtension()

    PackageAndDumpExtension packageLiberty = new PackageAndDumpExtension()
    PackageAndDumpExtension dumpLiberty = new PackageAndDumpExtension()
    PackageAndDumpExtension javaDumpLiberty = new PackageAndDumpExtension()

    InstallAppsExtension installapps = new InstallAppsExtension()

    def deploy(Closure closure) {
        if (numberOfClosures > 0){
            deploy.listOfClosures.add(deploy.clone())
            deploy.file = null
        }
        ConfigureUtil.configure(closure, deploy)
        numberOfClosures++
    }

    def undeploy(Closure closure) {
        ConfigureUtil.configure(closure, undeploy)
    }

    def packageLiberty(Closure closure) {
        ConfigureUtil.configure(closure, packageLiberty)
    }

    def dumpLiberty(Closure closure) {
        ConfigureUtil.configure(closure, dumpLiberty)
    }

    def javaDumpLiberty(Closure closure) {
        ConfigureUtil.configure(closure, javaDumpLiberty)
    }

    def installapps(Closure closure) {
        ConfigureUtil.configure(closure, installapps)
    }
}
