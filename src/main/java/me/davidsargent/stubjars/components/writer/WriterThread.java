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

package me.davidsargent.stubjars.components.writer;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @see Writer
 */
public class WriterThread extends Thread implements Runnable {
    private final ArrayBlockingQueue<Writer> writersToProcess;
    private volatile boolean stop = false;

    public WriterThread() {
        super();
        writersToProcess = new ArrayBlockingQueue<>(5000);
    }

    public void done() {
        stop = true;
    }

    void addWriter(Writer writer) {
        try {
            writersToProcess.put(writer);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && !(writersToProcess.isEmpty() && stop)) {
            Writer writer = writersToProcess.poll();
            if (writer == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                continue;
            }

            writer._threadWrite();
        }
    }
}
