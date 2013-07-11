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

import java.util.Iterator;
import java.util.ServiceLoader;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Peergreen server configuration.
 * @author Florent Benoit
 */
public class PeergreenConfiguration implements ContainerConfiguration {

    /**
     * Instance of the framework factory of the underlying Peergreeen server instance
     */
    private FrameworkFactory frameworkFactory;

    /**
     * Checks that we're able to find a Peergreen OSGi Framework factory
     */
    @Override
    public void validate() throws ConfigurationException {

        // Get the {@link FrameworkFactory}
        Iterator<FrameworkFactory> factories = ServiceLoader.load(FrameworkFactory.class).iterator();
        if (!factories.hasNext())
            throw new ConfigurationException("Unable to get '" + FrameworkFactory.class.getName() + " service");

        frameworkFactory = factories.next();
    }

    /**
     * @return the OSGi framework factory of peergreen.
     */
    public FrameworkFactory getFrameworkFactory() {
        return frameworkFactory;
    }

}
