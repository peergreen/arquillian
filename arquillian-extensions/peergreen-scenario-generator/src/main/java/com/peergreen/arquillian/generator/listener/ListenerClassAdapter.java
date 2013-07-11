/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.peergreen.arquillian.generator.listener;

import javax.servlet.annotation.WebListener;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import com.peergreen.arquillian.generator.CommonClassAdapter;

/**
 * Transform component into a listener
 * @author Florent Benoit
 */
public class ListenerClassAdapter extends CommonClassAdapter {

    public ListenerClassAdapter(ClassVisitor cv) {
      super(cv, PeergreenTestRunnerListener.class);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        // Add @WebListener(value="/MyComponentListener") annotation on the class
        AnnotationVisitor annotationVisitor = cv.visitAnnotation(Type.getDescriptor(WebListener.class), true);
        annotationVisitor.visit("value", "/".concat(getComponentClassName().concat("Listener")));
        annotationVisitor.visitEnd();

    }
  }
