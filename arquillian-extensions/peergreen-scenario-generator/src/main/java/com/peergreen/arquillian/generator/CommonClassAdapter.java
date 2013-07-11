/**
 * Copyright 2013 Peergreen S.A.S. All rights reserved.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.peergreen.arquillian.generator;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Common stuff for ASM class adapters.
 * @author Florent Benoit
 */
public class CommonClassAdapter extends ClassVisitor implements Opcodes {

    private String componentClassName;

    private final Class<?> superClass;

    public CommonClassAdapter(ClassVisitor cv, Class<?> superClass) {
        super(ASM4, cv);
        this.superClass = superClass;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.componentClassName = name;
        // Gets the super class name name of the class
       super.visit(version, ACC_PUBLIC, name, signature, Type.getInternalName(superClass), interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if ("<init>".equals(name)) {
            return callToSuperClass();
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }


    public MethodVisitor callToSuperClass() {
        MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(superClass), "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        return mv;
    }

    public String getComponentClassName() {
        return componentClassName;
    }

}
