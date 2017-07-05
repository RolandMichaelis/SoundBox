/**
 * Created by Roland Michaelis 05. April 2017.
 * V 1.0
 */
package de.spas.soundbox;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener, SensorEventListener, OnSeekBarChangeListener {

    private SensorManager mgr;
    private Sensor light;
    private TextView text;
    private TextView song;
    private TextView light_text;
    private StringBuilder msg = new StringBuilder(2048);
    private MediaPlayer mpMusic;
    private int changer = 0;
    private int rndcount;
    private boolean hider=false;
    SeekBar seekBar1;
    private int seekbar_value;
    private int counter;
    private Uri uri;
    private Cursor cursor;
    private boolean isSDPresent;
    private int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE=123456789;
    PowerManager pm;
    PowerManager.WakeLock wl;
    private long idMem;
    private String output;

    final String TAG = "MainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        seekBar1=(SeekBar)findViewById(R.id.seekBar1);
        seekBar1.setOnSeekBarChangeListener(this);

        mgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        light = mgr.getDefaultSensor(Sensor.TYPE_LIGHT);

        light_text = (TextView) findViewById(R.id.light);
        text = (TextView) findViewById(R.id.text);
        song = (TextView) findViewById(R.id.song);
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        seekbar_value = sp.getInt("sensitivity", 10);
        text.setText("Sensitivity: "+String.valueOf(seekbar_value));
        seekBar1.setProgress(seekbar_value);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl =  pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My WakeLock");
        if(!wl.isHeld())wl.acquire(); //Bildschirm bleibt an!

/*        if (android.os.Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // Explain to the user why we need to read the contacts
                }

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                // app-defined int constant that should be quite unique

                return;
            }
        }*/
        //Toast.makeText(getApplicationContext(),String.valueOf(android.os.Build.VERSION.SDK_INT)+"|"+String.valueOf(MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE), Toast.LENGTH_SHORT).show();

        ContentResolver contentResolver = getContentResolver();
        //isSDPresent=externalMemoryAvailable();
        isSDPresent=true;
        if(isSDPresent){uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;}
        else{uri = android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI;}

        String[] STAR = { "*" };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        cursor = contentResolver.query(uri, STAR, selection, null, null);


        //cursor = managedQuery(allsongsuri, STAR, selection, null, null);

        //String[] columns = new String[]{MediaStore.Audio.AudioColumns.DATA};
        //cursor = contentResolver.query(uri, columns, null, null, null);

        //Log.i(TAG, "Querying media...");
        //Log.i(TAG, "URI: " + uri.toString());


        if (cursor == null) {
            // query failed, handle error.
            Toast.makeText(getApplicationContext(),"SoundBox: Query failed. No music found. (handle)", Toast.LENGTH_SHORT).show();

        } else if (!cursor.moveToFirst()) {
            // no media on the device
            Toast.makeText(getApplicationContext(),"SoundBox: Query failed. No music found.", Toast.LENGTH_SHORT).show();
        } else {
            int artistColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
            int durationColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DURATION);
            int titleColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            counter=0;
            do {
                counter++;

            } while (cursor.moveToNext());
        }

        playMusic();

        //getAllSongsFromSDCARD();
    }
    public void getAllSongsFromSDCARD() {
        String[] STAR = { "*" };
        //Cursor cursor;
        Uri allsongsuri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        cursor = managedQuery(allsongsuri, STAR, selection, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                counter=0;
                do {
                    String song_name = cursor
                            .getString(cursor
                                    .getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    int song_id = cursor.getInt(cursor
                            .getColumnIndex(MediaStore.Audio.Media._ID));

                    String fullpath = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Audio.Media.DATA));

                    String album_name = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    int album_id = cursor.getInt(cursor
                            .getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));

                    String artist_name = cursor.getString(cursor
                            .getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    int artist_id = cursor.getInt(cursor
                            .getColumnIndex(MediaStore.Audio.Media.ARTIST_ID));

                    msg.insert(0, "song name "+fullpath + "\n");
                    light_text.setText(msg);
                    light_text.invalidate();
                    counter++;

                } while (cursor.moveToNext());

            }
            //cursor.close();
            playMusic();

        }
    }
    public boolean externalMemoryAvailable() {
        if (Environment.isExternalStorageRemovable()) {
            //device support sd card. We need to check sd card availability.
            String state = Environment.getExternalStorageState();
            return state.equals(Environment.MEDIA_MOUNTED) || state.equals(
                    Environment.MEDIA_MOUNTED_READ_ONLY);
        } else {
            //Toast.makeText(getApplicationContext(),"Keine SD Card gefunden!", Toast.LENGTH_SHORT).show();
            //device not support sd card.
            return false;
       }
    }
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //Toast.makeText(getApplicationContext(),"seekbar progress: "+progress, Toast.LENGTH_SHORT).show();
        seekbar_value=progress;
        text.setText("Sensitivity: "+String.valueOf(seekbar_value));
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //Toast.makeText(getApplicationContext(),"seekbar touch started!", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //Toast.makeText(getApplicationContext(),"seekbar touch stopped!", Toast.LENGTH_SHORT).show();
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor e = sp.edit();
        e.putInt("sensitivity", seekbar_value);
        e.commit();
    }

    protected void hideView(int id) {
        findViewById(id).setVisibility(View.GONE);
    }
    protected void showView(int id) {
        findViewById(id).setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.container) {
            if(hider==false) {showView(R.id.icon);showView(R.id.next);showView(R.id.seekBar1);showView(R.id.song);showView(R.id.text);showView(R.id.titel);showView(R.id.light);hider=true;} else {hideView(R.id.icon);hideView(R.id.song);hideView(R.id.next);hideView(R.id.seekBar1);hideView(R.id.text);hideView(R.id.titel);hideView(R.id.light);hider=false;}
        }
        if(view.getId()==R.id.titel || view.getId()==R.id.next || view.getId()==R.id.icon) {
            playMusic();
        }
    }
    public void playMusic(){
        if(mpMusic!=null) {
            try {
                mpMusic.reset();
                mpMusic.prepare();
                mpMusic.stop();
                mpMusic.release();
                mpMusic = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        do { // Nicht den selben Titel hintereinander spielen!
            rndcount = (int) (Math.random() * counter+1);
        }while(rndcount==changer); // wenn es der gleiche Titel ist, noch mal würfeln!

        changer = rndcount;
        //mpMusic = MediaPlayer.create(this, resID[changer]);
        long id = changer;
        Uri contentUri;
//        if(isSDPresent){contentUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);}
//        else{contentUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI, id);}
        cursor.moveToPosition(changer-1);
//        int musicColumn = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
        int artistColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
        String fullpath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        int durationColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DURATION);
        int titleColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
        int idColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
        int timeSeconds=cursor.getInt(durationColumn)/1000;
        int timeMinutes=timeSeconds/60;
        timeSeconds=timeSeconds-(timeMinutes*60);

        Log.i(TAG, "ID: " + cursor.getLong(idColumn) + " Artist: " + cursor.getString(artistColumn) + " Title: " + cursor.getString(titleColumn) + " Title: " + cursor.getString(titleColumn));
        song.setText(changer+"/"+counter+" Artist: " + cursor.getString(artistColumn) + " Title: " + cursor.getString(titleColumn) + " " + timeMinutes+":"+String.format("%02d",timeSeconds));
//        song.setText(changer+"/"+counter+" Artist: " + cursor.getString(artistColumn) + " Title: " + cursor.getString(titleColumn) + " " + timeMinutes+":"+String.format("%02d",timeSeconds)+" "+cursor.getString(musicColumn));
        output=changer+"/"+counter+" Artist: " + cursor.getString(artistColumn) + " Title: " + cursor.getString(titleColumn) + " " + timeMinutes+":"+String.format("%02d",timeSeconds);

        try {
            //String path = contentUri.getPath();
            //msg.insert(0, "path: "+fullpath + "\n");
            //light_text.setText(msg);
            //light_text.invalidate();

            mpMusic = new MediaPlayer();
            mpMusic.setAudioStreamType(AudioManager.STREAM_MUSIC);
//            mpMusic.setDataSource(getApplicationContext(), contentUri);
            FileInputStream is = new FileInputStream(fullpath);
            mpMusic.setDataSource(is.getFD());

            mpMusic.prepare();
            mpMusic.start();

        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mpMusic.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mpMusic) {
                playMusic();
            }
        });

        mpMusic.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                //Toast.makeText(MainActivity.this, "Error: "+String.valueOf(output), Toast.LENGTH_SHORT).show();
                return false;

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

