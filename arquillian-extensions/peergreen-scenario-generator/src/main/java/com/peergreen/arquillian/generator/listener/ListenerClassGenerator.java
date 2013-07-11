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

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.RemappingClassAdapter;

import com.peergreen.arquillian.generator.ClassRemapper;

/**
 * Generates a listener bytecode from a component class
 * @author Florent Benoit
 */
public class ListenerClassGenerator {

    public byte[] generate(InputStream is) throws IOException {
        ClassReader cr = new ClassReader(is);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        RemappingClassAdapter remappingClassAdapter = new RemappingClassAdapter(cw, new ClassRemapper("Listener"));
        ListenerClassAdapter classAdapter = new ListenerClassAdapter(remappingClassAdapter);
        cr.accept(classAdapter, ClassReader.EXPAND_FRAMES);
        return cw.toByteArray();
    }
}
