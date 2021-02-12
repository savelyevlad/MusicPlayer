package com.savelyevlad.musicplayer.tools;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.savelyevlad.musicplayer.R;

import java.util.ArrayList;

public class AudioAdapter extends BaseAdapter {

    private Context context;
    private LayoutInflater lInflater;
    private ArrayList<Audio> objects;

    public AudioAdapter(Context context, ArrayList<Audio> audios) {
        this.context = context;
        objects = audios;
        lInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

        ((TextView) (view.findViewById(R.id.textView_songName))).setText(curr.getTitle());

        return view;
    }

    // товар по позиции
    public Audio getAudio(int position) {
        return ((Audio) getItem(position));
    }
}
