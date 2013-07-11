/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.peergreen.arquillian;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.container.spi.client.deployment.Validate;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.ClassAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import com.peergreen.arquillian.generator.CommonPeergreenTestRunner;
import com.peergreen.arquillian.generator.filter.FilterClassGenerator;
import com.peergreen.arquillian.generator.filter.PeergreenTestRunnerFilter;
import com.peergreen.arquillian.generator.listener.ListenerClassGenerator;
import com.peergreen.arquillian.generator.listener.PeergreenTestRunnerListener;
import com.peergreen.arquillian.generator.servlet.PeergreenTestRunnerServlet;
import com.peergreen.arquillian.generator.servlet.ServletClassGenerator;

/**
 * Scenario allowing to generate multiple assemblies for a given set of classes.
 * For example it could generate a war file, an ejb-jar file or ejb-in-war file.
 * @author Florent Benoit
 */
public class PeergreenScenarioGenerator implements DeploymentScenarioGenerator {

    private static Logger log = Logger.getLogger(PeergreenScenarioGenerator.class.getName());

    @Override
    public List<DeploymentDescription> generate(TestClass testClass) {
        // Generating
        List<DeploymentDescription> deployments = new ArrayList<>();
        Method[] deploymentMethods = testClass.getMethods(Deployment.class);

        for (Method deploymentMethod : deploymentMethods) {
            deployments.add(generateDeployment(deploymentMethod));
        }

        return deployments;
    }

    /**
     * @param deploymentMethod
     * @return
     */
    private DeploymentDescription generateDeployment(Method deploymentMethod) {
        TargetDescription target = generateTarget(deploymentMethod);
        ProtocolDescription protocol = generateProtocol(deploymentMethod);

        Deployment deploymentAnnotation = deploymentMethod.getAnnotation(Deployment.class);
        DeploymentDescription deployment = null;
        if (Archive.class.isAssignableFrom(deploymentMethod.getReturnType())) {
            deployment = new DeploymentDescription(deploymentAnnotation.name(), invokeArchive(deploymentMethod));
            logWarningIfArchiveHasUnexpectedFileExtension(deployment);
            deployment.shouldBeTestable(deploymentAnnotation.testable());
        } else if (Descriptor.class.isAssignableFrom(deploymentMethod.getReturnType())) {
            deployment = new DeploymentDescription(deploymentAnnotation.name(), invoke(Descriptor.class,
                    deploymentMethod));
            // deployment.shouldBeTestable(false);
        }
        deployment.shouldBeManaged(deploymentAnnotation.managed());
        deployment.setOrder(deploymentAnnotation.order());
        if (target != null) {
            deployment.setTarget(target);
        }
        if (protocol != null) {
            deployment.setProtocol(protocol);
        }

        if (deploymentMethod.isAnnotationPresent(ShouldThrowException.class)) {
            deployment.setExpectedException(deploymentMethod.getAnnotation(ShouldThrowException.class).value());
            deployment.shouldBeTestable(false); // can't test against failing
                                                // deployments
        }

        return deployment;
    }

    private void logWarningIfArchiveHasUnexpectedFileExtension(final DeploymentDescription deployment) {
        if (!Validate.archiveHasExpectedFileExtension(deployment.getArchive())) {
            log.warning("Deployment archive of type "
                    + deployment.getArchive().getClass().getSimpleName()
                    + " has been given an unexpected file extension. Archive name: "
                    + deployment.getArchive().getName()
                    + ", deployment name: "
                    + deployment.getName()
                    + ". It might not be wrong, but the container will"
                    + " rely on the given file extension, the archive type is only a description of a certain structure.");
        }
    }

    /**
     * @param deploymentMethod
     * @return
     */
    private TargetDescription generateTarget(Method deploymentMethod) {
        if (deploymentMethod.isAnnotationPresent(TargetsContainer.class)) {
            return new TargetDescription(deploymentMethod.getAnnotation(TargetsContainer.class).value());
        }
        return TargetDescription.DEFAULT;
    }

    /**
     * @param deploymentMethod
     * @return
     */
    private ProtocolDescription generateProtocol(Method deploymentMethod) {
        if (deploymentMethod.isAnnotationPresent(OverProtocol.class)) {
            return new ProtocolDescription(deploymentMethod.getAnnotation(OverProtocol.class).value());
        }
        return ProtocolDescription.DEFAULT;
    }

