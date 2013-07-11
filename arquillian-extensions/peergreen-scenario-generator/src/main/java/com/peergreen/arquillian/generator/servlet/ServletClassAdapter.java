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

import javax.servlet.annotation.WebServlet;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import com.peergreen.arquillian.generator.CommonClassAdapter;

/**
 * Adds inheritance on a component to transform it into a servlet
 * @author Florent Benoit
 */
public class ServletClassAdapter extends CommonClassAdapter {

    public ServletClassAdapter(ClassVisitor cv) {
      super(cv, PeergreenTestRunnerServlet.class);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        // Add @WebServlet(loadOnStartup=1, value="/servlet) annotation on the class
        AnnotationVisitor annotationVisitor = cv.visitAnnotation(Type.getDescriptor(WebServlet.class), true);
        annotationVisitor.visit("loadOnStartup", new Integer(1));
        AnnotationVisitor value = annotationVisitor.visitArray("value");
        value.visit(null, "/".concat(getComponentClassName().concat("Servlet")));
        value.visitEnd();
        annotationVisitor.visitEnd();

    }
  }
