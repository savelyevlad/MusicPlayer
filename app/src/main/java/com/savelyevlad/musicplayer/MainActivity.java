package com.savelyevlad.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.savelyevlad.musicplayer.services.MediaPlayerService;
import com.savelyevlad.musicplayer.tools.Audio;
import com.savelyevlad.musicplayer.tools.AudioAdapter;
import com.savelyevlad.musicplayer.tools.StorageUtil;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class MainActivity extends AppCompatActivity {

    public Button getButtonPrevious() {
        return buttonPrevious;
    }

    public Button getButtonNext() {
        return buttonNext;
    }

    public Button getButtonAction() {
        return buttonAction;
    }

    public TextView getTextViewSongName() {
        return textViewSongName;
    }

    public SeekBar getSeekBar() {
        return seekBar;
    }

    public ListView getListView() {
        return listView;
    }

    //    @Getter
    private Button buttonPrevious;
//    @Getter
    private Button buttonNext;
//    @Getter
    private Button buttonAction;
//    @Getter
    private TextView textViewSongName;
//    @Getter
    private SeekBar seekBar;
//    @Getter
    private ListView listView;

    private ArrayList<Audio> audioList;
    private AudioAdapter audioAdapter;

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.savelyevlad.musicplayer.PlayNewAudio";

    private MediaPlayerService player;
    boolean serviceBound = false;
    private boolean isPaused = true;

    private MainActivity getThis() {
        return this;
    }

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            player.setMainActivity(getThis());
            serviceBound = true;

            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE,
                              Manifest.permission.MEDIA_CONTENT_CONTROL,
                              Manifest.permission.INTERNET,
                              Manifest.permission.READ_PHONE_STATE},
                1);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonAction = findViewById(R.id.button_action);
        buttonNext = findViewById(R.id.button_next);
        buttonPrevious = findViewById(R.id.button_previous);
        seekBar = findViewById(R.id.seekBar);
        textViewSongName = findViewById(R.id.textView_songName);
        listView = findViewById(R.id.listView);

        loadAudio();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            private boolean isChanging = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(isChanging) {
                    player.goToPosition(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isChanging = true;
                player.pauseMedia();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isChanging = false;
                player.resumeMedia();
            }
        });

        buttonAction.setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = v -> {
        switch (v.getId()) {
            case R.id.button_action:
                if(isPaused && !serviceBound) {
//                    playAudio("https://upload.wikimedia.org/wikipedia/commons/6/6c/Grieg_Lyric_Pieces_Kobold.ogg");
//                    playAudio(audioList.get(0).getData());
                }
                else if(isPaused) {
                    resumeAudio();
                }
                else {
                    pauseAudio();
                }
//                playAudio(audioList.get(0).getData());
                break;
        }
    };

    private void playAudio(int audioIndex) {
        //Check is service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(audioList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    private void pauseAudio() {
        if (!isPaused && player != null) {
            player.pauseMedia();
            isPaused = true;
            buttonAction.setText(R.string.play);
        }
    }

    private void resumeAudio() {
        if (isPaused && player != null) {
            buttonAction.setText("Pause");
            player.resumeMedia();
            isPaused = false;
        }
    }

    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                // Save to audioList
                audioList.add(new Audio(data, title, album, artist));
            }
        }
        cursor.close();

        audioAdapter = new AudioAdapter(this, audioList);
        listView.setAdapter(audioAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }
    }
}