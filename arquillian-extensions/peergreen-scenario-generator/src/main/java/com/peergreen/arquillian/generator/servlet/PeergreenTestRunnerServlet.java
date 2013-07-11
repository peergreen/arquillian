/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.peergreen.arquillian.generator.servlet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.http.HttpServlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.peergreen.tests.runner.Runner;

/**
 * Super class of all servlets components.
 * @author Florent Benoit
 */
public class PeergreenTestRunnerServlet extends HttpServlet {

    private static final long serialVersionUID = 6282426082344180458L;

    private Runner runner;

    private ServiceReference<?> serviceReference;

    @Resource
    private BundleContext bundleContext;

    @PostConstruct
    public void postConstruct() {
        this.serviceReference = bundleContext.getServiceReference(Runner.class.getName());
        this.runner = (Runner) bundleContext.getService(serviceReference);
        this.runner.register(this);
    }

    @PreDestroy
    public void preDestroy() {
        this.runner.unregister(this);
    }


}
