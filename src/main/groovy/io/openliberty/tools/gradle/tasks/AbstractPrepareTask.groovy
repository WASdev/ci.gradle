/**
* (C) Copyright IBM Corporation 2020, 2021.
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

import io.openliberty.tools.common.plugins.util.PrepareFeatureUtil
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException

public class AbstractPrepareTask extends AbstractServerTask {

    
    private PrepareFeatureUtil util;


    private class PrepareFeatureTaskUtil extends PrepareFeatureUtil {
        public PrepareFeatureTaskUtil(File installDir, String openLibertyVerion) throws PluginScenarioException, PluginExecutionException {
            super(installDir, openLibertyVerion)
        }

        @Override
        public void debug(String msg) {
           logger.debug(msg)
        }

        @Override
        public void debug(String msg, Throwable e) {
           logger.debug(msg, (Throwable) e)
        }

        @Override
        public void debug(Throwable e) {
            logger.debug("Throwable exception received: "+e.getMessage(), (Throwable) e)
        }

        @Override
        public void warn(String msg) {
            logger.warn(msg)
        }

        @Override
        public void info(String msg) {
            logger.info(msg)
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isEnabled(LogLevel.DEBUG)
        }

        @Override
        public void error(String msg) {
            logger.error(msg);
        }

        @Override
        public void error(String msg, Throwable e) {
            logger.error(msg, e);
        }

        @Override
        public File downloadArtifact(String groupId, String artifactId, String type, String version) throws PluginExecutionException {
            String coordinates = groupId + ":" + artifactId + ":" + version + "@" + type
            def dep = project.dependencies.create(coordinates)
            def config = project.configurations.detachedConfiguration(dep)

            Set<File> files = new HashSet<File>()
            try {
                config.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                    File artifactFile = artifact.file
                    files.add(artifactFile)
                    debug(artifactFile.toString())
                }
            } catch (ResolveException e) {
                throw new PluginExecutionException("Could not find artifact with coordinates " + coordinates)  
            }

            if (!files) {
                throw new ResolveException("Could not find artifact with coordinates " + coordinates)
            }
            return files.iterator().next()
        }
    }
	
	protected List<String> getDependencyBoms() {
		List<String> result = new ArrayList<String>()
		project.configurations.featuresBom.dependencies.each { dep ->
			def coordinate = dep.group + ":" + dep.name + ":" + dep.version
			logger.debug("feature BOM: " + coordinate)
			result.add(coordinate)
		}
		return result;
	}

    private void createNewPrepareFeatureUtil(String openLibertyVerion) throws PluginExecutionException {
        try {
            util = new PrepareFeatureTaskUtil(getInstallDir(project), openLibertyVerion)
        } catch (PluginScenarioException e) {
            throw new PluginExecutionException("Exception received: "+e.getMessage())
        }
    }

    protected PrepareFeatureUtil getPrepareFeatureUtil(String openLibertyVerion) throws PluginExecutionException {
        createNewPrepareFeatureUtil(openLibertyVerion)
        return util
    }

}