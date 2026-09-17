package com.example.myapplication.data.ai;

import android.content.Context;
import android.net.Uri;

import com.example.myapplication.domain.ai.VerifiedModelCopy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

/** A single pinned model, kept out of Android backups. No vehicle data is sent over the network. */
public final class LocalModelStore {
    public static final long BYTES = 1107409472L;
    public static final String SHA256 = "b139949c5bd74937ad8ed8c8cf3d9ffb1e99c866c823204dc42c0d91fa181897";
    public static final String URL = "https://huggingface.co/unsloth/Qwen3-1.7B-GGUF/resolve/"
            + "d7f544eead698dbd1f15126ef60b45a1e1933222/Qwen3-1.7B-Q4_K_M.gguf";
    public static final String MODEL_PAGE = "https://huggingface.co/unsloth/Qwen3-1.7B-GGUF";
    private final Context context;
    private final File model;

    public LocalModelStore(Context context) {
        this.context = context.getApplicationContext();
        model = new File(context.getNoBackupFilesDir(), "ai/qwen3-1.7b-q4_k_m.gguf");
    }
    public File file() { return model; }
    public boolean isReady() { return model.isFile() && model.length() == BYTES; }

    private void prepare() throws IOException {
        File directory = model.getParentFile();
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Não foi possível criar a pasta do modelo.");
        if (directory.getUsableSpace() < BYTES + 128 * 1024 * 1024L)
            throw new IOException("Libere pelo menos 1,3 GB para guardar o modelo.");
    }
    public void download(AtomicBoolean cancelled, LongConsumer progress) throws IOException {
        prepare();
        HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept-Encoding", "identity");
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new IOException("Download indisponível. Tente novamente ou importe o arquivo.");
            if (!"https".equals(connection.getURL().getProtocol())) throw new IOException("Endereço de download inválido.");
            try (InputStream input = connection.getInputStream()) {
                VerifiedModelCopy.copy(input, model, BYTES, SHA256, cancelled, progress);
            }
        } finally { connection.disconnect(); }
    }
    public void importModel(Uri uri, AtomicBoolean cancelled, LongConsumer progress) throws IOException {
        prepare();
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IOException("Não foi possível abrir o arquivo.");
            VerifiedModelCopy.copy(input, model, BYTES, SHA256, cancelled, progress);
        }
    }
    public void delete() throws IOException {
        if (model.exists() && !model.delete()) throw new IOException("Não foi possível remover o modelo.");
    }
}
