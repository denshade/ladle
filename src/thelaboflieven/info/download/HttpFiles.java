package thelaboflieven.info.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class HttpFiles {
    private HttpFiles() {
    }

    public static void download(String url, File target) throws IOException {
        var connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "ladle");
        connection.connect();

        var status = connection.getResponseCode();
        if (status >= 400) {
            throw new IOException("HTTP " + status + " downloading " + url);
        }

        try (InputStream input = connection.getInputStream()) {
            Files.copy(input, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }
    }
}
