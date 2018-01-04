package me.davidsargent.stubjars.components.writer;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Writer {
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
            e.printStackTrace();
        }
        dataCache = null;
    }
}
