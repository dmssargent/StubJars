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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A specialized {@link Thread} for operations with a {@link Writer}.
 *
 * @see Writer
 */
public class WriterThread extends Thread implements Runnable {
    private final ArrayBlockingQueue<Writer> writersToProcess;
    private Thread runningThread = null;
    private volatile boolean stop = false;

    public WriterThread() {
        super();
        writersToProcess = new ArrayBlockingQueue<>(5000);
    }

    public void done() {
        stop = true;
        // runningThread.interrupt();
    }

    void addWriter(Writer writer) {
        try {
            writersToProcess.put(writer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Waits for all of the pending I/O operations to complete.
     *
     * @throws InterruptedException if the current {@link Thread} encounters an I/O exception
     */
    public void waitForCompletion() throws InterruptedException {
        if (runningThread == null) {
            return;
        }
        runningThread.join();
    }

    private void internalRun() {
        while (!Thread.currentThread().isInterrupted() && !stop) {
            while (!(writersToProcess.isEmpty() && stop)) {
                Writer writer;
                try {
                    writer = writersToProcess.poll(100, TimeUnit.MILLISECONDS);
                    if (writer != null) {
                        writer.threadWrite();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void run() {
        runningThread = Thread.currentThread();
        internalRun();
        runningThread = null;
    }
}
