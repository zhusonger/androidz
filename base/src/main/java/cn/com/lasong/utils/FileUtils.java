package cn.com.lasong.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Author: zhusong
 * Email: song.zhu@lasong.com.cn
 * Date: 2020/7/29
 * Description:
 */
public class FileUtils {
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getFilePath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * 压缩图片文件
     *
     * @param file 图片yuan源路径
     * @param maxSize 图片最大文件大小
     * @return
     */
    public static File compressJpeg(File file, long maxSize, String dstDir, int step) {
        long fileLength = file.length();
        // 不需要压缩
        if (fileLength <= maxSize) {
            return file;
        }

        if (TextUtils.isEmpty(dstDir)) {
            dstDir = file.getParent();
        }
        File packedFile = new File(dstDir,
                "packed_" + file.getName());
        // 已经存在压缩过的, 不再处理, 直接返回
        Bitmap bitmap = null;
        FileOutputStream fos = null;
        // 先直接压缩图看看
        try {
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int percent = 100 - step;
            long outSize = fileLength;
            while (outSize > maxSize) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, percent, bos);
                outSize = bos.size();
                percent -= step;
            }
            fos = new FileOutputStream(packedFile);
            fos.write(bos.toByteArray());
            bos.reset();
        } catch (Throwable e) {
            ILog.e("COMPRESS JPG", e);
            if (packedFile.exists()) {
                packedFile.delete();
            }
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        // 太大了, 内存放不下, 先进行缩放
        if (null == bitmap) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            int outHeight = options.outHeight;
            int outWidth = options.outWidth;
            int inSampleSize = 1;
            long outSize = fileLength;
            while (outSize > maxSize) {
                inSampleSize *= 2;
                outHeight = outHeight / 2;
                outWidth = outWidth / 2;
                outSize = outHeight * outWidth * 4;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            try {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);

                if (packedFile.exists()) {
                    packedFile.delete();
                }
                try {
                    packedFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fos = new FileOutputStream(packedFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            } catch (Throwable e) {
                e.printStackTrace();
                return null;
            } finally {
                if (null != fos) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        if (null != bitmap && bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }

        return packedFile;
    }
}