    /**
     * @param deploymentMethod
     * @return
     */
    private Archive invokeArchive(Method deploymentMethod) {
        Archive<?> archive = invoke(Archive.class, deploymentMethod);

        // Not a multiple deployment, return it directly
        if (!"createMultipleDeployment".equals(deploymentMethod.getName())) {
            return archive;
        }

        // Build a WebArchive from the user-defined archive
        WebArchive webArchive = ShrinkWrap.create(WebArchive.class);

        // Add the content of the archive (except the Content class)
        Map<ArchivePath, Node> content = archive.getContent();
        Set<Entry<ArchivePath, Node>> entries = content.entrySet();
        Iterator<Entry<ArchivePath, Node>> iterator = entries.iterator();
        ArchivePath componentPath = null;
        Node componentNode = null;
        while (iterator.hasNext()) {
            Entry<ArchivePath, Node> entry = iterator.next();

            ArchivePath entryPath = entry.getKey();
            Node entryNode = entry.getValue();
            if (entryPath.get().endsWith("Component.class")) {
                componentPath = entryPath;
                componentNode = entryNode;
            } else if (entryNode.getAsset() != null) {
                // Add it in the web Archive only if it has something
                webArchive.add(entryNode.getAsset(), "/WEB-INF/classes".concat(entryPath.get()));
            }

        }
        if (componentPath == null) {
            throw new IllegalStateException("No component.class found so no classes have been generated");
        }

        String parentPath = componentPath.getParent().get();

        // Generate the servlet class
        try {
            byte[] servletClassByteCode = new ServletClassGenerator().generate(componentNode.getAsset().openStream());
            Asset servletAsset = new ByteArrayAsset(servletClassByteCode);
            webArchive.add(servletAsset, "/WEB-INF/classes".concat(parentPath).concat("/ComponentServlet.class"));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate class", e);
        }
        // add super servlet class
        webArchive.add(
                new ClassAsset(PeergreenTestRunnerServlet.class),
                "/WEB-INF/classes/".concat(PeergreenTestRunnerServlet.class.getName().replace(".", "/")
                        .concat(".class")));

        // Generate the Listener class
        try {
            byte[] listenerClassByteCode = new ListenerClassGenerator().generate(componentNode.getAsset().openStream());
            Asset listenerAsset = new ByteArrayAsset(listenerClassByteCode);
            webArchive.add(listenerAsset, "/WEB-INF/classes".concat(parentPath).concat("/ComponentListener.class"));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate class", e);
        }
        // add super listener class
        webArchive.add(
                new ClassAsset(PeergreenTestRunnerListener.class),
                "/WEB-INF/classes/".concat(PeergreenTestRunnerListener.class.getName().replace(".", "/")
                        .concat(".class")));

        // Generate the Filter class
        try {
            byte[] filterClassByteCode = new FilterClassGenerator().generate(componentNode.getAsset().openStream());
            Asset filterAsset = new ByteArrayAsset(filterClassByteCode);
            webArchive.add(filterAsset, "/WEB-INF/classes".concat(parentPath).concat("/ComponentFilter.class"));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate class", e);
        }
        // add super filter class
        webArchive.add(new ClassAsset(PeergreenTestRunnerFilter.class), "/WEB-INF/classes/"
                .concat(PeergreenTestRunnerFilter.class.getName().replace(".", "/").concat(".class")));

        // add super common class
        webArchive.add(new ClassAsset(CommonPeergreenTestRunner.class), "/WEB-INF/classes/"
                .concat(CommonPeergreenTestRunner.class.getName().replace(".", "/").concat(".class")));

        System.setProperty("peergreen.arquillian.generated.components", "3");

        return webArchive;
    }

    /**
     * @param deploymentMethod
     * @return
     */
    private <T> T invoke(Class<T> type, Method deploymentMethod) {
        try {
            return type.cast(deploymentMethod.invoke(null));
        } catch (Exception e) {
            throw new RuntimeException("Could not invoke deployment method: " + deploymentMethod, e);
        }
    }

}
