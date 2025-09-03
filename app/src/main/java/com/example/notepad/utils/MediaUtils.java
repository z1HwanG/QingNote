package com.example.notepad.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MediaUtils {
    
    private static final String TAG = "MediaUtils";
    
    /**
     * 创建选择图片的Intent
     * @return 用于选择图片的Intent
     */
    public static Intent createImagePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return intent;
    }

    /**
     * 检查是否是FileProvider URI
     */
    public static boolean isFileProviderUri(Uri uri) {
        return uri != null && 
               uri.getAuthority() != null && 
               uri.getAuthority().contains("fileprovider");
    }

    /**
     * 检查是否是外部内容URI
     */
    public static boolean isExternalContentUri(Uri uri) {
        return uri != null && 
               ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && 
               !isFileProviderUri(uri);
    }

    /**
     * 获取永久URI权限 - 仅用于外部内容URI
     */
    public static void takePersistableUriPermission(Context context, Uri uri) {
        if (uri == null) {
            Log.w(TAG, "URI为空，跳过获取持久化权限");
            return;
        }

        // 只处理外部内容URI
        if (!isExternalContentUri(uri)) {
            Log.d(TAG, "非外部内容URI，跳过获取持久化权限: " + uri);
            return;
        }

        try {
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(uri, flags);
            Log.d(TAG, "已获取持久化URI权限: " + uri);
        } catch (SecurityException e) {
            Log.e(TAG, "获取持久化权限失败: " + uri, e);
        }
    }

    /**
     * 保存URI指向的文件到应用私有目录
     */
    public static boolean saveUriToFile(Context context, Uri sourceUri, File destinationFile) {
        if (sourceUri == null || destinationFile == null) {
            Log.e(TAG, "源URI或目标文件为空");
            return false;
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            // 如果是外部内容URI，尝试获取持久化权限
            if (isExternalContentUri(sourceUri)) {
                try {
                    takePersistableUriPermission(context, sourceUri);
                } catch (Exception e) {
                    Log.w(TAG, "获取持久化权限失败，尝试直接读取", e);
                }
            }
            
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                Log.e(TAG, "无法打开输入流: " + sourceUri);
                return false;
            }

            // 确保目标目录存在
            File parentDir = destinationFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    Log.e(TAG, "无法创建目标目录: " + parentDir);
                    return false;
                }
            }

            outputStream = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            
            Log.d(TAG, "文件保存成功: " + destinationFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "保存文件失败: " + sourceUri, e);
            return false;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "关闭流失败", e);
            }
        }
    }

    /**
     * 获取文件的MIME类型
     */
    public static String getMimeType(Context context, Uri uri) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            String mimeType = contentResolver.getType(uri);
            
            if (mimeType == null) {
                String path = uri.getPath();
                if (path != null) {
                    int extensionIndex = path.lastIndexOf(".");
                    if (extensionIndex > 0) {
                        return path.substring(extensionIndex + 1);
                    }
                }
                return "jpg";
            }
            
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String extension = mime.getExtensionFromMimeType(mimeType);
            return extension != null ? extension : "jpg";
        } catch (Exception e) {
            Log.e(TAG, "获取MIME类型失败", e);
            return "jpg";
        }
    }

    /**
     * 检查是否需要请求权限
     */
    public static boolean needsPermissionRequest() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    /**
     * 注册图片选择结果处理器
     */
    public static ActivityResultLauncher<Intent> registerImagePicker(
            FragmentActivity activity,
            ImagePickerCallback callback) {
        return activity.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            // 只对外部内容URI获取持久化权限
                            if (isExternalContentUri(uri)) {
                                takePersistableUriPermission(activity, uri);
                            }
                            Log.d(TAG, "选择的图片URI: " + uri);
                            callback.onImageSelected(uri);
                        } catch (Exception e) {
                            Log.e(TAG, "处理选中的图片失败", e);
                        }
                    } else {
                        Log.e(TAG, "未获取到有效图片URI");
                    }
                }
            }
        );
    }

    public interface ImagePickerCallback {
        void onImageSelected(Uri uri);
    }
} 