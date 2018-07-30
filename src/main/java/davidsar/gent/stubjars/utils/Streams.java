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

import org.jetbrains.annotations.NotNull;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Streams {
    /**
     * @param e   the {@link Enumeration} to back to {@link Stream}
     * @param <T> type of objects contained in the {@code Enumeration}
     * @return a new {@code Stream } from the elements of the {@code Enumeration}
     * @implNote from https://stackoverflow.com/questions/33242577/how-do-i-turn-a-java-enumeration-into-a-stream
     */
    @NotNull
    public static <T> Stream<T> makeFor(@NotNull Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                            @Override
                            public T next() {
                                return e.nextElement();
                            }

                            @Override
                            public boolean hasNext() {
                                return e.hasMoreElements();
                            }
                        },
                        Spliterator.ORDERED), false);
    }
}