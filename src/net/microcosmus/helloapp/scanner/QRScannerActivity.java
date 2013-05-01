package net.microcosmus.helloapp.scanner;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.example.AndroidTest.R;
import net.sourceforge.zbar.*;

/* Import ZBar Class files */

public class QRScannerActivity extends Activity {

    public static final boolean DEBUG = true;

    private Camera camera;
    private Handler autoFocusHandler;

//    TextView scanText;
//    Button scanButton;

    ImageScanner scanner;

    private boolean barcodeScanned = false;
    private boolean previewing = true;

    private Context ctx;

    static {
        System.loadLibrary("iconv");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ctx = this;

        if (!DEBUG) {
            setContentView(R.layout.scanner);
            startScanner();

        } else {

            setContentView(R.layout.fake_scanner);
            startFakeScanner();
        }

    }

    private void startFakeScanner() {
        Log.d("QRScanner", "Starting fake scanner");


        final TextView scanText = (TextView) findViewById(R.id.scanText);
        scanText.setText("");

        Button scanButton = (Button) findViewById(R.id.ScanButton);

        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!barcodeScanned) {

                    scanText.setText("Scanning...");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    scanText.setText("Done.");

                    String text = "fake number";

                    Log.d("QRScanner", "Detected: " + text);

                    barcodeScanned = true;
                    backToDiscount(ctx, text);
                }


            }
        });
    }


    private void startScanner() {
        Log.d("QRScanner", "Starting scanner...");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        autoFocusHandler = new Handler();
        camera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);


        PreviewCallback previewCb = new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                Log.d("QRScanner", "onPreview");
                Camera.Parameters parameters = camera.getParameters();
                Size size = parameters.getPreviewSize();

                Image barcode = new Image(size.width, size.height, "Y800");
                barcode.setData(data);

                int result = scanner.scanImage(barcode);

                Log.d("QRScanner", "got result: " + result);


                if (result != 0) {
                    previewing = false;
                    QRScannerActivity.this.camera.setPreviewCallback(null);
                    QRScannerActivity.this.camera.stopPreview();

                    barcodeScanned = true;

                    SymbolSet syms = scanner.getResults();
                    for (Symbol sym : syms) {


//                    scanText.setText("barcode result " + sym.getData());
                        String text = sym.getData();

                        Log.d("QRScanner", "text: " + text);

                        if (checkCode(text)) {
                            backToDiscount(ctx, text);
                        }
                    }

                }
            }
        };

        CameraPreview preview = new CameraPreview(this, camera, previewCb, autoFocusCB);
        FrameLayout layout = (FrameLayout) findViewById(R.id.cameraPreview);
        layout.addView(preview);

//        scanText = (TextView) findViewById(R.id.scanText);
//
//        scanButton = (Button) findViewById(R.id.ScanButton);
//
//        scanButton.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                if (barcodeScanned) {
//                    barcodeScanned = false;
//                    scanText.setText("Scanning...");
//                    camera.setPreviewCallback(previewCb);
//                    camera.startPreview();
//                    previewing = true;
//                    camera.autoFocus(autoFocusCB);
//                }
//            }
//        });

    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    private void releaseCamera() {
        if (camera != null) {
            previewing = false;
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                camera.autoFocus(autoFocusCB);
        }
    };


    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };


    private void backToDiscount(Context context, String text) {
        Log.d("QRScanner", "Back to discount activity...");

        Intent intent = new Intent(context, QRScannerActivity.class);
        intent.putExtra("text", text);
        setResult(RESULT_OK, intent);
        finish();
    }


    private static boolean checkCode(String data) {
        return true;
    }
}
