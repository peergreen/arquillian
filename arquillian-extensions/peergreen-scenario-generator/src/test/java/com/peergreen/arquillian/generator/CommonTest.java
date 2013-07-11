/**
 * Copyright 2013 Peergreen S.A.S.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.peergreen.arquillian.generator;

import java.io.InputStream;

import org.testng.annotations.BeforeMethod;

public class CommonTest {

    private InputStream componentInputStream;



    public InputStream getComponentInputStream() {
        return componentInputStream;
    }

    @BeforeMethod
    public void loadComponentStream() {
        this.componentInputStream = Component.class.getResourceAsStream("/".concat(Component.class.getName().replace(".", "/")).concat(".class"));

    }

    protected Class loadClass(String className, byte[] b) {
        Class clazz = null;
        try {
            ClassLoader loader = TestServletGenerator.class.getClassLoader();
            Class cls = Class.forName("java.lang.ClassLoader");
            java.lang.reflect.Method method =
                    cls.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class, int.class });

            // protected method invocaton
            method.setAccessible(true);
            try {
                Object[] args = new Object[] { className, b, new Integer(0), new Integer(b.length)};
                clazz = (Class) method.invoke(loader, args);
            } finally {
                method.setAccessible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clazz;
    }
}
