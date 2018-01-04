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
