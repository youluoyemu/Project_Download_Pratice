package com.example.youluoyemu2011.project_download_pratice_170727;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by youluoyemu2011 on 2017/7/27.
 * Email: youluoyemu2011@126.com
 */

/**
 * AsyncTask三个泛型参数分别代表传入的
 * - 参数类型 比如传入的Url
 * - 进度类型 一般就是整型
 * - 返回结果类型 根据返回结果实现不同的操作，比如显示下载成功、失败、暂停、取消
 */
public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    public static final String DOWNLOAD_URL = "";

    // 下载任务的四种情况
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private boolean isPaused;
    private boolean isCanceled;
    private OnDownloadListener mOnDownloadListener;
    private int lastProgress;

    public interface OnDownloadListener {
        void onSuccess();

        void onFailed();

        void onPaused();

        void onCanceled();

        void onProgress(int progress);
    }

    public DownloadTask(OnDownloadListener mOnDownloadListener) {
        this.mOnDownloadListener = mOnDownloadListener;
    }

    @Override
    protected Integer doInBackground(String... params) {

        File localFile;
        RandomAccessFile randomAccessFile = null;
        InputStream is = null;
        long downloadLength = 0;
        long contentLength = 0;

        String downloadUrl = params[0];
        String localFileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
        String localFileDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        localFile = new File(localFileDirectory + localFileName);
        if (localFile.exists()) {
            downloadLength = localFile.length();
        }

        try {
            contentLength = getContentlength(downloadUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 判断是否需要下载，下载已经失败或者成功都不需要继续下载
        if (contentLength == 0) {
            return TYPE_FAILED;
        } else if (contentLength == downloadLength) {
            return TYPE_SUCCESS;
        }

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().addHeader("RANGE", "bytes=" + downloadLength + "-").url(DOWNLOAD_URL).build();
        Call call = okHttpClient.newCall(request);
        try {
            Response response = call.execute();
            if (response != null) {
                is = response.body().byteStream();
                randomAccessFile = new RandomAccessFile(localFile, "rw");
                // 跳过已经下载的字节
                randomAccessFile.seek(downloadLength);
                byte[] bytes = new byte[1024];
                int length;
                int total = 0;
                while ((length = is.read(bytes)) != -1) {

                    if (isCanceled) {
                        return TYPE_FAILED;
                    } else if (isPaused) {
                        return TYPE_PAUSED;
                    }

                    randomAccessFile.write(bytes, 0, length);
                    total += length;
                    int progress = (int) ((total + downloadLength) * 100 / contentLength);
                    publishProgress(progress);
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (isCanceled && localFile != null) {
                localFile.delete();
            }
        }

        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress) {
            mOnDownloadListener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer) {
            case TYPE_SUCCESS:{
                mOnDownloadListener.onSuccess();
            }
            break;
            case TYPE_FAILED:{
                mOnDownloadListener.onFailed();
            }
            break;
            case TYPE_PAUSED:{
                mOnDownloadListener.onPaused();
            }
            break;
            case TYPE_CANCELED:{
                mOnDownloadListener.onCanceled();
            }
            break;
            default:
                break;
        }
    }

    private long getContentlength(String downloadUrl) throws IOException {

        long contentLength = 0;

        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = okHttpClient.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            contentLength = response.body().contentLength();
            response.close();
        }

        return contentLength;
    }

    public void cancelDownload() {
        isCanceled = true;
    }

    public void pauseDownload() {
        isPaused = true;
    }

}
