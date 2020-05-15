package com.example.colorblindnesssimulatorforandroid;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // filter data
    static
    {
        System.loadLibrary("NativeImageProcessor");
    }
    /**
     * Default screen gamma on Windows is 2.2.
     */
    private static final double GAMMA = 2.2;
    private static final double GAMMA_INV = 1. / GAMMA;

    /**
     * A lookup table for the conversion from gamma-corrected sRGB values
     * [0..255] to linear RGB values [0..32767].
     */
    private static final short[] SRGB_TO_LINRGB;

    static {
        // initialize SRGB_TO_LINRGB
        SRGB_TO_LINRGB = new short[256];
        for (int i = 0; i < 256; i++) {
            // compute linear rgb between 0 and 1
            final double lin = (0.992052 * Math.pow(i / 255., GAMMA) + 0.003974);

            // scale linear rgb to 0..32767
            SRGB_TO_LINRGB[i] = (short) (lin * 32767.);
        }
    }

    /**
     * A lookup table for the conversion of linear RGB values [0..255] to
     * gamma-corrected sRGB values [0..255].
     */
    private static final byte[] LINRGB_TO_SRGB;

    static {
        // initialize LINRGB_TO_SRGB
        LINRGB_TO_SRGB = new byte[256];
        for (int i = 0; i < 256; i++) {
            LINRGB_TO_SRGB[i] = (byte) (255. * Math.pow(i / 255., GAMMA_INV));
        }
    }

    ImageView imageView;
    Context context;
    Integer simulatorType;
    Bitmap normalBitmap;
    Toast toast;

    private static final int REQUEST_PERMISSIONS = 34;

    private static final int PERMISSIONS_COUNT = 1;


    private static final String[] PERMISSIONS= {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @SuppressLint("NewApi")
    private boolean arePermissionsDenied(){
        for(int i = 0; i <PERMISSIONS_COUNT; i++){
            if(checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_PERMISSIONS && grantResults.length > 0){
            if(arePermissionsDenied()){
                ((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else{
                onResume();
            }
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        simulatorType = 0;
        Button btnCamera = (Button)findViewById(R.id.btnCamera);
        imageView = (ImageView)findViewById(R.id.imageView);

        btnCamera.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 0);
            }
        });

        Button btnSave = (Button)findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M && arePermissionsDenied()){
                    requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
                }

                imageView.invalidate();
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                if( drawable == null){
                    toast = Toast.makeText(context, "Please take a photo first!", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }else{
                    Bitmap bitmap = drawable.getBitmap();
                    Long tsLong = System.currentTimeMillis()/1000;
                    String ts = tsLong.toString();
                    saveImage(bitmap, ts);
                    toast = Toast.makeText(context, "Saved", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        Button btnNormal = (Button)findViewById(R.id.normal);
        btnNormal.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if (normalBitmap == null){
                    toast = Toast.makeText(context, "Please take a photo first!", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                simulatorType = 0;
                imageView.setImageBitmap(normalBitmap);
            }
        });
        Button btnDeutan = (Button)findViewById(R.id.deutan);
        btnDeutan.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if (normalBitmap == null){
                    toast = Toast.makeText(context, "Please take a photo first!", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                simulatorType = 1;
                Bitmap bitmap = applyFilter(normalBitmap);
                imageView.setImageBitmap(bitmap);
            }
        });
        Button btnProtan = (Button)findViewById(R.id.protan);
        btnProtan.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if (normalBitmap == null){
                    toast = Toast.makeText(context, "Please take a photo first!", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                simulatorType = 2;
                Bitmap bitmap = applyFilter(normalBitmap);
                imageView.setImageBitmap(bitmap);
            }
        });
        Button btnTritan = (Button)findViewById(R.id.tritan);
        btnTritan.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if (normalBitmap == null){
                    toast = Toast.makeText(context, "Please take a photo first!", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                simulatorType = 3;
                Bitmap bitmap = applyFilter(normalBitmap);
                imageView.setImageBitmap(bitmap);
            }
        });
        Button btnGrey = (Button)findViewById(R.id.grayscale);
        btnGrey.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if (normalBitmap == null){
                    toast = Toast.makeText(context, "Please take a photo first!", Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                simulatorType = 4;
                Bitmap bitmap = applyFilter(normalBitmap);
                imageView.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    // This function is a callback function of taking photo.
    // This function attempt to set the imageView.
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        normalBitmap = (Bitmap)data.getExtras().get("data");
        imageView.setImageBitmap(normalBitmap);
    }

    protected Bitmap applyFilter(Bitmap bitmap){
        switch (simulatorType){
            case 0:
                return bitmap;
            case 1:
                return applyDeutanFilter(bitmap);
            case 2:
                return applyProtanFilter(bitmap);
            case 3:
                return applyTritanFilter(bitmap);
            case 4:
                return applyGreyFilter(bitmap);
        }
        return bitmap;
    }

    // This function attempt to apply deutan filter.
    protected Bitmap applyDeutanFilter(Bitmap bitmap){

        Bitmap newBitmap = bitmap.copy( Bitmap.Config.ARGB_8888, true); //Bitmap.createBitmap(bitmap);

        int prevIn = 0;
        int prevOut = 0;

        //(9591, 23173, -730);
        int k1 = 9591;
        int k2 = 23173;
        int k3 = -730;

        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {
                //Color pixel = bitmap.getColor(i, j);
                final int rgb = bitmap.getPixel(i, j);
                final int r = (0xff0000 & rgb) >> 16;
                final int g = (0xff00 & rgb) >> 8;
                final int b = 0xff & rgb;
                // get linear rgb values in the range 0..2^15-1
                final int r_lin = SRGB_TO_LINRGB[r];
                final int g_lin = SRGB_TO_LINRGB[g];
                final int b_lin = SRGB_TO_LINRGB[b];

                int r_blind = (int) (k1 * r_lin + k2 * g_lin) >> 22;
                int b_blind = (int) (k3 * r_lin - k3 * g_lin + 32768 * b_lin) >> 22;

                if (r_blind < 0) {
                    r_blind = 0;
                } else if (r_blind > 255) {
                    r_blind = 255;
                }

                if (b_blind < 0) {
                    b_blind = 0;
                } else if (b_blind > 255) {
                    b_blind = 255;
                }

                // convert reduced linear rgb to gamma corrected rgb
                int red = LINRGB_TO_SRGB[r_blind];
                red = red >= 0 ? red : 256 + red; // from unsigned to signed
                int blue = LINRGB_TO_SRGB[b_blind];
                blue = blue >= 0 ? blue : 256 + blue; // from unsigned to signed

                final int out = 0xff000000 | red << 16 | red << 8 | blue;

                newBitmap.setPixel(i, j, out);
                prevIn = rgb;
                prevOut = out;
            }
        }
        return newBitmap;
    }

    // This function attempt to apply protan filter.
    protected Bitmap applyProtanFilter(Bitmap bitmap){

        Bitmap newBitmap = bitmap.copy( Bitmap.Config.ARGB_8888, true); //Bitmap.createBitmap(bitmap);

        int prevIn = 0;
        int prevOut = 0;

        int k1 = 3683;
        int k2 = 29084;
        int k3 = 131;

        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {
                //Color pixel = bitmap.getColor(i, j);
                final int rgb = bitmap.getPixel(i, j);
                final int r = (0xff0000 & rgb) >> 16;
                final int g = (0xff00 & rgb) >> 8;
                final int b = 0xff & rgb;
                // get linear rgb values in the range 0..2^15-1
                final int r_lin = SRGB_TO_LINRGB[r];
                final int g_lin = SRGB_TO_LINRGB[g];
                final int b_lin = SRGB_TO_LINRGB[b];

                int r_blind = (int) (k1 * r_lin + k2 * g_lin) >> 22;
                int b_blind = (int) (k3 * r_lin - k3 * g_lin + 32768 * b_lin) >> 22;

                if (r_blind < 0) {
                    r_blind = 0;
                } else if (r_blind > 255) {
                    r_blind = 255;
                }

                if (b_blind < 0) {
                    b_blind = 0;
                } else if (b_blind > 255) {
                    b_blind = 255;
                }

                // convert reduced linear rgb to gamma corrected rgb
                int red = LINRGB_TO_SRGB[r_blind];
                red = red >= 0 ? red : 256 + red; // from unsigned to signed
                int blue = LINRGB_TO_SRGB[b_blind];
                blue = blue >= 0 ? blue : 256 + blue; // from unsigned to signed

                final int out = 0xff000000 | red << 16 | red << 8 | blue;

                newBitmap.setPixel(i, j, out);
                prevIn = rgb;
                prevOut = out;
            }
        }
        return newBitmap;
    }

    // This function attempt to apply tritan filter.
    protected Bitmap applyTritanFilter(Bitmap bitmap){
        /* Code for tritan simulation from GIMP 2.2
         *  This could be optimised for speed.
         *  Performs tritan color image simulation based on
         *  Brettel, Vienot and Mollon JOSA 14/10 1997
         *  L,M,S for lambda=475,485,575,660
         *
         * Load the LMS anchor-point values for lambda = 475 & 485 nm (for
         * protans & deutans) and the LMS values for lambda = 575 & 660 nm
         * (for tritans)
         */
        final float anchor_e0 = 0.05059983f + 0.08585369f + 0.00952420f;
        final float anchor_e1 = 0.01893033f + 0.08925308f + 0.01370054f;
        final float anchor_e2 = 0.00292202f + 0.00975732f + 0.07145979f;
        final float inflection = anchor_e1 / anchor_e0;

        /* Set 1: regions where lambda_a=575, set 2: lambda_a=475 */
        final float a1 = -anchor_e2 * 0.007009f;
        final float b1 = anchor_e2 * 0.0914f;
        final float c1 = anchor_e0 * 0.007009f - anchor_e1 * 0.0914f;
        final float a2 = anchor_e1 * 0.3636f - anchor_e2 * 0.2237f;
        final float b2 = anchor_e2 * 0.1284f - anchor_e0 * 0.3636f;
        final float c2 = anchor_e0 * 0.2237f - anchor_e1 * 0.1284f;

        // make sure the two images have the same size, color space, etc.

        Bitmap newBitmap = bitmap.copy( Bitmap.Config.ARGB_8888, true); //Bitmap.createBitmap(bitmap);

        int prevIn = 0;
        int prevOut = 0;

        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {

                final int rgb = bitmap.getPixel(i, j);

                int r = (0xff0000 & rgb) >> 16;
                int g = (0xff00 & rgb) >> 8;
                int b = 0xff & rgb;

                // get linear rgb values in the range 0..2^15-1
                r = SRGB_TO_LINRGB[r];
                g = SRGB_TO_LINRGB[g];
                b = SRGB_TO_LINRGB[b];

                /* Convert to LMS (dot product with transform matrix) */
                final float L = (r * 0.05059983f + g * 0.08585369f + b * 0.00952420f) / 32767.f;
                final float M = (r * 0.01893033f + g * 0.08925308f + b * 0.01370054f) / 32767.f;
                float S; // = (r * 0.00292202f + g * 0.00975732f + b * 0.07145979f) / 32767.f;

                final float tmp = M / L;

                /* See which side of the inflection line we fall... */
                if (tmp < inflection) {
                    S = -(a1 * L + b1 * M) / c1;
                } else {
                    S = -(a2 * L + b2 * M) / c2;
                }

                /* Convert back to RGB (cross product with transform matrix) */
                int ired = (int) (255.f * (L * 30.830854f
                        - M * 29.832659f + S * 1.610474f));
                int igreen = (int) (255.f * (-L * 6.481468f
                        + M * 17.715578f - S * 2.532642f));
                int iblue = (int) (255.f * (-L * 0.375690f
                        - M * 1.199062f + S * 14.273846f));

                // convert reduced linear rgb to gamma corrected rgb
                if (ired < 0) {
                    ired = 0;
                } else if (ired > 255) {
                    ired = 255;
                } else {
                    ired = LINRGB_TO_SRGB[ired];
                    ired = ired >= 0 ? ired : 256 + ired; // from unsigned to signed
                }
                if (igreen < 0) {
                    igreen = 0;
                } else if (igreen > 255) {
                    igreen = 255;
                } else {
                    igreen = LINRGB_TO_SRGB[igreen];
                    igreen = igreen >= 0 ? igreen : 256 + igreen; // from unsigned to signed
                }
                if (iblue < 0) {
                    iblue = 0;
                } else if (iblue > 255) {
                    iblue = 255;
                } else {
                    iblue = LINRGB_TO_SRGB[iblue];
                    iblue = iblue >= 0 ? iblue : 256 + iblue; // from unsigned to signed
                }

                final int out = (int) (ired << 16 | igreen << 8 | iblue | 0xff000000);

                newBitmap.setPixel(i, j, out);
                prevIn = rgb;
                prevOut = out;
            }
        }
        return newBitmap;
    }

    // This function attempt to apply greyscale filter.
    protected Bitmap applyGreyFilter(Bitmap bitmap){
        Bitmap newBitmap = bitmap.copy( Bitmap.Config.ARGB_8888, true); //Bitmap.createBitmap(bitmap);
        int prevIn = 0;
        int prevOut = 0;

        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {

                final int rgb = bitmap.getPixel(i, j);

                final int r = (0xff0000 & rgb) >> 16;
                final int g = (0xff00 & rgb) >> 8;
                final int b = 0xff & rgb;

                // get linear rgb values in the range 0..2^15-1
                final int r_lin = SRGB_TO_LINRGB[r];
                final int g_lin = SRGB_TO_LINRGB[g];
                final int b_lin = SRGB_TO_LINRGB[b];

                // perceptual luminance-preserving conversion to grayscale
                // https://en.wikipedia.org/wiki/Grayscale#Colorimetric_(perceptual_luminance-preserving)_conversion_to_grayscale
                double luminance = 0.2126 * r_lin + 0.7152 * g_lin + 0.0722 * b_lin;
                int linRGB = ((int) (luminance)) >> 8; // divide by 2^8 to rescale

                // convert linear rgb to gamma corrected sRGB
                if (linRGB < 0) {
                    linRGB = 0;
                } else if (linRGB > 255) {
                    linRGB = 255;
                } else {
                    linRGB = LINRGB_TO_SRGB[linRGB];
                    linRGB = linRGB >= 0 ? linRGB : 256 + linRGB; // from unsigned to signed
                }

                final int out = (int) (linRGB << 16 | linRGB << 8 | linRGB | 0xff000000);

                newBitmap.setPixel(i, j, out);
                prevIn = rgb;
                prevOut = out;
            }
        }
        return newBitmap;

    }


    // This function attempts to save bitmap in a jpg format to gallery.
    protected void saveImage(Bitmap finalBitmap, String image_name) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "Image-" + image_name+ ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();

        Log.i("LOAD", root + fname);

        try {
            FileOutputStream out = new FileOutputStream(file);
            Log.i("LOAD", "output stream");
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("LOAD", e.getMessage());
        }
    }
}


