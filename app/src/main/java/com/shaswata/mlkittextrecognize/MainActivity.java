package com.shaswata.mlkittextrecognize;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.shaswata.mlkittextrecognize.Helper.GraphicOverlay;
import com.shaswata.mlkittextrecognize.Helper.TextGraphic;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.Arrays;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    CameraView  cameraView;
    AlertDialog waitingDialog;
    GraphicOverlay graphicOverlay;
    Button btnCapture;

    //Ctrl+O


    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Init dialog

        waitingDialog = new SpotsDialog.Builder().setCancelable(false).setMessage("Please wait").setContext(this).build();

        cameraView = (CameraView)findViewById(R.id.camera_view);
        graphicOverlay = (GraphicOverlay)findViewById(R.id.graphic_overlay);
        btnCapture = (Button)findViewById(R.id.btn_capture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.start();
                cameraView.captureImage();
                graphicOverlay.clear();
            }
        });

        //Event Camera View

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

                //showDialog
                waitingDialog.show();

                //Processing image

                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap,cameraView.getWidth(),cameraView.getHeight(),false);
                cameraView.stop();

                recognizeText(bitmap);

            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });
    }

    private void recognizeText(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionCloudTextRecognizerOptions options = new FirebaseVisionCloudTextRecognizerOptions.Builder().setLanguageHints(Arrays.asList("en")).build();

        FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();

        textRecognizer.processImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                drawTextResult(firebaseVisionText);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("PANJA_ERROR",e.getMessage());

            }
        });
    }

    private void drawTextResult(FirebaseVisionText firebaseVisionText) {

        //Get Text Block

        List<FirebaseVisionText.TextBlock> blocks = firebaseVisionText.getTextBlocks();

        if(blocks.size() == 0)
        {
            Toast.makeText(this,"No Text Found",Toast.LENGTH_SHORT).show();
            return;
        }

        graphicOverlay.clear();

        for(int i=0;i<blocks.size();i++)
        {
            //Get Line

            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for(int j=0;j<lines.size();j++)
            {
                //Get Element

                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();

                for(int k=0;k<elements.size();k++)
                {
                    //Draw element

                    TextGraphic textGraphic = new TextGraphic(graphicOverlay,elements.get(k));
                    graphicOverlay.add(textGraphic);
                }

            }
        }

        //Dismiss dialog

        waitingDialog.dismiss();
    }
}
