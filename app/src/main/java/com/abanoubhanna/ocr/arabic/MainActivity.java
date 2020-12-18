package com.abanoubhanna.ocr.arabic;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.snackbar.Snackbar;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final int IMAGE_GALLERY_REQUEST = 10;
    public static final int CAMERA_REQUEST_CODE = 20;
    Bitmap bmp;
    ImageView ocrImage;
    ImageButton ocrBtn, showImgBtn, showTxtBtn;
    boolean isDone = false;
    TextView resultTextView;
    String[] cameraPermission;
    String currentPhotoPath;
    Uri photoURI;
    private AdView adView;
    private InterstitialAd mInterstitialAd;
    OcrAsyncTask task;
    LinearLayout ll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultTextView = findViewById(R.id.resultTextView);
        resultTextView.setMovementMethod(new ScrollingMovementMethod());
        resultTextView.setTextIsSelectable(true);
        resultTextView.setText(Html.fromHtml("<b>أهلاُ بك</b><br/>أنصحك بما يلي<br/><ul>" +
                "<li>استخدم أيفونة الصورة لجلب صورة من ستوديو الصور في الهاتف</li>" +
                "<li>استخدم أيقونة الكاميرا لتصوير ورقة أو وثيقة مكتوبة باللغة العربية</li>" +
                "</ul>"));

        ocrImage = findViewById(R.id.ocrImage);
        ll = findViewById(R.id.linearLayout);
        showImgBtn = findViewById(R.id.img);
        showTxtBtn = findViewById(R.id.txt);

        ocrBtn = findViewById(R.id.cpyrun);
        ocrBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isDone){
                    justCopy();
                } else {
                    if (isNetworkAvailable()) { adView.setVisibility(View.VISIBLE); }

                    bmp = imageDenoise(bmp);

                    /*
                    TODO:
                    if the font size / letter size is small,
                    then make the size of image larger
                     */
                    if (bmp.getDensity() < 300){
                        if (bmp.getHeight() < 200 || bmp.getWidth() < 200){
                            bmp = upscale(bmp, 10.0f);
                        } else if (bmp.getHeight() < 400 || bmp.getWidth() < 400){
                            bmp = upscale(bmp, 5.0f);
                        } else if (bmp.getHeight() < 600 || bmp.getWidth() < 600){
                            bmp = upscale(bmp, 3.33f);
                        } else if (bmp.getHeight() < 800 || bmp.getWidth() < 800){
                            bmp = upscale(bmp, 2.5f);
                        } else if (bmp.getHeight() < 1000 || bmp.getWidth() < 1000){
                            bmp = upscale(bmp, 2.0f);
                        } else if (bmp.getHeight() < 1200 || bmp.getWidth() < 1200){
                            bmp = upscale(bmp, 1.66f);
                        } else if (bmp.getHeight() < 1400 || bmp.getWidth() < 1400){
                            bmp = upscale(bmp, 1.42f);
                        } else if (bmp.getHeight() < 1600 || bmp.getWidth() < 1600){
                            bmp = upscale(bmp, 1.25f);
                        } else if (bmp.getHeight() < 1800 || bmp.getWidth() < 1800){
                            bmp = upscale(bmp, 1.11f);
                        }
                    }

                    //ocrImage.setImageBitmap(tmp);
                    runThread(bmp, ocrImage);

                    if (isBgBlack(bmp)){
                        bmp = invertColors(bmp);
                        //ocrImage.setImageBitmap(bmp);
                        runThread(bmp, ocrImage);
                    }

                    runOCR();

                    deleteAllPhotos();
                }
            }
        });

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        loadAds();
        onSharedIntent();

        showTxtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ocrImage.setVisibility(View.GONE);
                resultTextView.setVisibility(View.VISIBLE);
                ocrBtn.setVisibility(View.VISIBLE);
                showImgBtn.setVisibility(View.VISIBLE);
                showTxtBtn.setVisibility(View.GONE);
            }
        });

        showImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultTextView.setVisibility(View.GONE);
                ocrImage.setVisibility(View.VISIBLE);
                ocrBtn.setVisibility(View.GONE);
                showTxtBtn.setVisibility(View.VISIBLE);
                showImgBtn.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadAds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAds();
    }

    private void loadAds() {
        //banner
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();

        //interstitial
        MobileAds.initialize(this);
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-4971969455307153/9016275042");

        //if network available, load the ad
        if (isNetworkAvailable()) {
            adView.loadAd(adRequest);
            mInterstitialAd.loadAd(new AdRequest.Builder().build());

        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "OCRit_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onBackPressed() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            super.onBackPressed();
        }
    }

    void copy2Clipboard(CharSequence text){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("copy text", text);
        if (clipboard != null){
            clipboard.setPrimaryClip(clip);
        }
        Notify(getString(R.string.copied));
    }

    private void justCopy() {
        copy2Clipboard(resultTextView.getText().toString());
    }

    private void runOCR() {
        task = new OcrAsyncTask(MainActivity.this);
        task.execute(bmp);

        //OcrManager manager = new OcrManager();
        //manager.initAPI();
        //Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.test);
        //String result = manager.startRecognize(bmp);

        //resultTextView.setText(getString(R.string.identifying));
        resultTextView.setText("");
        resultTextView.setVisibility(View.VISIBLE);

        //apply the copy function instead of OCR
        isDone = true;
        //change fab icon
        ocrBtn.setImageDrawable(
                //ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_copy)
                AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_copy)
        );
    }

    private void Notify(String text) {
        Snackbar.make(findViewById(R.id.cpyrun), text, Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.camera) {
            deleteAllPhotos();
            if (checkCameraPermission()){
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // Ensure that there's a camera activity to handle the intent
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Notify(ex.getMessage());
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(this,
                                "com.abanoubhanna.ocr.arabic.android.fileprovider",
                                photoFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                    }
                }
            } else {
                showDialogMsg();//if yes see the permission requests
            }
            return true;
        }else if(id == R.id.gallery){
            deleteAllPhotos();
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            String pictureDirectoryPath = pictureDirectory.getPath();
            Uri data = Uri.parse(pictureDirectoryPath);
            photoPickerIntent.setDataAndType(data,"image/*");
            startActivityForResult(photoPickerIntent, IMAGE_GALLERY_REQUEST);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDialogMsg() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.permissionHint)
                .setPositiveButton("موافق", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User agree to accept permissions, show them permissions requests
                        requestCameraPermission();
                        //re-call the camera button
                        findViewById(R.id.camera).performClick();
                    }
                })
                .setNegativeButton("لا", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // No -> so user can not use camera to take picture of papers
                        final AlertDialog.Builder notifBuilder = new AlertDialog.Builder(MainActivity.this);
                        notifBuilder.setMessage("لن تستطيع استخدام الكاميرا لتصوير الورق والمستندات لأنك لم تعطي التطبيق الأذون والتصريحات اللازمة")
                                .setPositiveButton("تم", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        //do nothing
                                    }
                                }).show();
                    }
                }).show();
    }

    private void requestCameraPermission() {
        cameraPermission = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean result0 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result0 && result1;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_GALLERY_REQUEST && data != null) {
                Uri imageUri = data.getData();
                CropImage.activity(imageUri).start(this);

            } else if (requestCode == CAMERA_REQUEST_CODE) {
                Uri camUri = Uri.fromFile(new File(currentPhotoPath));
                //Uri camUri = Uri.parse(new File(currentPhotoPath).toString());
                CropImage.activity(camUri).start(this);

                //Bitmap cameraImage = (Bitmap) data.getExtras().get("data");
                //Uri camUri = getImageUri(this, cameraImage);
                //CropImage.activity(camUri).start(this);

            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (result != null) {
                    bmp = uriToBitmap(result.getUri());
                    showOCRView();
                    ocrImage.setImageBitmap(bmp);
                } else {
                    Notify("Error: Result is NULL");
                }
                isDone = false;
                resultTextView.setVisibility(View.GONE);
                ll.setVisibility(View.VISIBLE);
                ocrBtn.setVisibility(View.VISIBLE);

                //change fab icon
                ocrBtn.setImageDrawable(
                        //ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_ocr)
                        AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_ocr)
                );
            }
        }
    }

    private void showOCRView() {
        ocrImage.setVisibility(View.VISIBLE);

        ocrBtn.setVisibility(View.VISIBLE);
        //fab.show();
    }

    private Bitmap uriToBitmap(Uri uri){
        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeStream(inputStream);
    }

