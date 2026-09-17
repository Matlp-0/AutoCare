package com.example.myapplication.data.ai;

import java.nio.charset.StandardCharsets;

/** One request per instance. generate/close run on the worker; cancel may run on the UI thread. */
public final class NativeLlama implements AutoCloseable {
    static { System.loadLibrary("autocare_ai"); }
    public interface Listener { void onText(byte[] utf8); }
    private long handle = create();

    public int generate(String path, String prompt, Listener listener) {
        return generateNative(handle, path.getBytes(StandardCharsets.UTF_8),
                prompt.getBytes(StandardCharsets.UTF_8), listener);
    }
    public synchronized void cancel() {
        if (handle != 0) cancelNative(handle);
    }
    @Override public synchronized void close() {
        if (handle != 0) { destroy(handle); handle = 0; }
    }
    private static native long create();
    private static native void cancelNative(long handle);
    private static native void destroy(long handle);
    private static native int generateNative(long handle, byte[] path, byte[] prompt, Listener listener);
}
