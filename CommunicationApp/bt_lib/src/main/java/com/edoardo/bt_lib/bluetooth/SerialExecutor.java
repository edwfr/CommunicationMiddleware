package com.edoardo.bt_lib.bluetooth;

import android.support.annotation.NonNull;

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

    public synchronized void execute(@NonNull final Runnable runnable) {
        mTasks.offer(() -> {
            try {
                    runnable.run();
                } finally {
                    scheduleNext();
                }
        });
        if (mRunnableActive == null) {
            scheduleNext();
        }
    }

    private synchronized void scheduleNext() {
        if ((mRunnableActive = mTasks.poll()) != null) {
            mExecutor.execute(mRunnableActive);
        }
    }
}
