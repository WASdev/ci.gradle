package net.wasdev.wlp.gradle.plugins;

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

import java.io.File;
import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class TestLooseEarApplication extends AbstractIntegrationTest{
    static File resourceDir = new File("build/resources/integrationTest/loose-ear-test")
    static File buildDir = new File(integTestDir, "/test-loose-ear-application")
    static String buildFilename = "build.gradle"

    @BeforeClass
    public static void setup() {
        createDir(buildDir)
        if(test_mode == "offline"){
            WLP_DIR.replace("\\","/")
        }
        createTestProject(buildDir, resourceDir, buildFilename)
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTasks(buildDir, 'libertyStop')
    }

    @Test
    public void test_loose_config_file_exists() {
        try {
            runTasks(buildDir, 'installApps')
        } catch (Exception e) {
            throw new AssertionError ("Fail on task installApps. " + e)
        }
        assert new File('build/testBuilds/test-loose-ear-application/ejb-ear/build/wlp/usr/servers/ejbServer/apps/ejb-ear.ear.xml').exists() : 'looseApplication config file was not copied over to the liberty runtime'
    }
/*
  Expected output to the XML
  <?xml version="1.0" encoding="UTF-8"?>
  <archive>
      <file sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-ear/build/tmp/ear/application.xml" targetInArchive="/META-INF/application.xml"/>
      <archive targetInArchive="/ejb-ejb.jar">
          <dir sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-ejb/build/classes/java/main" targetInArchive="/"/>
          <file sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-ejb/build/tmp/jar/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
      </archive>
      <archive targetInArchive="/ejb-war.war">
          <dir sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-war/build/classes/java/main" targetInArchive="/WEB-INF/classes"/>
          <file sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-war/build/tmp/jar/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
      </archive>
      <file sourceOnDisk="/Users/jjvilleg/Desktop/ci.gradle/build/testBuilds/test-loose-ear-application/ejb-ear/build/tmp/ear/MANIFEST.MF" targetInArchive="/META-INF/MANIFEST.MF"/>
  </archive>

*/
    @Test
    public void test_loose_config_file_contents_are_correct(){
      File on = new File("build/testBuilds/test-loose-ear-application/ejb-ear/build/wlp/usr/servers/ejbServer/apps/ejb-ear.ear.xml");
      FileInputStream input = new FileInputStream(on);

      // get input XML Document
      DocumentBuilderFactory inputBuilderFactory = DocumentBuilderFactory.newInstance();
      inputBuilderFactory.setIgnoringComments(true);
      inputBuilderFactory.setCoalescing(true);
      inputBuilderFactory.setIgnoringElementContentWhitespace(true);
      inputBuilderFactory.setValidating(false);
      DocumentBuilder inputBuilder = inputBuilderFactory.newDocumentBuilder();
      Document inputDoc=inputBuilder.parse(input);

      // parse input XML Document
      XPath xPath = XPathFactory.newInstance().newXPath();
      String expression = "/archive/dir";
      NodeList nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <dir/> element ==>", 0, nodes.getLength());

      expression = "/archive/archive";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <archive/> element ==>", 2, nodes.getLength());
      Assert.assertEquals("archive targetInArchive attribute value", "/ejb-ejb.jar",
              nodes.item(0).getAttributes().getNamedItem("targetInArchive").getNodeValue());
      Assert.assertEquals("archive targetInArchive attribute value", "/ejb-war.war",
              nodes.item(1).getAttributes().getNamedItem("targetInArchive").getNodeValue());

      expression = "/archive/file";
      nodes = (NodeList) xPath.compile(expression).evaluate(inputDoc, XPathConstants.NODESET);
      Assert.assertEquals("Number of <file/> element ==>", 2, nodes.getLength());

    }


}