//            int hasWritePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int hasReadPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
  //          int hasCameraPermission = checkSelfPermission(Manifest.permission.CAMERA);

            List<String> permissions = new ArrayList<String>();
    /*        if (hasWritePermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            }*/

            if (hasReadPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);

            }
/*
            if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.CAMERA);

            }*/
            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), 111);
            }
        }
    }

   @Override
    protected void onResume() {
        mgr.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mgr.unregisterListener(this, light);
        super.onDestroy();
        if(mpMusic!=null) {
            mpMusic.stop();
            try {
                mpMusic.reset();
                mpMusic.prepare();
                mpMusic.stop();
                mpMusic.release();
                mpMusic=null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(wl.isHeld())wl.release();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        msg.insert(0, sensor.getName() + " accuracy changed: " + accuracy +
//                (accuracy==1?" (LOW)":(accuracy==2?" (MED)":" (HIGH)")) + "\n");
//        text.setText(msg);
//        text.invalidate();
    }

    public void onSensorChanged(SensorEvent event) {
        StringBuilder msg = new StringBuilder(2048);
        msg.insert(0, "Lightsensor: " + event.values[0] + " SI lux\n");
        light_text.setText(msg);
        light_text.invalidate();

        if(mpMusic!=null && mpMusic.isPlaying() && event.values[0]<seekbar_value) {
            mpMusic.pause();
        }
        if(mpMusic!=null && !mpMusic.isPlaying() && event.values[0]>=seekbar_value) {
            mpMusic.start();
        }
      }
}