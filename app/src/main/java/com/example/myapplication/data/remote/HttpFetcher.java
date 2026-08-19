package com.example.myapplication.data.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Cliente HTTP mínimo (HttpURLConnection, sem dependências extras) para baixar
 * páginas públicas de plano de revisão. Timeouts curtos e limite de tamanho para
 * nunca travar a UI nem estourar memória.
 */
public class HttpFetcher {

    public static final String USER_AGENT =
            "AutoCare/1.0 (Android; app de manutenção veicular)";

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 12000;
    private static final int MAX_BYTES = 1024 * 1024;
    private static final int MAX_REDIRECTS = 4;

    public String get(String url) throws IOException {
        return get(url, 0);
    }

    private String get(String url, int redirectCount) throws IOException {
        if (redirectCount > MAX_REDIRECTS) {
            throw new IOException("Muitos redirecionamentos para " + url);
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
            connection.setRequestProperty("Accept-Language", "pt-BR,pt;q=0.9");
            connection.setRequestProperty("Accept-Encoding", "gzip");

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == 307 || status == 308) {
                String location = connection.getHeaderField("Location");
                if (location == null) {
                    throw new IOException("Redirecionamento sem destino");
                }
                return get(new URL(new URL(url), location).toString(), redirectCount + 1);
            }
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + status + " em " + url);
            }

            InputStream stream = connection.getInputStream();
            if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                stream = new GZIPInputStream(stream);
            }
            return readLimited(stream);
        } finally {
            connection.disconnect();
        }
    }

    private String readLimited(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        int total = 0;
        while ((read = stream.read(chunk)) != -1) {
            total += read;
            if (total > MAX_BYTES) {
                break;
            }
            buffer.write(chunk, 0, read);
        }
        stream.close();
        return buffer.toString("UTF-8");
    }
}
