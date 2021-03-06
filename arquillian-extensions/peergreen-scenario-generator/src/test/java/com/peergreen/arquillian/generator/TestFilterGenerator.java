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

import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.Filter;
import javax.servlet.annotation.WebFilter;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.peergreen.arquillian.generator.filter.FilterClassGenerator;

public class TestFilterGenerator extends CommonTest {

    private Filter filter;


    @Test
    public void testGenerate() throws IOException, InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException {
        byte[] bytecode = new FilterClassGenerator().generate(getComponentInputStream());
        Class<?> clazz = loadClass(Component.class.getPackage().getName().concat(".ComponentFilter"), bytecode);

        // it should implements Filter
        this.filter = (Filter) clazz.newInstance();
        Method method = filter.getClass().getDeclaredMethod("helloWorld");
        String result = (String) method.invoke(filter);
        Assert.assertEquals(result, "hello");
    }


    @Test(dependsOnMethods="testGenerate")
    public void testAnnotation()  {
        WebFilter webFilter = filter.getClass().getAnnotation(WebFilter.class);
        assertNotNull(webFilter);
        // There is a value
        assertNotNull(webFilter.value());
        assertNotEquals(webFilter.value(), "");
    }


}
