/*
 *  Copyright 2018 David Sargent
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package davidsar.gent.stubjars.utils;

import static davidsar.gent.stubjars.utils.Streams.makeFor;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class StreamsTest {
    @Test(expected = NullPointerException.class)
    public void makeFor_EnumNull() {
        //noinspection ConstantConditions
        makeFor(null);
    }

    @Test
    public void makeFor_EmptyEnumeration() {
        Enumeration<Object> enumeration = Collections.emptyEnumeration();
        Stream<Object> stream = makeFor(enumeration);

        Assert.assertNotNull(stream);
        final List<Object> collect = stream.collect(Collectors.toList());
        Assert.assertEquals(collect.size(), 0);
    }

    @Test
    public void makeFor_Normal() {
        @SuppressWarnings("JdkObsolete") // for the enumeration
            Vector<Object> enumeration = new Vector<>();
        enumeration.add("cow");
        enumeration.add("dog");
        enumeration.add("pig");

        Stream<Object> stream = makeFor(enumeration.elements());

        Assert.assertNotNull(stream);
        AtomicInteger index = new AtomicInteger();
        Assert.assertTrue(stream.allMatch(value -> enumeration.get(index.getAndIncrement()).equals(value)));
        Assert.assertEquals(enumeration.size(), index.get());
    }
}