/* public class MainActivity extends AppCompatActivity implements Camera.PreviewCallback {
    final String TAG = "mytag";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // iris
    @Override
    public void onPreviewFrame(byte[] data, Camera camera){
        Toast toast1 = Toast
                .makeText(getBaseContext(), "success", Toast.LENGTH_LONG);
        toast1.show();
        try {
            Camera.Parameters parameters = camera.getParameters();

            Camera.Size size = parameters.getPreviewSize();
            YuvImage image = new YuvImage(data, parameters.getPreviewFormat(),
                    size.width, size.height, null);
            File file = new File(Environment.getExternalStorageDirectory(), "out.jpg");
            FileOutputStream filecon = new FileOutputStream(file);
            image.compressToJpeg(
                    new Rect(0, 0, image.getWidth(), image.getHeight()), 90,
                    filecon);
            Toast toast = Toast
                    .makeText(getBaseContext(), "success", Toast.LENGTH_LONG);
            toast.show();
        } catch (FileNotFoundException e) {
            Toast toast = Toast
                    .makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_LONG);
            toast.show();
        }

    }


    private static final String[] PERMISSIONS= {
            Manifest.permission.CAMERA
    };
    private static final int REQUEST_PERMISSIONS = 34;

    private static final int PERMISSIONS_COUNT = 1;


    @SuppressLint("NewApi")
    private boolean arePermissionsDenied(){
        for(int i = 0; i <PERMISSIONS_COUNT; i++){
            if(checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_PERMISSIONS && grantResults.length > 0){
            if(arePermissionsDenied()){
                ((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
                recreate();
            }else{
                onResume();
            }
        }

    }

    private boolean isCameraInitialized;

    private Camera mCamera = null;

    private static SurfaceHolder myHolder;

    private static CameraPreview mPreview;

    private FrameLayout preview;

    private static OrientationEventListener orientationEventListener = null;

    @Override
    protected void onResume(){
        super.onResume();
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M && arePermissionsDenied()){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
        if(!isCameraInitialized){
            mCamera = Camera.open();
            mPreview = new CameraPreview(this, mCamera);
            preview = findViewById(R.id.camera_preview);

            preview.addView(mPreview);
            final Button switchCameraButton = findViewById(R.id.switchCamera);
            switchCameraButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCamera.release();
                    switchCamera();
                    rotateCamera();
                    try{
                        mCamera.setPreviewDisplay(myHolder);
                    }catch (Exception e){

                    }
                    mCamera.startPreview();
                }
            });
            orientationEventListener = new OrientationEventListener(this) {
                @Override
                public void onOrientationChanged(int orientation) {
                    rotateCamera();
                }
            };
            orientationEventListener.enable();


        }
    }
    private void switchCamera(){
        if(whichCamera){
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }else{
            mCamera = Camera.open();
        }
        whichCamera = !whichCamera;
    }

    @Override
    protected void onPause(){
        super.onPause();
        releaseCamera();

    }

    private void releaseCamera(){
        if(mCamera != null){
            preview.removeView(mPreview);
            mCamera.release();
            orientationEventListener.disable();
            mCamera = null;
            whichCamera = !whichCamera;
        }
    }

    private static int rotation;

    private static boolean whichCamera = true;

    private static Camera.Parameters p;

    private void rotateCamera(){
        if(mCamera != null){
            rotation = this.getWindowManager().getDefaultDisplay().getRotation();
            if(rotation == 0){
                rotation = 90;
            }else if (rotation == 1){
                rotation = 0;
            }else if (rotation == 2){
                rotation = 270;
            }else {
                rotation = 180;
            }
            mCamera.setDisplayOrientation(rotation);
            //back/front camera
            if(!whichCamera){
                if(rotation == 90){
                    rotation = 270;
                }else if (rotation == 270){
                    rotation = 90;
                }
            }
            p = mCamera.getParameters();
            p.setRotation(rotation);
            mCamera.setParameters(p);
        };
    }

    // MARK: -- grey scale filter
    public void onSepiaClick(View view){
        p = mCamera.getParameters();
        p.setColorEffect("mono");
        mCamera.setParameters(p);
        mCamera.startPreview();
    }

    public void onNoneClick(View view){
        p = mCamera.getParameters();
        p.setColorEffect("none");
        mCamera.setParameters(p);
        mCamera.startPreview();
    }
    
    private static class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{
        private static SurfaceHolder mHolder;
        private static Camera mCamera;

        private CameraPreview(Context context, Camera camera){
            super(context);
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder){
            myHolder = holder;
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void surfaceDestroyed(SurfaceHolder holder){

        }
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h){

        }
    }
    public void onCaptureClick(View view){
        mCamera.takePicture(null,null,mPicture);
    }


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile();
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            MediaScannerConnection.scanFile(MainActivity.this,
                    new String[] { pictureFile.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };


    private static File getOutputMediaFile(){

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CamPictures");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("tag", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Log.d("path", mediaStorageDir.getAbsolutePath());
        return new File(mediaStorageDir.getAbsolutePath() + File.separator +
                "IMG_" + ".jpg");
    }

}*/
