package com.example.myapplication.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Fotos de veículo guardadas dentro do app.
 *
 * <p>A imagem escolhida é copiada, não referenciada: a Uri do seletor de fotos
 * vale só para a sessão e o arquivo original pode sumir da galeria. A cópia é
 * reduzida porque a foto só aparece em miniatura e no cabeçalho.
 */
public final class ImageStore {

    private static final String DIRECTORY = "vehicles";
    private static final int MAX_DIMENSION = 1024;
    private static final int QUALITY = 85;

    private ImageStore() {
    }

    /** Copia a imagem para o app e devolve o caminho do arquivo gravado. */
    public static String saveVehiclePhoto(Context context, Uri source) throws IOException {
        File directory = new File(context.getFilesDir(), DIRECTORY);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta de fotos");
        }

        Bitmap bitmap = decodeSampled(context, source, MAX_DIMENSION);
        if (bitmap == null) {
            throw new IOException("Imagem inválida");
        }
        File file = new File(directory, "vehicle_" + System.currentTimeMillis() + ".jpg");
        try (OutputStream output = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, output);
        } finally {
            bitmap.recycle();
        }
        return file.getAbsolutePath();
    }

    /** Lê a foto já gravada; devolve null quando o arquivo não existe mais. */
    public static Bitmap load(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(path);
    }

    public static void delete(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }

    /** Decodifica já reduzida: foto de celular inteira na memória é desperdício. */
    private static Bitmap decodeSampled(Context context, Uri source, int maxDimension)
            throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }

        int sample = 1;
        int largest = Math.max(bounds.outWidth, bounds.outHeight);
        while (largest / sample > maxDimension) {
            sample *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        try (InputStream input = context.getContentResolver().openInputStream(source)) {
            return BitmapFactory.decodeStream(input, null, options);
        }
    }
}
