package com.bytedance.mediademo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SystemRecordActivity extends AppCompatActivity {
    private final static int PERMISSION_REQUEST_CODE = 1001;
    private final static int REQUEST_CODE_RECORD = 1002;

    private String mp4Path = "";
    private String filename;
    private VideoView mVideoView;
    private File mediaFile;
    public static void startUI(Context context) {
        Intent intent = new Intent(context, SystemRecordActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_record);
        mVideoView = findViewById(R.id.videoview);
    }

    public void record(View view) {
        requestPermission();
    }

    private void requestPermission() {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean hasAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        if (hasCameraPermission && hasAudioPermission) {
            recordVideo();
        } else {
            List<String> permission = new ArrayList<String>();
            if (!hasCameraPermission) {
                permission.add(Manifest.permission.CAMERA);
            }
            if (!hasAudioPermission) {
                permission.add(Manifest.permission.RECORD_AUDIO);
            }
            ActivityCompat.requestPermissions(this, permission.toArray(new String[permission.size()]), PERMISSION_REQUEST_CODE);
        }

    }

    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        mp4Path = getOutputMediaPath();
        intent.putExtra(MediaStore.EXTRA_OUTPUT,PathUtils.getUriForFile(this,mp4Path));
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY,1);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // todo （已完成）
            startActivityForResult(intent,REQUEST_CODE_RECORD);

        }
    }

    private String getOutputMediaPath() {
        // 以下方法写入目录为应用私密，无法被其他应用访问，因此无法被系统相册识别
        File mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // Android 10 以上版本废弃了下述方法，应用程序不再能直接访问外部存储设备
        // File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        filename = "IMG_" + timeStamp + ".mp4";
        mediaFile = new File(mediaStorageDir, filename);
        if (!mediaFile.exists()) {
            mediaFile.getParentFile().mkdirs();
        }
        return mediaFile.getAbsolutePath();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermission = true;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                hasPermission = false;
                break;
            }
        }
        if (hasPermission) {
            recordVideo();
        } else {
            Toast.makeText(this, "权限获取失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // todo （已完成）
        if(requestCode == REQUEST_CODE_RECORD && resultCode == RESULT_OK){
            play(); //播放视频
            //TODO extra 主动扫描视频文件
            MediaScannerConnection mMediaScannerConnection = new MediaScannerConnection(this,null);
            mMediaScannerConnection.connect();
            mMediaScannerConnection.scanFile(mp4Path, "video/mp4");
            mMediaScannerConnection.disconnect();
        }

    }


    private void play() {
        mVideoView.setVideoPath(mp4Path);
        mVideoView.start();

        // TODO extra: 点击暂停、播放，循环播放
        //点击暂停、播放
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mVideoView.isPlaying()){
                    mVideoView.pause();
                }else {
                    mVideoView.start();
                }
                return false;
            }
        });
        //循环播放
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start();
                mp.setLooping(true);
            }
        });
    }
    //TODO extra 发送广播，更新系统相册
    private static void scan_file(Context context, String imgFileName) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(imgFileName);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }
}