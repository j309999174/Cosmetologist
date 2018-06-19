package com.example.administrator.cosmetologist;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import id.zelory.compressor.Compressor;

public class MainActivity extends AppCompatActivity {

    private File actualImage;
    private File compressedImage;

    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;

    String targetUrl;
    WebView webview;


    static String webaddress="www.oushelun.cn";


    String picturefileName = "picturefileName";//上传图片连续2次图片名不能相同，否则无法上传
    int picturenumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webview = (WebView) findViewById(R.id.webview);
        assert webview != null;
        WebSettings settings = webview.getSettings();
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptEnabled(true);

        //读写权限
        checkPermission();

        webview.setWebViewClient(new WebViewClient(){

            //无网处理
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                view.loadUrl("file:///android_asset/error.html");
            }

            public boolean shouldOverviewUrlLoading(WebView   view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webview.setWebChromeClient(new WebChromeClient() {

            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> valueCallback) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            // For Android  >= 3.0
            public void openFileChooser(ValueCallback valueCallback, String acceptType) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            //For Android  >= 4.1
            public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            // For Android >= 5.0
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                uploadMessageAboveL = filePathCallback;
                openImageChooserActivity();
                return true;
            }
        });
        //默认的主页
        targetUrl = "http://"+webaddress+"/cosmetologist/customerlist";

        //通知页的跳转

        String nolink = getIntent().getStringExtra("nolink");

        if (null != nolink) {
            targetUrl = "http://"+webaddress+nolink;
            //Toast.makeText(getApplicationContext(), targetUrl, Toast.LENGTH_LONG).show();
        }

        webview.loadUrl(targetUrl);
        webview.addJavascriptInterface(MainActivity.this,"android");

    }

    private void checkPermission() {
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        };
        //检查权限（NEED_PERMISSION）是否被授权 PackageManager.PERMISSION_GRANTED表示同意授权
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //用户已经拒绝过一次，再次弹出权限申请对话框需要给用户一个解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "请开通相关权限，否则无法正常使用本应用！", Toast.LENGTH_SHORT).show();
            }
            //申请权限
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);

        } else {
            Toast.makeText(this, "授权成功！", Toast.LENGTH_SHORT).show();

        }
    }

    //图片上传
    private void openImageChooserActivity() {
//        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
//        i.addCategory(Intent.CATEGORY_OPENABLE);
//        i.setType("image/*");
//        startActivityForResult(Intent.createChooser(i, "图片选择"), FILE_CHOOSER_RESULT_CODE);

        //1.文件夹和相册
        Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickIntent.setType("image/*");
        //2.拍照
        String path = Environment.getExternalStorageDirectory().getAbsolutePath(); //获取路径
        String fileName = picturefileName+picturenumber+".jpg";//定义文件名
        File file = new File(path,fileName);
        if(!file.getParentFile().exists()){//文件夹不存在
            file.getParentFile().mkdirs();
        }

        Uri imageUri;

        //判断android版本，7.0的相机路径读取有修改
        if (Build.VERSION.SDK_INT >= 24) {
            //  大于等于24即为7.0及以上执行内容
            imageUri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file);
        } else {
            //  低于24即为7.0以下执行内容
            imageUri = Uri.fromFile(file);
        }

        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        //3.选择器
        Intent chooserIntent = Intent.createChooser(pickIntent,
                getString(R.string.activity_main_pick_both));
        //将拍照intent设置为额外初始化intent
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                new Intent[] { takePhotoIntent });


            startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);
            //startActivityForResult(new Intent(MainActivity.this, CaptureActivity.class),0);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessage && null == uploadMessageAboveL) return;
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data);
            } else if (uploadMessage != null) {
                uploadMessage.onReceiveValue(result);
                uploadMessage = null;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        try {//图片压缩
                            actualImage = FileUtil.from(this,  item.getUri());
                            //compressedImage=new Compressor(this).compressToFile(actualImage);
                            compressedImage = new Compressor(this)
                                    .setMaxWidth(1024)
                                    .setMaxHeight(1024)
                                    .setQuality(70)
                                    .compressToFile(actualImage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.d("Compressor", "Compressed image save in " + item.getUri());
                        //results[i] = item.getUri();
                        results[i] = android.net.Uri.parse(compressedImage.toURI().toString());
                    }
                }
                if (dataString != null){
                    try {//图片压缩
                        actualImage = FileUtil.from(this, intent.getData());
                        //compressedImage = new Compressor(this).compressToFile(actualImage);
                        compressedImage = new Compressor(this)
                                .setMaxWidth(1024)
                                .setMaxHeight(1024)
                                .setQuality(70)
                                .compressToFile(actualImage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d("Compresso1r", "Compresse1d image save in " + compressedImage.toURI().toString());
                    results = new Uri[]{android.net.Uri.parse(compressedImage.toURI().toString())};
                    //results = new Uri[]{Uri.parse(dataString)};
                }
            }else{
                String path = Environment.getExternalStorageDirectory().getAbsolutePath(); //获取路径
                String fileName = picturefileName+picturenumber+".jpg";//定义文件名
                File file = new File(path,fileName);
                Uri imageUri = Uri.fromFile(file);
                try {//图片压缩
                    actualImage = file;
                    //compressedImage = new Compressor(this).compressToFile(actualImage);
                    compressedImage = new Compressor(this)
                            .setMaxWidth(1024)
                            .setMaxHeight(1024)
                            .setQuality(70)
                            .compressToFile(actualImage);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //results=new Uri[]{imageUri};
                results=new Uri[]{android.net.Uri.parse(compressedImage.toURI().toString())};
                picturenumber=picturenumber+1;
            }
        }
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }
    //用户登陆后id的储存，mysql的查询
    @android.webkit.JavascriptInterface
    public void cosidsave(final int cosid){
        Log.d("nihao", "cosid"+cosid);
        SharedPreferences sharedPreferences=getSharedPreferences("mycusid",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putInt("cosid",cosid);
        editor.commit();
        //通知线程的开始
        new Thread(newrunnable).start();
    }
    //TODO 最新通知
    Runnable newrunnable = new Runnable() {
        @Override
        public void run() {

            Intent intent2=new Intent(MainActivity.this,MessagenoteService.class);
            startService(intent2);
        }

    };
}
