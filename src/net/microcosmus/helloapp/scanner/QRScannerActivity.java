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
import android.widget.FrameLayout;
import com.example.AndroidTest.R;
import net.microcosmus.helloapp.Discount;
import net.sourceforge.zbar.*;

/* Import ZBar Class files */

public class QRScannerActivity extends Activity {

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
        setContentView(R.layout.scanner);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        autoFocusHandler = new Handler();
        camera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);


        PreviewCallback previewCb = new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                Camera.Parameters parameters = camera.getParameters();
                Size size = parameters.getPreviewSize();

                Image barcode = new Image(size.width, size.height, "Y800");
                barcode.setData(data);

                int result = scanner.scanImage(barcode);

                if (result != 0) {
                    previewing = false;
                    QRScannerActivity.this.camera.setPreviewCallback(null);
                    QRScannerActivity.this.camera.stopPreview();

                    barcodeScanned = true;

                    SymbolSet syms = scanner.getResults();
                    for (Symbol sym : syms) {


//                    scanText.setText("barcode result " + sym.getData());
                        String text = sym.getData();
                        if (checkCode(text)) {
                            backToDiscount(ctx, null,  text);
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


    private void backToDiscount(Context context, Discount d, String text) {
        Intent intent = new Intent(context, QRScannerActivity.class);
        intent.putExtra("discount", d);
        intent.putExtra("text", text);
        context.startActivity(intent);
    }


    private static boolean checkCode(String data) {
        return true;
    }
}
