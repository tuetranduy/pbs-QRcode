package com.example.tue19.pbs_qrcode;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Vibrator;
import android.widget.Toast;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private SurfaceView cameraView;
    private TextView barcodeValue;
    private String ipAddress = "http://34.203.199.193:3000";

    Socket mSocket;
    {
        try {
            mSocket = IO.socket(ipAddress);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setIP();
        initSocket();
        addControls();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            readBarcode();
        }
    }

    private String setIP(){
        final Button btnOK;
        final EditText txtIP;
        btnOK = (Button) findViewById(R.id.btnOK);
        txtIP = (EditText) findViewById(R.id.txtIP);
        txtIP.setSelection(7);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ipAddress = txtIP.getText().toString();
                Toast.makeText(MainActivity.this, "Set IP successful, IP now: " + ipAddress, Toast.LENGTH_SHORT).show();
            }
        });
        return ipAddress;
    }

    private void initSocket() {
        mSocket.connect();
    }

    private void setVibrator(){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);
    }

    private void addControls() {
        cameraView = (SurfaceView) findViewById(R.id.camera_view);
        barcodeValue = (TextView) findViewById(R.id.barcodeValue);
    }

    protected void readBarcode(){
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(720, 720)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                final String data = barcodes.valueAt(0).displayValue;

                if (barcodes.size() != 0) {
                    barcodeValue.post(new Runnable() {

                        @Override
                        public void run() {
                            barcodeValue.setText(data);
                            mSocket.emit("SEND_BARCODE", data);
                            setVibrator();
                            cameraSource.stop();
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        cameraSource.start(cameraView.getHolder());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 3000);

                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraSource.release();
        barcodeDetector.release();
    }
}
