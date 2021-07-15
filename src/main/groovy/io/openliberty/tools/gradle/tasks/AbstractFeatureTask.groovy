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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import io.openliberty.tools.common.plugins.util.InstallFeatureUtil
import io.openliberty.tools.common.plugins.util.InstallFeatureUtil.ProductProperties
import io.openliberty.tools.common.plugins.util.PluginExecutionException
import io.openliberty.tools.common.plugins.util.PluginScenarioException

public class AbstractFeatureTask extends AbstractServerTask {

    // DevMode uses this option to provide the location of the
    // temporary serverDir it uses after a change to the server.xml
    private String serverDirectoryParam;
    
    public boolean installFeaturesFromAnt;

    private InstallFeatureUtil util;
	
	Project newProject = project;

    @Option(option = 'serverDir', description = '(Optional) Server directory to get the list of features from.')
    void setServerDirectoryParam(String serverDir) {
        this.serverDirectoryParam = serverDir;
    }

    private class InstallFeatureTaskUtil extends InstallFeatureUtil {
		
		
        public InstallFeatureTaskUtil(File installDir, String from, String to, Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons)  throws PluginScenarioException, PluginExecutionException {
            super(installDir, from, to, pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons)
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
			def dep = newProject.dependencies.create(coordinates)
			def config = newProject.configurations.detachedConfiguration(dep)
	
			Set<File> files = new HashSet<File>()
			try {
				config.resolvedConfiguration.resolvedArtifacts.each { artifact ->
					File artifactFile = artifact.file
					files.add(artifactFile)
					debug(artifactFile.toString())
				}
			} catch (ResolveException e) {
				throw new PluginExecutionException("Could not find artifact with coordinates " + coordinates, e)
			}
	
			if (!files) {
				throw new PluginExecutionException("Could not find artifact with coordinates " + coordinates)
			}
			return files.iterator().next()
		
        }
    }

    protected Set<String> getPluginListedFeatures(boolean findEsaFiles) {
        def features = server.features.name
        Set<String> result = new HashSet<String>()
        for (String feature : features) {
            if ((findEsaFiles && feature.endsWith(".esa"))
                    || (!findEsaFiles && !feature.endsWith(".esa"))) {
                result.add(feature)
                logger.debug(("Plugin listed " + (findEsaFiles ? "ESA" : "feature") + ": " + feature))
            }
        }
        return result
    }

    @Internal
    protected Set<String> getDependencyFeatures() {
        Set<String> features = new HashSet<String>()
        project.configurations.libertyFeature.dependencies.each { dep ->
            logger.debug("Dependency feature: " + dep.name)
            features.add(dep.name)
        }
        return features
    }
	
	protected List<String> getAdditionalJsonList() {
		List<String> result = new ArrayList<String>()
		project.configurations.featuresBom.dependencies.each { dep ->
			def coordinate = dep.group + ":" + "features" + ":" + dep.version
			logger.debug("feature Json: " + coordinate)
			result.add(coordinate)
		}
		return result;
	}

    protected Set<String> getSpecifiedFeatures(String containerName) throws PluginExecutionException {
        if (util == null) {
            def pluginListedEsas = getPluginListedFeatures(true)
            def propertiesList = null;
            def openLibertyVersion = null;
            if (containerName == null) {
                propertiesList = InstallFeatureUtil.loadProperties(getInstallDir(project))
                openLibertyVersion = InstallFeatureUtil.getOpenLibertyVersion(propertiesList)
            }
			def additionalJsons = getAdditionalJsonList()
            createNewInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVersion, containerName, additionalJsons)
        }
        // if createNewInstallFeatureUtil failed to create a new InstallFeatureUtil instance, then features are installed via ant
        if(installFeaturesFromAnt) {
            Set<String> featuresInstalledFromAnt;
            if(server.features.name != null) {
                featuresInstalledFromAnt = new HashSet<String>(server.features.name);
                return featuresInstalledFromAnt;
            }
            else {
                featuresInstalledFromAnt = new HashSet<String>();
                return featuresInstalledFromAnt;
            }
        }
        
        def pluginListedFeatures = getPluginListedFeatures(false)
        def dependencyFeatures = getDependencyFeatures()
        def serverFeatures = null;

        // if DevMode provides a server directory parameter use that for finding the server features
        if (serverDirectoryParam != null) {
            serverFeatures = util.getServerFeatures(new File(serverDirectoryParam), getLibertyDirectoryPropertyFiles(serverDirectoryParam))
        } else if (getServerDir(project).exists()) {
            serverFeatures = util.getServerFeatures(getServerDir(project), getLibertyDirectoryPropertyFiles(null))
        }

        Set<String> featuresToInstall = InstallFeatureUtil.combineToSet(pluginListedFeatures, dependencyFeatures, serverFeatures)
        return featuresToInstall 
    }

    private void createNewInstallFeatureUtil(Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons) throws PluginExecutionException {
        try {
            util = new InstallFeatureTaskUtil(getInstallDir(project), server.features.from, server.features.to, pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons)
        } catch (PluginScenarioException e) {
            logger.debug("Exception received: "+e.getMessage(),(Throwable)e)
            logger.debug("Installing features from installUtility.")
            installFeaturesFromAnt = true
            return
        }
    }

    protected InstallFeatureUtil getInstallFeatureUtil(Set<String> pluginListedEsas, List<ProductProperties> propertiesList, String openLibertyVerion, String containerName, List<String> additionalJsons) throws PluginExecutionException {
		//if installing userFeature, recompile gradle to find mavenLocal artifacts created by prepareFeature task. 
		if(project.configurations.featuresBom.dependencies) {  
			try {
				ProjectBuilder builder = ProjectBuilder.builder();
				newProject = builder
						.withProjectDir(project.rootDir)
						.withGradleUserHomeDir(project.gradle.gradleUserHomeDir)
						.withName(project.name)
						.build();

				// need this for gradle to evaluate the project
				// and load the different plugins and extensions
				newProject.evaluate();
			} catch (Exception e) {
				logger.error("Could not parse build.gradle " + e.getMessage());
				logger.debug('Error parsing build.gradle', e);
			}
		}
		createNewInstallFeatureUtil(pluginListedEsas, propertiesList, openLibertyVerion, containerName, additionalJsons)
        return util
    }

}