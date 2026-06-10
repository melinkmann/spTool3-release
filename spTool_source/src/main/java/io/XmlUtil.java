/*
 *  Copyright 2026 Matthias Elinkmann, spTool3
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io;

import core.SpTool3Main;
import gui.dialog.FxEntry;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.ListMethod;
import processing.parameterSets.Method;
import processing.parameterSets.ParamBundle;
import processing.parameterSets.ParamSet;
import processing.parameterSets.XmlInstanceDictionary;
import processing.parameters.Parameter;

public abstract class XmlUtil {

  private static final Logger LOGGER = LogManager.getLogger(XmlUtil.class);

  public static final String XML_ROOT_TAG = "spTool3";


  /*
  XmlIDs for the specific fields in each parameter.
  - unique ID to identify the parameter, e.g., comment, label, distributionType, ...
  - the label of the parameter (not necessary for reading an xml but helpful to make the xml human readable)
  - default value, current value,
  - the text that the node has (all of them are called "parameter"
   */
  public static final String PAR_XML_ID_ATTRIBUTE = "xmlID";
  public static final String PAR_XML_LABEL_ATTRIBUTE = "label";
  public static final String PAR_DEFAULT_ATTRIBUTE = "defaultValue";
  public static final String PAR_VALUE_ATTRIBUTE = "value";
  public static final String PAR_EXPERT_ATTRIBUTE = "expert";
  public static final String PAR_INCREMENT_ATTRIBUTE = "increment";
  public static final String PAR_MIN_ATTRIBUTE = "sliderMin";
  public static final String PAR_MAX_ATTRIBUTE = "sliderMax";
  public static final String PAR_NODE = "parameter";
  public static final String BUNDLE_NODE = "parameterBundle";
  public static final String BUNDLE_DEFAULT_NODE = "parameterBundleDefaults";
  public static final String BUNDLE_ID_ATTRIBUTE = "bundleID";
  public static final String PAR_CHANNEL_ID_ATTRIBUTE = "channelXmlID";
  public static final String PAR_MATCH_ID_ATTRIBUTE = "matcherID";


  public static void writeToXml(Method method, Path file) {
    try {
      // I think this writer needs the file to exist?
      if (!Files.isRegularFile(file)) {
        Files.createFile(file);
      }

      // Create a new DocumentBuilderFactory
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();

      // Create a new Document
      Document document = builder.newDocument();

      // Create configurations root element
      Element root = document.createElement(XML_ROOT_TAG);
      root.setAttribute("version", SpTool3Main.VERSION_ID);
      document.appendChild(root);

      // Create element for the method
      Element methodElement = document.createElement(Method.XML_ELEMENT_TAG);
      method.writeMetaDataToXmlElement(methodElement); // label and date
      root.appendChild(methodElement);

      List<ParamSet> sets = method.getSets();
      for (ParamSet set : sets) {
        // Create element for the set
        Element paramSetElement = document.createElement(set.getXmlType());
        methodElement.appendChild(paramSetElement);

        // Create element for each setParameter in the list
        List<Parameter<?>> parameters = set.listAllParametersForXML();

        //<< These lines were wrapped in the recursive call:
//        for (Parameter<?> par : parameters) {
//          Element parElement = document.createElement(par.getXmlType());
//          par.writeToXmlElement(parElement);
//          paramSetElement.appendChild(parElement);
//
//          // Check if this parameter is header of a bundle of parameters
//          List<ParamBundle> bundles = par.getBundles();
//          if (!bundles.isEmpty()) {
//
//            // Iterate over bundles and save each parameter set under a new element
//            for (ParamBundle bundle : bundles) {
//              Element bundleElement = document.createElement(BUNDLE_NODE);
//              paramSetElement.appendChild(bundleElement);
//            }
//          }
//        }
        // >>

        writeParametersAndBundles(parameters, document, paramSetElement);
      }

      // Write the DOM document to the file
      FileOutputStream fos = new FileOutputStream(file.toFile());
      XmlUtil.writeXmlFile(document, fos);
      fos.close();

      LOGGER.info("Writing to XML file successful: " + file.toFile() + ".");
    } catch (Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
  }

  // Recursive writing was needed, hence this method!
  private static void writeParametersAndBundles(
      List<Parameter<?>> parameters,
      Document document,
      Element paramSetElement) {

    for (Parameter<?> par : parameters) {
      Element parElement = document.createElement(par.getXmlType());
      par.writeToXmlElement(parElement);
      paramSetElement.appendChild(parElement);

      // Check if this parameter is header of a bundle of parameters
      List<ParamBundle> bundles = par.getAllBundlesForXml();
      if (bundles != null && !bundles.isEmpty()) {

        // Iterate over bundles and save each parameter set under a new element
        for (ParamBundle bundle : bundles) {
          Element bundleElement = document.createElement(BUNDLE_NODE);
          par.writeBundleMetaDataToBundleNode(bundleElement);
          // Add the bundle to the containing parameter (not the set-node!)
          parElement.appendChild(bundleElement);
          List<Parameter<?>> bundleParameters = bundle.getSelfAndAllChildrenAllGenForXml();
          writeParametersAndBundles(bundleParameters, document, bundleElement);
        }
      }

      List<ParamBundle> defaultBundles = par.getAllDefaultBundlesForXml();
      if (bundles != null && !bundles.isEmpty()) {

        // Iterate over bundles and save each parameter set under a new element
        for (ParamBundle bundle : defaultBundles) {
          Element bundleElement = document.createElement(BUNDLE_DEFAULT_NODE);
          par.writeBundleMetaDataToBundleNode(bundleElement);
          // Add the bundle to the containing parameter (not the set-node!)
          parElement.appendChild(bundleElement);
          List<Parameter<?>> bundleParameters = bundle.getSelfAndAllChildrenAllGenForXml();
          writeParametersAndBundles(bundleParameters, document, bundleElement);
        }
      }
    }
  }

  public static void writeToXml(ParamSet set, Path file) {
    try {
      // I think this writer needs the file to exist?
      if (!Files.isRegularFile(file)) {
        Files.createFile(file);
      }

      // Create a new DocumentBuilderFactory
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();

      // Create a new Document
      Document document = builder.newDocument();

      // Create configurations root element
      Element root = document.createElement(XML_ROOT_TAG);
      root.setAttribute("version", SpTool3Main.VERSION_ID);
      document.appendChild(root);

      // Create element for the set
      Element paramSetElement = document.createElement(set.getXmlType());
      root.appendChild(paramSetElement);

      // Create element for each setParameter in the list
      List<Parameter<?>> parameters = set.listAllParametersForXML();

      //<< These lines were wrapped in the recursive call:
//      for (Parameter<?> par : parameters) {
//        Element parElement = document.createElement(par.getXmlType());
//        par.writeToXmlElement(parElement);
//        paramSetElement.appendChild(parElement);
//      }
      //>>
      writeParametersAndBundles(parameters, document, paramSetElement);

      // Write the DOM document to the file
      FileOutputStream fos = new FileOutputStream(file.toFile());
      XmlUtil.writeXmlFile(document, fos);
      fos.close();

      LOGGER.info("Writing to XML file successful: " + file.toFile() + ".");

    } catch (ParserConfigurationException | IOException e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
  }

  public static void writeXmlFile(Document document, FileOutputStream fos) throws IOException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    try {
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(fos);
      transformer.transform(source, result);
    } catch (TransformerException e) {
      throw new IOException("Error writing XML file.", e);
    }
  }


  public static List<ParamSet> readSetsFromXml(Path file) {
    List<ParamSet> sets = new ArrayList<>();

    try {
      // Prevents "premature end of file exception" if file is truly empty
      if (Files.size(file) > GlobalIO.MIN_XML_SIZE_BYTES) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        addLoggerToDOM(dBuilder);
        Document doc = dBuilder.parse(file.toFile());
        doc.getDocumentElement().normalize();

        // LOGGER.info("Root element: " + doc.getDocumentElement().getNodeName());

        // List all available nodes (Strings)
        List<String> instancesInXml = new ArrayList<>();

        NodeList allNodeList = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < allNodeList.getLength(); i++) {
          Node node = allNodeList.item(i);
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            instancesInXml.add(node.getNodeName());
          }
        }

        // Try to parse the instances
        for (String instanceTag : instancesInXml) {

          // Get the instance nodes, e.g., configuration
          Element instanceXml = (Element) doc.getElementsByTagName(instanceTag).item(0);
          ParamSet instance = XmlInstanceDictionary.lookup(instanceTag);
          // Found?
          if (instance != null) {
            // Get all parameter nodes under configuration
            NodeList nodeList = instanceXml.getElementsByTagName(PAR_NODE);

            instance.fillFromXml(nodeList, file);
            sets.add(instance);
          }
        }
      }

    } catch (Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
    LOGGER.debug("Finished Reading from " + file);
    return sets;
  }


  public static Method readMethodFromXml(Path file) {
    Method newMethod = new ListMethod();
    if (Files.isRegularFile(file) && Files.isReadable(file)
        && PathUtil.getExtensionWithDot(file).equals(GlobalIO.METHOD_EXTENSION)) {

      try {
        // Prevents "premature end of file exception" if file is truly empty
        if (Files.size(file) > GlobalIO.MIN_XML_SIZE_BYTES) {
          DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
          DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
          addLoggerToDOM(dBuilder);
          Document doc = dBuilder.parse(file.toFile());
          doc.getDocumentElement().normalize();

          // LOGGER.info("Root element: " + doc.getDocumentElement().getNodeName());

          // Document is the top level node <spTool3 version="3.0.1">.
          Node documentNode = doc.getDocumentElement();

          // Get all nodes with the element tag for <method>
          NodeList methodNodes = doc.getElementsByTagName(Method.XML_ELEMENT_TAG);
        /*
         Check for method node: read label and date (similar to how a parameter reads its default fields)
         Note that the node of the tag "Method.XML_ELEMENT_TAG",i.e., "method" is:
         <method commentForSet="" dateCreated="Thu Jun 27 19:07:54 CEST 2024" labelOfSet="Csv reader templates">
         and not its children!

         in other words

         "fillWithMetaDataFromXmlElement" method looks for <method ...> nodes in the list,
         checks if they are proper Nodes
         and checks its children if they are the attributes "label" or "date"
         */
          newMethod.fillWithMetaDataFromXmlElement(methodNodes, file);

          // Loop through the children and find proper nodes (not attributes and such)
          for (int i = 0; i < methodNodes.getLength(); i++) {
            Node methodNode = methodNodes.item(i);

            // Check if is the suspected method node is a node or attribute (which should be the case)
            if (methodNode != null && methodNode.getNodeType() == Node.ELEMENT_NODE) {

              // Loop through the method node's children and only use the proper element nodes (not the attributes)
              NodeList childrenOfMethodNode = methodNode.getChildNodes();
              int noOfChildren = childrenOfMethodNode.getLength();

              for (int n = 0; n < noOfChildren; n++) {
                // Check if the child is a proper node, e.g., <configuration> or <BaselineParams>
                Node child = childrenOfMethodNode.item(n);
                if (child != null) {
                  String xmlIDofSubMethod = child.getNodeName();
                  if (child.getNodeType() == Node.ELEMENT_NODE) {
                    ParamSet instance = XmlInstanceDictionary.lookup(xmlIDofSubMethod);
                    // Found?
                    if (instance != null && child instanceof Element) {
                      // Get all parameter nodes under configuration
                      Element instanceXml = (Element) child;
                      NodeList nodeList = instanceXml.getElementsByTagName(PAR_NODE);
                      instance.fillFromXml(nodeList, file);
                      newMethod.addSet(instance);
                    }
                  }
                }
              }
            }
          }
        }

      } catch (Exception e) {
        LOGGER.error(ExceptionUtils.getStackTrace(e));
      }
    }
    LOGGER.info("Finished Reading from " + file);
    return newMethod;
  }

  //   https://stackoverflow.com/questions/10022796/why-am-i-getting-premature-end-of-file-error
  private static void addLoggerToDOM(DocumentBuilder documentBuilder) {
    documentBuilder.setErrorHandler(new ErrorHandler() {
      @Override
      public void warning(SAXParseException exception) throws SAXException {
        LOGGER.warn(ExceptionUtils.getStackTrace(exception));
      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException {
        LOGGER.fatal(ExceptionUtils.getStackTrace(exception));
      }

      @Override
      public void error(SAXParseException e) throws SAXException {
        LOGGER.error(ExceptionUtils.getStackTrace(e));
      }
    });
  }

  /*
  Special cases.
   */
  public static void writeSubMethodEntriesToFile(List<FxEntry<FxParamSet>> list,
      String containerLabel,
      Path methodPath) {

    List<ParamSet> fxParamSets =
        list.stream()
            .filter(Predicate.not(Objects::isNull))
            .map(FxEntry::unwrap)
            .filter(Predicate.not(Objects::isNull))
            .map(FxParamSet::getPlainSet)
            .collect(Collectors.toList());

    writeSubMethodsToFile(fxParamSets, containerLabel, methodPath);
  }

  public static void writeSubMethodsToFile(List<ParamSet> list, String containerLabel,
      Path methodPath) {

    // Method only serves as a container to store
    Method container = new ListMethod(containerLabel);
    container.addSets(
        list.stream()
            .filter(Predicate.not(Objects::isNull))
            .collect(Collectors.toList()));

    XmlUtil.writeToXml(container, methodPath);
  }

  public static List<FxParamSet> getSubMethodsFromFile(Path file) {
    return getSubMethodsFromFile(file, null);
  }

  public static List<FxParamSet> getSubMethodsFromFile(Path file,
      @Nullable Supplier<ParamSet> newInstanceSource) {

    // Read
    Method container = XmlUtil.readMethodFromXml(file);
    List<ParamSet> sets = container.getSets();

    // For better usability: If empty, add one.
    if (sets.isEmpty() && newInstanceSource != null) {
      sets.add(newInstanceSource.get());
    }
    //
    List<FxParamSet> fxEntries = sets.stream()
        .map(ParamSet::getObservableInstance)
        .collect(Collectors.toList());
    return fxEntries;
  }

}
