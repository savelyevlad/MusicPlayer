package com.savelyevlad.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.View;
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

    public MediaPlayerService getPlayer() {
        return player;
    }

    private MediaPlayerService player;
    boolean serviceBound = false;
    private boolean isPaused = true;

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

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
                if(player != null && isChanging) {
                    player.goToPosition(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(player != null) {
                    isChanging = true;
                    player.pauseMedia();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(player != null) {
                    isChanging = false;
                    player.resumeMedia();
                }
            }
        });
        buttonAction.setOnClickListener(onClickListener);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            playAudio((int) id);
            audioAdapter.notifyDataSetChanged();
            checkIsPlaying();
//            view.setBackgroundColor(0xFF00FF00);
            if(lastClickedSong != null) {
//                lastClickedSong.setBackgroundColor(0);
            }
            lastClickedSong = view;
        });
    }

    public void checkIsPlaying() {
        if(player != null) {
            if(player.isPlaying()) {
                if(isPaused) {
                    isPaused = false;
                    buttonAction.setText("pause");
                }
            }
            else {
                if(!isPaused) {
                    isPaused = true;
                    buttonAction.setText(R.string.play);
                }
            }
        }
    }

    private View lastClickedSong = null;

    @SuppressLint("NonConstantResourceId")
    private View.OnClickListener onClickListener = v -> {
        switch (v.getId()) {
            case R.id.button_action:
                if(isPaused && !serviceBound) {
                    playAudio(0);
                }
                else if(isPaused) {
                    resumeAudio();
                }
                else {
                    pauseAudio();
                }
                break;
            case R.id.button_next:
                if(serviceBound && player != null) {
                    player.skipToNext();
                }
                break;
            case  R.id.button_previous:
                if(serviceBound && player != null) {
                    player.skipToPrevious();
                }
        }
    };

    private void playAudio(int audioIndex) {
        isPaused = false;
        buttonAction.setText("Pause");
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
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));

                // Save to audioList
                audioList.add(new Audio(data, title, album, artist, duration));
            }
        }
        cursor.close();

        audioAdapter = new AudioAdapter(this, audioList, player);
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