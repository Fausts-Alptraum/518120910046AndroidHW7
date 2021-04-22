package com.bytedance.mediademo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemCameraActivity extends AppCompatActivity {
    private int REQUEST_CODE_TAKE_PHOTO = 1001;
    private int REQUEST_CODE_TAKE_PHOTO_PATH = 1002;
    private int PERMISSION_REQUEST_CAMERA_CODE = 1003;
    private int PERMISSION_REQUEST_CAMERA_PATH_CODE = 1004;

    private ImageView imageView;
    private String takeImagePath;
    private String filename;
    private File mediaFile;

    public static void startUI(Context context) {
        Intent intent = new Intent(context, SystemCameraActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_camera);
        imageView = findViewById(R.id.iv_img);
    }

    public void takePhoto(View view) {
        requestCameraPermission();
    }

    public void takePhotoUsePath(View view) {
        requestCameraAndSDCardPermission();
    }

    private void requestCameraAndSDCardPermission() {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (hasCameraPermission) {
            takePhotoUsePathHasPermission();
        } else {
            String[] permissions = new String[]{Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CAMERA_PATH_CODE);
        }
    }

    private void takePhotoUsePathHasPermission() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takeImagePath = getOutputMediaPath();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, PathUtils.getUriForFile(this,takeImagePath));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO_PATH);
        }
    }

    private String getOutputMediaPath() {
        // 以下方法（demo中的方法）写入目录为应用私密，无法被其他应用访问，因此无法被系统相册识别
        File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // Android 10 以上版本废弃了下述方法，应用程序不再能直接访问外部存储设备
        // File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        filename =  "IMG_" + timeStamp + ".jpg";
        mediaFile = new File(mediaStorageDir, filename);
        if (!mediaFile.exists()) {
            mediaFile.getParentFile().mkdirs();
        }
        return mediaFile.getAbsolutePath();
    }


    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CAMERA_CODE);
        } else {
            takePhotoHasPermission();
        }
    }

    private void takePhotoHasPermission() {
        // todo （已完成）
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhotoHasPermission();
            } else {
                Toast.makeText(this, "权限获取失败", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CAMERA_PATH_CODE) {
            boolean hasPermission = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    hasPermission = false;
                    break;
                }
            }
            if (hasPermission) {
                takePhotoUsePathHasPermission();
            } else {
                Toast.makeText(this, "权限获取失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            // todo （已完成）
            //若不用path
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(bitmap);

        } else if (requestCode == REQUEST_CODE_TAKE_PHOTO_PATH && resultCode == RESULT_OK) {
            //TODO extra 为刚刚拍摄的图片在mediastore中注册uri,发送广播，更新系统相册
            try{
                MediaStore.Images.Media.insertImage(getContentResolver(),takeImagePath,filename,null);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mediaFile)));
            // todo （已完成）
            //获取ImageView控件宽高
            int targetWidth = imageView.getWidth();
            int targetHeight = imageView.getHeight();
            //创建Options,设置inJustDecodeBounds为true，只解码图片宽高信息
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            // 如果该值设为true那么将不返回实际的bitmap，也不给其分配内存空间，这样就避免内存溢出了。
            // 但是允许我们查询图片的信息，其中包括图片大小信息。
            BitmapFactory.decodeFile(takeImagePath, options);
            int photoWidth = options.outWidth;
            int photoHeight = options.outHeight;
            //计算图片和控件的缩放比例，并设置给Options,然后inJustDecodeBounds置为false，解码真正的图片信息
            int scaleFactor = Math.min(photoWidth / targetWidth, photoHeight / targetHeight);
            options.inJustDecodeBounds = false;
            options.inSampleSize = scaleFactor;
            Bitmap bitmap = BitmapFactory.decodeFile(takeImagePath, options);
            Bitmap rotateBitmap = PathUtils.rotateImage(bitmap,takeImagePath);
            imageView.setImageBitmap(rotateBitmap);


        }
    }


}