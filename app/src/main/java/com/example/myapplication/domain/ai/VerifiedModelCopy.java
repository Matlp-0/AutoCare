package com.example.myapplication.domain.ai;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

/** Bounded, verified and atomic replacement. The old model survives failed transfers. */
public final class VerifiedModelCopy {
    private VerifiedModelCopy() { }

    public static void copy(InputStream input, File target, long expectedBytes, String expectedSha256,
                            AtomicBoolean cancelled, LongConsumer progress) throws IOException {
        File partial = new File(target.getParentFile(), target.getName() + ".part");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long total = 0;
            try (FileOutputStream output = new FileOutputStream(partial)) {
                byte[] buffer = new byte[128 * 1024];
                while (true) {
                    check(cancelled);
                    int read = input.read(buffer);
                    if (read == -1) break;
                    total += read;
                    if (total > expectedBytes) throw new IOException("Arquivo maior que o modelo indicado.");
                    output.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                    progress.accept(total);
                }
                output.getFD().sync();
            }
            check(cancelled);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            if (total != expectedBytes || !hex.toString().equals(expectedSha256))
                throw new IOException("Arquivo incompleto ou diferente do modelo indicado. Baixe o Qwen3 1.7B Q4_K_M pelo link do app.");
            Files.move(partial.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } finally {
            Files.deleteIfExists(partial.toPath());
        }
    }
    private static void check(AtomicBoolean cancelled) {
        if (cancelled.get()) throw new CancellationException("Operação cancelada.");
    }
}
