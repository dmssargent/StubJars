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

package davidsar.gent.stubjars;

import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {

    @Test(expected = NullPointerException.class)
    public void arrayToCommaSeparatedList_NullElement() {
        final String[] nullArray = {null};
        Utils.arrayToCommaSeparatedList(nullArray, String::toString);
    }

    @Test
    public void arrayToCommaSeparatedList_NoElements() {
        final String[] emptyArray = {};
        final String result = Utils.arrayToCommaSeparatedList(emptyArray, String::toString);

        Assert.assertEquals(result, "");
    }

    @Test
    public void arrayToCommaSeparatedList_SingleElement() {
        final String element1 = "moo";

        final String result = Utils.arrayToCommaSeparatedList(new String[] {element1}, String::toString);

        Assert.assertEquals(result, element1);
    }

    @Test
    public void arrayToCommaSeparatedList_TwoElements() {
        final String[] elements = {"cat", "dog"};
        final String result = Utils.arrayToCommaSeparatedList(elements, String::toString);

        Assert.assertEquals(result, "cat, dog");
    }

    @Test
    public void arrayToCommaSeparatedList_ElementsWithEmpty() {
        final String[] elements = {"cat", ""};
        final String result = Utils.arrayToCommaSeparatedList(elements, String::toString);

        Assert.assertEquals(result, "cat, ");
    }
}