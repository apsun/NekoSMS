package com.crossbowffs.nekosms.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncUtils {
    private static Executor EXECUTOR;
    private static Handler HANDLER;

    @FunctionalInterface
    public interface Supplier<T> {
        T get();
    }

    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t);
    }

    public static <T> void run(@NonNull Supplier<T> supplier, @NonNull Consumer<T> consumer) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new RuntimeException("AsyncUtils.run() must be called on the main thread");
        }
        if (EXECUTOR == null) {
            EXECUTOR = Executors.newSingleThreadExecutor();
        }
        if (HANDLER == null) {
            HANDLER = new Handler(Looper.getMainLooper());
        }
        EXECUTOR.execute(() -> {
            T result = supplier.get();
            HANDLER.post(() -> consumer.accept(result));
        });
    }
}
