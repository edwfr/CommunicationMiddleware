package com.edoardo.bt_lib.bluetooth;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

public class SerialExecutor implements Executor {
    private final Queue<Runnable> mTasks;
    private final Executor mExecutor;
    private Runnable mRunnableActive;

    SerialExecutor(Executor executor) {
        mTasks = new ArrayDeque<>();
        mExecutor = executor;
    }

    public synchronized void execute(final Runnable runnable) {
        mTasks.offer(new Runnable() {
            public void run() {
//                try {
//                    Thread.sleep(1);
//                    runnable.run();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                } finally {
//                    scheduleNext();
//                }
                try {
//                    Thread.sleep(1);
                    runnable.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        if (mRunnableActive == null) {
            scheduleNext();
        }
    }

    protected synchronized void scheduleNext() {
        if ((mRunnableActive = mTasks.poll()) != null) {
            mExecutor.execute(mRunnableActive);
        }
    }
}
