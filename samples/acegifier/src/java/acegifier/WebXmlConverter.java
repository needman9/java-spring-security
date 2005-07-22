package acegifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.DocumentResult;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * A utility to translate a web.xml file into a set of acegi security spring beans.
 *
 * Also produces a new "acegified" web.xml file with the necessary filters installed
 * and the security elements defined by the servlet DTD removed.
 *
 * <p>
 * This class wraps the XSL transform which actually does most of the work.
 * </p>
 *
 * @author Luke Taylor
 * @version $Id$
 */
public class WebXmlConverter {
    private static final String WEB_TO_SPRING_XSL_FILE = "web-to-spring.xsl";
    private static final String NEW_WEB_XSLT_FILE = "acegi-web.xsl";

    private Transformer acegiSecurityTransformer, newWebXmlTransformer;

    /**
     * The name of the spring-beans file which the beans will be stored in.
     * This is required when writing the new web.xml content.
     */
    private String acegiOutputFileName = "applicationContext-acegi-security.xml";

    /** The web.xml content to be converted */
    private Source xmlSource;
    /** The results of the conversion */
    private Document newWebXml, acegiBeansXml;

    public WebXmlConverter() throws IOException, TransformerConfigurationException {
        TransformerFactory tf = TransformerFactory.newInstance();

        acegiSecurityTransformer = tf.newTransformer(createTransformerSource(WEB_TO_SPRING_XSL_FILE));
        newWebXmlTransformer = tf.newTransformer(createTransformerSource(NEW_WEB_XSLT_FILE));
    }

    private Source createTransformerSource(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(fileName);
        return new StreamSource(resource.getInputStream());
    }

    /**
     * Performs the transformations on the input source.
     * Creates new web.xml content and a set of acegi-security Spring beans which can be
     * accessed through the appropriate getter methods.
     */
    public void doConversion() throws IOException, TransformerException {
        Assert.notNull(xmlSource, "The XML input must be set");

        // Create the modified web.xml file
        newWebXmlTransformer.setParameter("acegi-security-context-file", acegiOutputFileName);
//        newWebXmlTransformer.setParameter("cas-proxy-url", "http://localhost:8433/cas/proxy");
        DocumentResult result = new DocumentResult();
        newWebXmlTransformer.transform(xmlSource, result);
        newWebXml = result.getDocument();

        result = new DocumentResult();
        acegiSecurityTransformer.transform(xmlSource, result);
        acegiBeansXml = result.getDocument();
    }

    /** Set the input as an xml string */
    public void setInput(String xml) throws DocumentException {
        setInput(DocumentHelper.parseText(xml));
    }

    /** Set the input as a stream */
    public void setInput(InputStream in) throws DocumentException {
        SAXReader reader = new SAXReader();
        setInput(reader.read(in));
    }

    /** set the input as a dom4j document */
    public void setInput(Document document) throws DocumentException {
        validateWebXml(document);
        xmlSource = new DocumentSource(document);
    }

    /** Checks the web.xml to make sure it contains correct data */
    private void validateWebXml(Document document) throws DocumentException {
        Node authMethodNode =
                document.selectSingleNode("/web-app/login-config/auth-method");
        if(authMethodNode == null)
            throw new DocumentException("login-config and auth-method must be present");
        String authMethod =  authMethodNode.getStringValue().toUpperCase();
        if(!authMethod.equals("BASIC") && !authMethod.equals("FORM")) {
            throw new DocumentException("unsupported auth-method: " + authMethod);
        }
        List roles = document.selectNodes("/web-app/security-role");
        if(roles.isEmpty()) {
            throw new DocumentException("Each role used must be defined in a security-role element");
        }
    }

    public String getAcegiOutputFileName() {
        return acegiOutputFileName;
    }

    public void setAcegiOutputFileName(String acegiOutputFileName) {
        this.acegiOutputFileName = acegiOutputFileName;
    }

    /** Returns the converted web.xml content */
    public Document getNewWebXml() {
        return newWebXml;
    }

    /**
     * Returns the created spring-beans xml content which should be used in
     * the application context file.
     */
    public Document getAcegiBeans() {
        return acegiBeansXml;
    }
}
