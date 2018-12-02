/*
Created by Omid Alemi
Feb 17, 2017
 */

package al.omid.tfdroid;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Button;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.os.Message;
import android.view.Menu;
import android.widget.AdapterView;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final String MODEL_FILE_IMAGE = "file:///android_asset/stylize_transfer.pb";
    private Bitmap imageBitMapIn = null,imageBitMapOut = null;
    private int desiredSize = 512;
    private float[] floatValues;
    private int[] intValues;
    private static final String INPUT_NODE_IMAGE = "input";
    private static final String STYLE_NODE_IMAGE = "style_num";
    private static final String OUTPUT_NODE_IMAGE = "transformer/expand/conv3/conv/Sigmoid";
    private static final int NUM_STYLES = 26;
    private int selectedStyle;
    ImageView imageView_in,imageView_out;
    private ProgressBar spinner_load;
    private Thread mythread;
    private PopupWindow pw;
    ImageButton Close;

    private String[] galleryPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    static {
        System.loadLibrary("tensorflow_inference");
    }

    private static int RESULT_LOAD_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (! EasyPermissions.hasPermissions(this, galleryPermissions))
        {
            EasyPermissions.requestPermissions(this, "Access for storage",
                    101, galleryPermissions);        } else {

        }


        Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
        spinner_load = (ProgressBar)findViewById(R.id.progressBar1);
        spinner_load.setVisibility(View.GONE);

        //Create the spinner

        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        List<String> categories = new ArrayList<String>();

        categories.add("CRAYONS");
        categories.add("WATER WAVES");
        categories.add("STARRY NIGHT");
        categories.add("MODERN CANVAS");
        categories.add("EUGENE GRASSET's ART");
        categories.add("WASSILY KANDINSKY's COMPOSITION");
        categories.add("OSCAR BLUEMNER's OIL PAINT");
        categories.add("FRANCIS PICABIA's UDNIE");
        categories.add("ROBERT DELAUNAY's COMPOSITION");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,R.layout.spinner_item,categories);
        dataAdapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);

        imageView_in = (ImageView) findViewById(R.id.imgView_in);
        imageView_out = (ImageView) findViewById(R.id.imgView_out);
        imageView_out.setVisibility(View.GONE);
        imageBitMapIn = null;
        imageBitMapOut = null;


        spinner.setAdapter(dataAdapter);
        spinner.setGravity(Gravity.CENTER);

        spinner.setOnItemSelectedListener(this);


        ////////


        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                imageView_out.setVisibility(View.GONE);
                spinner_load.setVisibility(View.GONE);

                if((mythread != null) && mythread.isAlive()) {
                    mythread.interrupt();
                    imageBitMapOut = null;
                    imageView_out.setVisibility(View.GONE);
                }


                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View arg1, int position,long id) {
        String item = parent.getItemAtPosition(position).toString();

        if(item == "FRANCIS PICABIA's UDNIE")
            selectedStyle = 23;
        if(item == "WATER WAVES")
            selectedStyle = 24;
        if(item == "STARRY NIGHT")
            selectedStyle = 19;
        if(item == "CRAYONS")
            selectedStyle = 13;
        if(item == "EUGENE GRASSET's ART")
            selectedStyle = 11;
        if(item == "MODERN CANVAS")
            selectedStyle = 4;
        if(item == "WASSILY KANDINSKY's COMPOSITION")
            selectedStyle = 2;
        if(item == "OSCAR BLUEMNER's OIL PAINT")
            selectedStyle = 7;
        if(item=="ROBERT DELAUNAY's COMPOSITION")
            selectedStyle = 0;

        if(imageBitMapIn != null){
            imageView_out.setVisibility(View.GONE);
            spinner_load.setVisibility(View.VISIBLE);
            mythread = new Thread(runnable);
            mythread.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.about_us:
                showPopup_about_me();
                return true;
            case R.id.help:
                showPopup_help();
                return true;
            case R.id.recommendations:
                showPopup_recom();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showPopup_about_me() {
        try {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.popup_about_me,
                    (ViewGroup) findViewById(R.id.popup_about_me));
            pw = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);
            pw.showAtLocation(layout, Gravity.CENTER, 0, 0);

            Close = (ImageButton) layout.findViewById(R.id.close_popup);
            Close.setOnClickListener(cancel_button);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showPopup_help() {
        try {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.popup_help,
                    (ViewGroup) findViewById(R.id.popup_help));
            pw = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);
            pw.showAtLocation(layout, Gravity.CENTER, 0, 0);

            Close = (ImageButton) layout.findViewById(R.id.close_popup);
            Close.setOnClickListener(cancel_button);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showPopup_recom() {
        try {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.popup_recom,
                    (ViewGroup) findViewById(R.id.popup_recom));
            pw = new PopupWindow(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);
            pw.showAtLocation(layout, Gravity.CENTER, 0, 0);

            Close = (ImageButton) layout.findViewById(R.id.close_popup);
            Close.setOnClickListener(cancel_button);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View.OnClickListener cancel_button = new View.OnClickListener() {
        public void onClick(View v) {
            pw.dismiss();
        }
    };

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {

            Uri selectedImage = data.getData();

            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            imageBitMapIn = BitmapFactory.decodeFile(picturePath);

            imageBitMapIn = Bitmap.createScaledBitmap(imageBitMapIn,desiredSize,desiredSize,true);

            imageView_in.setImageBitmap(imageBitMapIn);

            spinner_load.setVisibility(View.VISIBLE);


            mythread = new Thread(runnable);
            mythread.start();

            imageView_out.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(imageView_out.getVisibility() == View.VISIBLE){
                        MediaStore.Images.Media.insertImage(getContentResolver(),imageBitMapOut,"crayons_image","CRAYONS");
                        Toast.makeText(getApplicationContext(),"IMAGE SUCCESSFULLY SAVED INTO YOUR GALLERY, ENJOY !!",Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            spinner_load.setVisibility(View.GONE);
            imageView_out.setImageBitmap(imageBitMapOut);
            imageView_out.setVisibility(View.VISIBLE);
            Toast.makeText(getApplicationContext(),"TAP TO SAVE THE IMAGE IN GALLERY",Toast.LENGTH_SHORT).show();
        }
    };


    Runnable runnable = new Runnable() {
        public void run() {

            float[] styleVals = new float[NUM_STYLES];
            floatValues = new float[desiredSize * desiredSize * 3];
            intValues = new int[desiredSize * desiredSize];
            styleVals[selectedStyle] = 1.0f;

            imageBitMapIn.getPixels(intValues, 0, imageBitMapIn.getWidth(), 0, 0, imageBitMapIn.getWidth(), imageBitMapIn.getHeight());

            for (int i = 0; i < intValues.length; ++i) {
                final int val = intValues[i];
                floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
                floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
                floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
            }

            TensorFlowInferenceInterface inferenceInterface_image = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE_IMAGE);

            inferenceInterface_image.feed(STYLE_NODE_IMAGE, styleVals, NUM_STYLES);
            inferenceInterface_image.feed(
                    INPUT_NODE_IMAGE, floatValues, 1, imageBitMapIn.getWidth(), imageBitMapIn.getHeight(), 3);
            inferenceInterface_image.run(new String[] {OUTPUT_NODE_IMAGE});
            inferenceInterface_image.fetch(OUTPUT_NODE_IMAGE, floatValues);

            for (int i = 0; i < intValues.length; ++i) {
                intValues[i] =
                        0xFF000000
                                | (((int) (floatValues[i * 3] * 255)) << 16)
                                | (((int) (floatValues[i * 3 + 1] * 255)) << 8)
                                | ((int) (floatValues[i * 3 + 2] * 255));
            }

            imageBitMapOut = Bitmap.createBitmap(imageBitMapIn.getWidth(),imageBitMapIn.getHeight(), Bitmap.Config.ARGB_8888);
            imageBitMapOut.setPixels(intValues, 0, imageBitMapIn.getWidth(), 0, 0, imageBitMapIn.getWidth(), imageBitMapIn.getHeight());
            handler.sendEmptyMessage(0);
        }
    };
}