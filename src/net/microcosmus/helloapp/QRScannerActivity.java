package net.microcosmus.helloapp;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.FrameLayout;
import com.example.AndroidTest.R;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import net.microcosmus.helloapp.domain.Campaign;
import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.SymbolSet;


public class QRScannerActivity extends Activity {


    private Tracker mGaTracker;


    private Camera camera;
    private Handler autoFocusHandler;

    ImageScanner scanner;

    private boolean previewing = true;

    private Campaign campaign;

    static {
        System.loadLibrary("iconv");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        GoogleAnalytics mGaInstance = GoogleAnalytics.getInstance(this);
        mGaTracker = mGaInstance.getTracker("UA-40626076-1");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            campaign = (Campaign) extras.getSerializable("campaign");
        }

        setContentView(R.layout.scanner);
        startScanner();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGaTracker.sendView("QRScannerActivity: id: " + campaign.getId() + ", place: " + campaign.getPlace());
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

                    SymbolSet syms = scanner.getResults();
                    if (!syms.isEmpty()) {
                        String text = syms.iterator().next().getData();
                        Log.d("QRScanner", "text: " + text);

                        if (checkCode(text)) {
                            backToDiscount(text);
                        }
                    }

                }
            }
        };

        CameraPreview preview = new CameraPreview(this, camera, previewCb, new AutoFocusCallback() {
            public void onAutoFocus(boolean success, final Camera camera) {
                // Mimic continuous auto-focusing
                final AutoFocusCallback autoFocusCB = this;
                autoFocusHandler.postDelayed(new Runnable() {
                    public void run() {
                        if (previewing) camera.autoFocus(autoFocusCB);
                    }
                }, 1000);
            }
        });

        FrameLayout layout = (FrameLayout) findViewById(R.id.cameraPreview);
        layout.addView(preview);
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

    private void backToDiscount(String text) {
        Log.d("QRScanner", "Back to discount activity...");
        mGaTracker.sendEvent("events", "qr_detected", text, null);

        Intent intent = new Intent(this, QRScannerActivity.class);
        intent.putExtra("text", text);
        setResult(RESULT_OK, intent);

        finish();
    }


    private static boolean checkCode(String data) {
        return true;
    }
}
