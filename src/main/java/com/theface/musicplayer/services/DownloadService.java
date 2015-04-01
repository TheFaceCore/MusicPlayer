package com.theface.musicplayer.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by TheFace Date: 17.03.2015 Time: 11:28
 */
public class DownloadService extends IntentService{
    public static final int UPDATE_PROGRESS = 9900;
    public static final int ERROR = 9901;
    public static final int COMPLETE = 9902;

    public static final String KEY_ERROR_MSG = "error";
    public static final String KEY_PROGRESS = "progress";
    public static final String KEY_TARGET_FILENAME = "target_filename";

    private static final int BUFFER_SIZE = 4096;

    private static final int HTTP_TEMP_REDIRECT = 307;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private static final String PARAM_DOWNLOAD_URL = "download_url";
    private static final String PARAM_TARGET_FILENAME = "target_filename";
    private static final String PARAM_RECEIVER = "receiver";

    public static Bundle createDownloadInfo(String urlToDownload, String targetFileName, ResultReceiver receiver) {
        Bundle info = new Bundle();
        info.putString(PARAM_DOWNLOAD_URL, urlToDownload);
        if (targetFileName != null) {
            info.putString(PARAM_TARGET_FILENAME, targetFileName);
        }
        info.putParcelable(PARAM_RECEIVER, receiver);
        return info;
    }

    public static Bundle createDownloadInfo(String urlToDownload, ResultReceiver receiver) {
        return createDownloadInfo(urlToDownload, null, receiver);
    }

    public DownloadService() {
        super("DownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String urlToDownload = intent.getStringExtra(PARAM_DOWNLOAD_URL);
        String targetFileName = intent.getStringExtra(PARAM_TARGET_FILENAME);
        ResultReceiver receiver = intent.getParcelableExtra(PARAM_RECEIVER);
        executeDownload(urlToDownload, targetFileName, receiver, 0);
    }

    private void executeDownload(String urlToDownload, String targetFileName, ResultReceiver receiver, int retryAttemptsMade) {
        URL url;
        try {
            url = new URL(urlToDownload);
        } catch (MalformedURLException e) {
            sendErrorMsg(receiver, "URI passed is malformed.");
            return;
        }

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();

            int responseCode = connection.getResponseCode();

            switch (responseCode) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case HTTP_TEMP_REDIRECT:
                    if (retryAttemptsMade >= MAX_RETRY_ATTEMPTS) {
                        sendErrorMsg(receiver, "Too many redirects.");
                        return;
                    }
                    String location = connection.getHeaderField("Location");
                    executeDownload(location, targetFileName, receiver, ++retryAttemptsMade);
                    return;
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                sendErrorMsg(receiver, "Connection response was not OK.");
                return;
            }

            download(connection, targetFileName, receiver);
        } catch(IOException e){
            e.printStackTrace();
            sendErrorMsg(receiver, "Trouble with connection.");
        } finally{
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void download(HttpURLConnection connection, String targetFileName, ResultReceiver receiver) {
        InputStream in = null;
        OutputStream out = null;

        if (targetFileName == null) {
            targetFileName = generateFileName();
            if (targetFileName == null) {
                sendErrorMsg(receiver, "Trouble with file creation.");
                return;
            }
        }

        try {
            int fileLength = connection.getContentLength();

            in = new BufferedInputStream(connection.getInputStream());

            out = openFileOutput(targetFileName, Context.MODE_PRIVATE);

            byte[] data = new byte[BUFFER_SIZE];

            long total = 0;
            int count;
            while ((count = in.read(data)) != -1) {
                total += count;
                // publishing the progress....
                if (fileLength > 0) {
                    sendProgressUpdateMsg(receiver, (int) (total * 100 / fileLength));
                }
                out.write(data, 0, count);
            }

            sendCompleteMsg(receiver, targetFileName);
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorMsg(receiver, "Downloading trouble");
            deleteFile(targetFileName);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (out != null) {
                try {
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String generateFileName() {
        File outputDir = getFilesDir();
        File outputFile;
        try {
            outputFile = File.createTempFile("track", ".mp3", outputDir);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return outputFile.getName();
    }

    private void sendCompleteMsg(ResultReceiver receiver, String targetFileName) {
        Bundle errorInfo = new Bundle();
        errorInfo.putString(KEY_TARGET_FILENAME, targetFileName);
        receiver.send(COMPLETE, errorInfo);
    }

    private void sendErrorMsg(ResultReceiver receiver, String msg) {
        Bundle errorInfo = new Bundle();
        errorInfo.putString(KEY_ERROR_MSG, msg);
        receiver.send(ERROR, errorInfo);
    }

    private void sendProgressUpdateMsg(ResultReceiver receiver, int progress) {
        Bundle progressInfo = new Bundle();
        progressInfo.putInt(KEY_PROGRESS, progress);
        receiver.send(UPDATE_PROGRESS, progressInfo);
    }
}
