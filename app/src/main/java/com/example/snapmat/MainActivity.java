package com.example.snapmat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int OPEN_GALLERY = 123;
    private static final int OPEN_CAMERA = 124;


    private Image image;
    private Bitmap bitmap;
    private Bitmap originalImage;

    private ImageView texture;

    private SeekBar lowBar;
    private SeekBar highBar;

    private TextView valueText;

    private int lowVal;
    private int highVal;

    private int[][] baseGrayscale;

    private String imageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        texture =(ImageView) findViewById(R.id.textureView);

        BitmapDrawable bitmapDrawable = ((BitmapDrawable) texture.getDrawable());
        bitmap = bitmapDrawable.getBitmap();

        bitmap = toGrayScale(bitmap);
        texture.setImageBitmap(bitmap);


        lowBar = (SeekBar) findViewById(R.id.lowBar);
        highBar = (SeekBar) findViewById(R.id.highBar);
        lowVal = lowBar.getProgress();
        highVal = highBar.getProgress();


        lowBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(highBar.getProgress() < progress) {
                    lowBar.setProgress(highBar.getProgress() - 1);
                    progress =  lowBar.getProgress();
                }
                else if(highVal == progress)
                    progress--;

                lowVal = progress;

                changeLevels();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        highBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(lowBar.getProgress() > progress) {
                    highBar.setProgress(lowBar.getProgress() + 1);
                    progress =  highBar.getProgress();
                }
                else if(lowVal == progress)
                    progress++;

                highVal = progress;

                changeLevels();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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
        if (id == R.id.open_gallery) {
            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(gallery, OPEN_GALLERY);
            return true;
        }

        if (id == R.id.open_camera) {
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(camera, OPEN_CAMERA);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == OPEN_GALLERY && resultCode == RESULT_OK){
            Uri galleryImage = intent.getData();
            texture.setImageURI(galleryImage);
            BitmapDrawable bitmapDrawable = ((BitmapDrawable) texture.getDrawable());
            bitmap = bitmapDrawable.getBitmap();
            bitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
            originalImage = bitmap;
            bitmap = toGrayScale(bitmap);
            texture.setImageBitmap(bitmap);

            //https://freakycoder.com/android-notes-74-how-to-extract-filename-from-uri-ea1caf248a87
            String filePath = galleryImage.getPath();
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            imageName = fileName;
        }
        if(requestCode == OPEN_CAMERA && resultCode == RESULT_OK){
            Bitmap capturedPhoto = (Bitmap) intent.getExtras().get("data");
            texture.setImageBitmap(capturedPhoto);
            BitmapDrawable bitmapDrawable = ((BitmapDrawable) texture.getDrawable());
            bitmap = bitmapDrawable.getBitmap();
            bitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
            originalImage = bitmap;
            bitmap = toGrayScale(bitmap);
            texture.setImageBitmap(bitmap);
            imageName = "captureImage" + (int) (Math.random() * 9999999 + 1);
        }


    }


    private Bitmap toGrayScale(Bitmap image){

        Bitmap mutableBitmap = image.copy(Bitmap.Config.ARGB_8888, true);
        baseGrayscale = new int[mutableBitmap.getWidth()][mutableBitmap.getHeight()];

        for(int x = 0; x < mutableBitmap.getWidth(); x++){
            for(int y = 0; y < mutableBitmap.getHeight(); y++){

                int pixelCol = mutableBitmap.getPixel(x, y);

                int red = Color.red(pixelCol);
                int green = Color.green(pixelCol);
                int blue = Color.blue(pixelCol);
                int alpha = Color.alpha(pixelCol);

                //Luminosity method
                int grayScale = (int) (red * 0.3 + green * 0.6 + blue * 0.1);

                baseGrayscale[x][y] = grayScale;

                int color = Color.argb(alpha, grayScale, grayScale, grayScale);
                mutableBitmap.setPixel(x, y, color);
            }
        }
        return mutableBitmap;

    }

    private void changeLevels(){
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        for(int x = 0; x < mutableBitmap.getWidth(); x++){
            for(int y = 0; y < mutableBitmap.getHeight(); y++){

                int pixelCol = mutableBitmap.getPixel(x, y);
                int alpha = Color.alpha(pixelCol);
                int newGrayscale = Math.min(255, Math.max(0, (baseGrayscale[x][y] - lowVal) * 255 / (highVal - lowVal)));
                if(newGrayscale < 0)
                    newGrayscale = 0;
                else if(newGrayscale > 255)
                    newGrayscale = 255;


                int color = Color.argb(alpha, newGrayscale, newGrayscale, newGrayscale);
                mutableBitmap.setPixel(x, y, color);
            }
        }
        bitmap = mutableBitmap;
        texture.setImageBitmap(bitmap);
    }

    public void save(View view){
        saveImage(originalImage, "Diffuse");
        saveImage(bitmap, "Height");
        saveImage(toNormalMap(bitmap), "Normal");
        Toast.makeText(getApplicationContext(), "Complete - Images saved to photos", Toast.LENGTH_SHORT).show();
    }

    private Bitmap toNormalMap(Bitmap heightMap){
        Bitmap normalMap = heightMap.copy(Bitmap.Config.ARGB_8888, true);
        int width = normalMap.getWidth();
        int height = normalMap.getHeight();


        for(int x = 0; x < normalMap.getWidth(); x++){
            for(int y = 0; y < normalMap.getHeight(); y++){
                int leftPixel = x - 1;
                int rightPixel = x + 1;
                int upPixel = y - 1;
                int downPixel = y + 1;

                //Wrap around
                if(leftPixel < 0)
                    leftPixel = width + leftPixel;
                else if(rightPixel >= width)
                    rightPixel = (rightPixel - width);

                if(upPixel < 0)
                    upPixel = height + upPixel;
                else if(downPixel >= width)
                    downPixel = (downPixel - height);

                int leftPixelCol = Color.red(normalMap.getPixel(leftPixel, y));
                int rightPixelCol = Color.red(normalMap.getPixel(rightPixel, y));
                int upPixelCol = Color.red(normalMap.getPixel(x, upPixel));
                int downPixelCol = Color.red(normalMap.getPixel(x, downPixel));

                int xDiff = leftPixelCol - rightPixelCol;
                int yDiff = downPixelCol - upPixelCol;
                int z = 255;

                float normalized = (float) (255 / Math.sqrt(xDiff * xDiff + yDiff * yDiff + z * z));

                int xPix = (int) Math.abs(xDiff * normalized * 1.2);
                int yPix = (int) Math.abs(yDiff * normalized * 1.2);
                int zPix = (int) Math.abs(z * normalized);

                int color = Color.argb(255, xPix, yPix, zPix);
                normalMap.setPixel(x, y, color);
            }
        }
        return normalMap;
    }



    private void saveImage(Bitmap image, String type){

        String imageURI = MediaStore.Images.Media.insertImage(getContentResolver(), image, type + "-" + imageName + ".jpg", type);

    }

}
