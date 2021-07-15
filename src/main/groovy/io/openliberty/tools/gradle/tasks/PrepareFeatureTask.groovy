/**
 * (C) Copyright IBM Corporation 2021.
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
package io.openliberty.tools.gradle.tasks

import java.util.Set

import org.gradle.api.artifacts.ResolveException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import io.openliberty.tools.common.plugins.util.InstallFeatureUtil
import io.openliberty.tools.common.plugins.util.PrepareFeatureUtil
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException

class PrepareFeatureTask extends AbstractPrepareTask {

    PrepareFeatureTask() {
        configure({
            description 'Prepare a user feature for upload to a maven repository'
            group 'Liberty'
        })
    }

    @TaskAction
    void prepareFeature() {
        def propertiesList = InstallFeatureUtil.loadProperties(getInstallDir(project))
        def openLibertyVersion = InstallFeatureUtil.getOpenLibertyVersion(propertiesList)

        PrepareFeatureUtil util = getPrepareFeatureUtil(openLibertyVersion)
		def featureboms = getDependencyBoms()
        util.prepareFeatures(featureboms)
    }
}
