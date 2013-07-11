/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.peergreen.arquillian.generator.filter;

import javax.servlet.annotation.WebFilter;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import com.peergreen.arquillian.generator.CommonClassAdapter;

/**
 * Transform component into a filter
 * @author Florent Benoit
 */
public class FilterClassAdapter extends CommonClassAdapter {

    public FilterClassAdapter(ClassVisitor cv) {
      super(cv, PeergreenTestRunnerFilter.class);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        // Add @WebFilter(value="/MyComponentListener") annotation on the class
        AnnotationVisitor annotationVisitor = cv.visitAnnotation(Type.getDescriptor(WebFilter.class), true);
        AnnotationVisitor av1 = annotationVisitor.visitArray("value");
        av1.visit(null, "/".concat(getComponentClassName().concat("Filter")));
        av1.visitEnd();
        annotationVisitor.visitEnd();

    }
  }
