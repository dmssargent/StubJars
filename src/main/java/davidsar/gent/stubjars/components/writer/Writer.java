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

package davidsar.gent.stubjars.components.writer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Writer {
    private static final Logger log = LoggerFactory.getLogger(Writer.class);
    private final File file;
    private volatile String dataCache;
    private final WriterThread writerThread;

    /**
     *
     * @param file the {@link File} to eventually write data to
     */
    public Writer(@NotNull File file) {
        this(file, null);
    }

    /**
     * Allows a Writer to write files eventually versus once {@link #write(String)} is called
     *
     * @param file the {@link File} to eventually write data to
     * @param writerThread the {@link WriterThread} that writes data
     *
     * @see #writeDataWithDedicatedThread(String)
     */
    Writer(@NotNull File file, @Nullable WriterThread writerThread) {
        this.file = file;
        this.writerThread = writerThread;
    }

    /**
     * Write the contents of a String to a {@link File}
     *
     * @param data the data to write
     * @throws IOException the file cannot be written to
     */
    public void write(String data) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
            writer.write(data);
        }
    }

    /**
     * Write the contents of a String to a {@link File} using the dedicated {@link WriterThread} for this
     * {@code WriterThread}. The {@code File} this {@code Writer} is bound to will eventually be written, as long as this
     * object lives.
     *
     * @param data the data to write
     * @see #canUseWriterThread()
     */
    synchronized void writeDataWithDedicatedThread(@NotNull String data) {
        if (canUseWriterThread()) throw new IllegalStateException("Not bound to writing thread");
        dataCache = data;
        writerThread.addWriter(this);
    }

    /**
     * Checks if this {@link Writer} can use a dedicated {@link Thread} for writing with
     *
     * @return {@code true} if a dedicated {@code Thread} can be used
     */
    @Contract(pure = true)
    private boolean canUseWriterThread() {
        return writerThread == null;
    }

    synchronized void _threadWrite() {
        if (dataCache == null) return;

        try {
            write(dataCache);
        } catch (IOException e) {
            log.error("Encountered an error writing to file", e);
        }
        dataCache = null;
    }
}