//    public Uri getImageUri(Context inContext, Bitmap inImage) {
//        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
//        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
//        return Uri.parse(path);
//    }

    @Override
    protected void onDestroy() {
        deleteAllPhotos();
        if( task != null && task.getStatus() == AsyncTask.Status.RUNNING){
            task.cancel(true);
        }
        super.onDestroy();
    }

    private void deleteAllPhotos(){
        if (photoURI != null) {
            this.getContentResolver().delete(photoURI, null, null);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null){
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        MainActivity.this.finish();
//    }

    //background OCR task
    private static class OcrAsyncTask extends AsyncTask<Bitmap, Integer, String> {
        private WeakReference<MainActivity> activityWeakReference;

        OcrAsyncTask(MainActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //MainActivity activity = activityWeakReference.get();
            //if (activity == null || activity.isFinishing()) {
            //return;
            //}
        }

        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            OcrManager manager = new OcrManager();
            manager.initAPI();
            return manager.startRecognize(bitmaps[0]);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            activity.resultTextView.setText(s);
            activity.resultTextView.setVisibility(View.VISIBLE);
            activity.ocrImage.setVisibility(View.GONE);
            activity.showImgBtn.setVisibility(View.VISIBLE);
        }
    }

    private void onSharedIntent() {
        Intent receivedIntent = getIntent();
        String receivedAction = receivedIntent.getAction();
        String receivedType = receivedIntent.getType();

        if (receivedAction != null && receivedAction.equals(Intent.ACTION_SEND) && receivedType != null && receivedType.startsWith("image/")) {
            Uri receiveUri = (Uri) receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (receiveUri != null) {
                CropImage.activity(receiveUri).start(this);
                //Log.e(TAG,receiveUri.toString());
            }
        }
//        else if (receivedAction != null && receivedAction.equals(Intent.ACTION_MAIN)) {
//            Notify("Nothing shared");
//            //Log.e(TAG, "onSharedIntent: nothing shared" );
//        }
    }

    Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    boolean isBgBlack(Bitmap inbmp){
        int w = (int)(inbmp.getWidth() / 4);
        int h = (int)(inbmp.getHeight() / 4);

        float darkThreshold = w*h*0.45f;
        int darkPixels=0;

        int[] pixels = new int[w*h];
        inbmp.getPixels(pixels,0,w,0,0,w,h);

        for (int color : pixels) {
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            float luminance = (0.299f * r + 0.0f + 0.587f * g + 0.0f + 0.114f * b + 0.0f);
            if (luminance < 150) {
                darkPixels++;
            }
        }

        return darkPixels >= darkThreshold;
    }

    Bitmap invertColors(Bitmap inbmp){
        int w = inbmp.getWidth();
        int h = inbmp.getHeight();
        Bitmap out = Bitmap.createBitmap(w, h, inbmp.getConfig());

        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                int color = inbmp.getPixel(x, y);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                int avg = (r + g + b) / 3;
                int newColor = Color.argb(255, 255 - avg, 255 - avg, 255 - avg);
                out.setPixel(x, y, newColor);
            }
        }
        return out;
    }

    Bitmap imageDenoise(Bitmap inbmp){
        int w = inbmp.getWidth();
        int h = inbmp.getHeight();
        Bitmap out = Bitmap.createBitmap(w, h, inbmp.getConfig());

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int pixel = inbmp.getPixel(x, y);
                if (Color.red(pixel) < 162 && Color.green(pixel) < 162 && Color.blue(pixel) < 162)
                    out.setPixel(x, y, Color.BLACK);
                else if (Color.red(pixel) > 162 && Color.green(pixel) > 162 && Color.blue(pixel) > 162)
                    out.setPixel(x, y, Color.WHITE);
            }
        }
        return out;
    }

    public Bitmap upscale(Bitmap inbmp, float factor) {
        Bitmap newBitmap = null;

        try {
            //width = 500 and height = 500
            //int width = inbmp.getWidth();
            //int height = inbmp.getHeight();
            int newWidth = (int)(inbmp.getWidth() * factor);
            int newHeight = (int)(inbmp.getHeight() * factor);

            // calculate the scale - in this case = 2.4f
            //float scaleWidth = 1.8f;
            //float scaleHeight = 1.8f;

            // create matrix for the manipulation
            Matrix matrix = new Matrix();

            // resize the bitmap 2.4f, 2.4f
            matrix.postScale(factor, factor);

            // recreate the new Bitmap
            newBitmap = Bitmap.createBitmap(inbmp, 0, 0, newWidth, newHeight, matrix, true);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return newBitmap;
    }

    public void runThread(final Bitmap inBitmap, final ImageView theImagePlace){
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap tmp = toGrayscale(inBitmap);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        theImagePlace.setImageBitmap(tmp);
                    }
                });
            }
        }).start();
    }
}