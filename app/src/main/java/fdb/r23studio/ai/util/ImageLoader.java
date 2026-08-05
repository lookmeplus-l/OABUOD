package fdb.r23studio.ai.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ImageLoader() {
    }

    public static void load(String source, ImageView imageView) {
        if (source == null) return;
        if (source.startsWith("data:")) {
            loadDataUri(source, imageView);
        } else if (source.startsWith("http://") || source.startsWith("https://")) {
            loadUrl(source, imageView);
        }
    }

    private static void loadDataUri(final String source, final ImageView imageView) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int comma = source.indexOf(',');
                    String base64 = source.substring(comma + 1);
                    byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bitmap != null) {
                        MAIN.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    private static void loadUrl(final String source, final ImageView imageView) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(source).openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(15000);
                    connection.setRequestProperty("Referer", "https://www.doubao.com/");
                    InputStream is = connection.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(is);
                    if (bitmap != null) {
                        MAIN.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (IOException ignored) {
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        });
    }
}
