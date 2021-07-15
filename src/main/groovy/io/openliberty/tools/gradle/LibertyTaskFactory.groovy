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


import org.gradle.api.Project

import io.openliberty.tools.gradle.tasks.StartTask
import io.openliberty.tools.gradle.tasks.StopTask
import io.openliberty.tools.gradle.tasks.StatusTask
import io.openliberty.tools.gradle.tasks.CreateTask
import io.openliberty.tools.gradle.tasks.RunTask
import io.openliberty.tools.gradle.tasks.PackageTask
import io.openliberty.tools.gradle.tasks.DumpTask
import io.openliberty.tools.gradle.tasks.JavaDumpTask
import io.openliberty.tools.gradle.tasks.DebugTask
import io.openliberty.tools.gradle.tasks.DeployTask
import io.openliberty.tools.gradle.tasks.UndeployTask
import io.openliberty.tools.gradle.tasks.InstallFeatureTask
import io.openliberty.tools.gradle.tasks.PrepareFeatureTask
import io.openliberty.tools.gradle.tasks.InstallLibertyTask
import io.openliberty.tools.gradle.tasks.UninstallFeatureTask
import io.openliberty.tools.gradle.tasks.CleanTask
import io.openliberty.tools.gradle.tasks.CompileJSPTask
import io.openliberty.tools.gradle.tasks.arquillian.ConfigureArquillianTask
import io.openliberty.tools.gradle.tasks.DevTask
import io.openliberty.tools.gradle.tasks.DevcTask

class LibertyTaskFactory {
    Project project
    LibertyTaskFactory(Project project) {
        this.project = project
    }

    void createTasks() {
        project.tasks.create('compileJSP', CompileJSPTask)
        project.tasks.create('installLiberty', InstallLibertyTask)
        project.tasks.create('libertyRun', RunTask)
        project.tasks.create('libertyDev', DevTask)
        project.tasks.create('libertyDevc', DevcTask)
        project.tasks.create('libertyStatus', StatusTask)
        project.tasks.create('libertyCreate', CreateTask)
        project.tasks.create('libertyStart', StartTask)
        project.tasks.create('libertyStop', StopTask)
        project.tasks.create('libertyPackage', PackageTask)
        project.tasks.create('libertyDump', DumpTask)
        project.tasks.create('libertyJavaDump', JavaDumpTask)
        project.tasks.create('libertyDebug', DebugTask)
        project.tasks.create('deploy', DeployTask)
        project.tasks.create('undeploy', UndeployTask)
        project.tasks.create('installFeature', InstallFeatureTask)
		project.tasks.create('prepareFeature', PrepareFeatureTask)
        project.tasks.create('uninstallFeature', UninstallFeatureTask)
        project.tasks.create('cleanDirs', CleanTask)
        project.tasks.create('configureArquillian', ConfigureArquillianTask)
    }
}
