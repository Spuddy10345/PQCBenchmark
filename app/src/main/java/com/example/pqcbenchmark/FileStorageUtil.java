package com.example.pqcbenchmark;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// saves BM result file, needed for recent android versions
public class FileStorageUtil {
    private static final String TAG = "FileStorageUtil";
    private static final String BENCHMARK_DIR = "PQCBenchmark";

    // saves file according to android version @param context Application context @param fileName Name of the file (without path) @param content String content to write to the file @return The Uri of the created file, or null if operation failed
public static Uri saveFile(Context context, String fileName, String content) {
        return saveFileWithMediaStore(context, fileName, content);
    }

    // saves file using mediastore
private static Uri saveFileWithMediaStore(Context context, String fileName, String content) {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
        contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + BENCHMARK_DIR);

        Uri uri = null;
        OutputStream outputStream = null;

        try {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                outputStream = resolver.openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(content.getBytes());
                    outputStream.close();
                    Log.d(TAG, "File saved successfully: " + uri.toString());
                    return uri;
                }
            }
            Log.e(TAG, "Failed to create MediaStore document");
            return null;
        } catch (IOException e) {
            Log.e(TAG, "Error saving file with MediaStore", e);
            //clean up if exception and URI
            if (uri != null) {
                resolver.delete(uri, null, null);
            }
            return null;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
            }
        }
    }


    // add timestamp
public static String generateTimestampedFilename(String prefix, String extension) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return prefix + "_" + timestamp + "." + extension;
    }

}