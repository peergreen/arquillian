/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.peergreen.arquillian.container;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

/**
 * Embedded container that will allows to deploy archives on Peergreen server.
 * @author Florent Benoit
 */
public class PeergreenEmbeddedContainer implements DeployableContainer<PeergreenConfiguration> {

    private Framework framework;

    private BundleContext bundleContext;

    private MBeanServerConnection mbeanServer;

    private ObjectName deploymentMBean;

    private final Map<String, String> urisByName;

    public PeergreenEmbeddedContainer() {
        this.urisByName = new HashMap<>();
    }


    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        String name = archive.getName();
        String uri;

        File archiveFile;
        try {
            archiveFile = File.createTempFile("arquillian", archive.getName());
            archiveFile.deleteOnExit();
            uri = archiveFile.toURI().toString();
        } catch (IOException e) {
            throw new DeploymentException("Unable to deploy archive", e);
        }
        archive.as(ZipExporter.class).exportTo(archiveFile, true);

        try {
            mbeanServer.invoke(deploymentMBean, "process", new Object[] { "DEPLOY", uri}, new String[] { String.class.getName(), String.class.getName()});
        } catch (InstanceNotFoundException | MBeanException | ReflectionException | IOException e) {
            throw new DeploymentException("Unable to deploy archive", e);
        }
        urisByName.put(name, uri);

        // If it is a war file then we could use Servlet 3.0 protocol
        if (name.endsWith(".war")) {
            //FIXME : should get port number through MBean
            HTTPContext httpContext = new HTTPContext("localhost", 9000);
            String defaultContext = "/".concat(archiveFile.getName().substring(0, archiveFile.getName().length() - ".war".length()));

            //FIXME : should connect to the MBean and get all the servlets running in the server
            httpContext.add(new Servlet("ArquillianServletRunner", defaultContext));

            return new ProtocolMetaData().addContext(httpContext);
        }
        return new ProtocolMetaData();
    }

    @Override
    public void deploy(Descriptor arg0) throws DeploymentException {
        throw new DeploymentException("Not supported");
    }

    @Override
    public Class<PeergreenConfiguration> getConfigurationClass() {
        return PeergreenConfiguration.class;
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Peergreen");
    }

    @Override
    public void setup(PeergreenConfiguration peergreenConfiguration) {
        Map<String, String> map = new HashMap<>();

        String tmpDir = System.getProperty("java.io.tmpdir");
        String username = System.getProperty("user.name");
        File tmpFolder = new File(tmpDir, "peergreen-arquillian-".concat(String.valueOf(username.hashCode())));
        delete(tmpFolder);
        map.put(Constants.FRAMEWORK_STORAGE, tmpFolder.toString());
        framework = peergreenConfiguration.getFrameworkFactory().newFramework(map);
    }

    protected void delete(File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File subFile : files) {
                delete(subFile);
            }
            file.delete();
        } else {
            file.delete();
        }


    }



    @Override
    public void start() throws LifecycleException {


        try {
            framework.start();
        } catch (BundleException e) {
            throw new LifecycleException("Cannot start the peergreen server", e);
        }

        //Needs to wait for stability in the kernel
        //FIXME: remove these wait when kernel will be updated
        try {
            Thread.sleep(9000L);
        } catch (InterruptedException e) {
            throw new LifecycleException("Cannot wait for stability", e);
        }

        bundleContext = framework.getBundleContext();
        this.mbeanServer = getMBeanServerConnection();

        // install bundle
        URL url = PeergreenEmbeddedContainer.class.getResource("/peergreen-tests-runner-internal.jar");
        Bundle bundle = null;
        try {
            bundle = bundleContext.installBundle(url.toExternalForm());
            bundle.start();
        } catch (BundleException e) {
            throw new LifecycleException("Cannot install the tests runner", e);
        }
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            throw new LifecycleException("Cannot wait for stability", e);
        }

        // Gets the MBean
        Set<ObjectInstance> instances = null;
        ObjectName depMBean;
        try {
            depMBean = new ObjectName("peergreen:type=Deployment");
            instances = mbeanServer.queryMBeans(depMBean, null);
        } catch (MalformedObjectNameException | IOException e) {
            throw new LifecycleException("Unable to get deployment MBean", e);
        }
        if (instances == null || instances.size() == 0) {
            throw new LifecycleException("Unable to find the Deployment MBean in the MBean server");
        }
        deploymentMBean = instances.iterator().next().getObjectName();

    }

    @Override
    public void stop() throws LifecycleException {
        try {
            framework.stop();
        } catch (BundleException e) {
            throw new LifecycleException("Cannot start the peergreen server", e);
        }
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        String name = archive.getName();
        String uri = urisByName.get(name);
        try {
            mbeanServer.invoke(deploymentMBean, "process", new Object[] { "UNDEPLOY", uri}, new String[] { String.class.getName(), String.class.getName()});
        } catch (InstanceNotFoundException | MBeanException | ReflectionException | IOException e) {
            throw new DeploymentException("Unable to undeploy archive", e);
        }
    }

    @Override
    public void undeploy(Descriptor arg0) throws DeploymentException {
        throw new DeploymentException("Not supported");
    }



    protected MBeanServerConnection getMBeanServerConnection() {
        return ManagementFactory.getPlatformMBeanServer();
    }

}
