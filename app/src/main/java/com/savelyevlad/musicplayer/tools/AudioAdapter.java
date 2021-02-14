package com.savelyevlad.musicplayer.tools;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.savelyevlad.musicplayer.MainActivity;
import com.savelyevlad.musicplayer.R;
import com.savelyevlad.musicplayer.services.MediaPlayerService;

import java.util.ArrayList;

public class AudioAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater lInflater;
    private ArrayList<Audio> objects;
    private MediaPlayerService player;

    public AudioAdapter(Context context, ArrayList<Audio> audios, MediaPlayerService playerService) {
        this.context = context;
        objects = audios;
        lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        player = playerService;
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public Object getItem(int position) {
        return objects.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position) {
        return position;
    }

    // пункт списка
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // используем созданные, но не используемые view
        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(R.layout.audio_item, parent, false);
        }

        Audio curr = getAudio(position);

        int milliseconds = Integer.parseInt(curr.getDuration());
        int minutes = milliseconds / 1000 / 60;
        int seconds = milliseconds / 1000 % 60;

        String time = minutes + ":" + seconds;

        ((TextView) (view.findViewById(R.id.textView_songName))).setText(curr.getTitle());
        ((TextView) view.findViewById(R.id.textView_songDuration)).setText(time);

        if(player == null) {
            player = ((MainActivity) context).getPlayer();
        }

        if(player != null && position == player.getAudioIndex()) {
            view.setBackgroundColor(0xFF00FF00);
        }
        else {
            view.setBackgroundColor(0);
        }

        return view;
    }

    // товар по позиции
    public Audio getAudio(int position) {
        return ((Audio) getItem(position));
    }
}
