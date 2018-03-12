package net.wasdev.wlp.gradle.plugins

interface ILibertyDefinitions {

  public Map<String, Map> taskDefMap = [:]

  public static final String GROUP_NAME = "Liberty"

  public static final String DEPLOY_FOLDER_DROPINS = "dropins"
  public static final String DEPLOY_FOLDER_APPS = 'apps'

  public static final String TASK_CORE_WAR = "war"

  public static final String TASK_COMPILE_JSP = "compileJSP"
  public static final String TASK_INSTALL_LIBERTY = "installLiberty"
  public static final String TASK_LIBERTY_RUN = "libertyRun"
  public static final String TASK_LIBERTY_STATUS = "libertyStatus"
  public static final String TASK_LIBERTY_CREATE = "libertyCreate"
  public static final String TASK_LIBERTY_CREATE_ANT = "libertyCreateAnt"
  public static final String TASK_LIBERTY_CREATE_CONFIG = "libertyCreateConfig"
  public static final String TASK_LIBERTY_CREATE_BOOTSTRAP = "libertyCreateBootstrap"
  public static final String TASK_LIBERTY_CREATE_JVM_OPTIONS = "libertyCreateJvmOptions"
  public static final String TASK_LIBERTY_CREATE_SERVER_XML = "libertyCreateServerXml"
  public static final String TASK_LIBERTY_CREATE_SERVER_DEFAULT_XML = "libertyCreateDefaultServerXml"
  public static final String TASK_LIBERTY_CREATE_SERVER_ENV = "libertyCreateServerEnv"
  public static final String TASK_LIBERTY_START = "libertyStart"
  public static final String TASK_LIBERTY_STOP = "libertyStop"
  public static final String TASK_LIBERTY_PACKAGE = "libertyPackage"
  public static final String TASK_LIBERTY_DUMP = "libertyDump"
  public static final String TASK_LIBERTY_JAVA_DUMP = "libertyJavaDump"
  public static final String TASK_LIBERTY_DEBUG = "libertyDebug"
  public static final String TASK_DEPLOY = "deploy"
  public static final String TASK_DEPLOY_CONFIG = "deployConfig"
  public static final String TASK_DEPLOY_LIBERTY = "deployLibertyBlock"
  public static final String TASK_DEPLOY_LOCAL = "deployLocal"
  public static final String TASK_UNDEPLOY = "undeploy"
  public static final String TASK_INSTALL_FEATURE = "installFeature"
  public static final String TASK_UNINSTALL_FEATURE = "uninstallFeature"
  public static final String TASK_CLEAN_DIRS = "cleanDirs"
  public static final String TASK_INSTALL_APPS = "installApps"
  public static final String TASK_INSTALL_APPS_ARCHIVE = "installAppsArchive"
  public static final String TASK_INSTALL_APPS_LOOSE = "installAppsLoose"
  public static final String TASK_INSTALL_APPS_SANITY = "installAppsSanity"
  public static final String TASK_INSTALL_APPS_AUTOCONFIG = "installAppsAutoConfig"

}
