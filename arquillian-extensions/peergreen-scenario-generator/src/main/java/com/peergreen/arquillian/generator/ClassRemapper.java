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

import org.objectweb.asm.commons.Remapper;

/**
 * Allows to rename some ASM fields/type
 * @author Florent Benoit
 */
public class ClassRemapper extends Remapper {

    private final String suffix;

    public ClassRemapper(String suffix) {
        this.suffix = suffix;
    }


    @Override
    public String map(String name) {
        if (name.endsWith("/Component")) {
            return name.concat(suffix);
        }
        return name;
    }
}
