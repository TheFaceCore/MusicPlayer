package com.theface.musicplayer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.theface.musicplayer.info.TrackInfo;

import java.util.List;

/**
 * Created by TheFace Date: 17.03.2015 Time: 17:28
 */
public class PlayerListAdapter extends ArrayAdapter<TrackInfo>{

    private LayoutInflater inflater;
    private int resourceId;

    public PlayerListAdapter(Activity context, int resourceId, List<TrackInfo> objects) {
        super(context, resourceId, objects);
        this.resourceId = resourceId;
        inflater = context.getLayoutInflater();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent);
    }

    private View createViewFromResource(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder;

        if (convertView == null) {
            view = inflater.inflate(resourceId, parent, false);
            viewHolder = new ViewHolder();

            viewHolder.notLoadedLayout = view.findViewById(R.id.not_loaded_layout);
            viewHolder.uriTextView = (TextView) viewHolder.notLoadedLayout.findViewById(R.id.track_uri);

            viewHolder.loadingLayout = view.findViewById(R.id.loading_layout);
            viewHolder.progressBar = (ProgressBar) viewHolder.loadingLayout.findViewById(R.id.progress_bar);
            viewHolder.loadingUriTextView = (TextView) viewHolder.loadingLayout.findViewById(R.id.loading_track_uri);

            viewHolder.loadedLayout = view.findViewById(R.id.loaded_layout);
            viewHolder.nameTextView = (TextView) viewHolder.loadedLayout.findViewById(R.id.track_name);
            viewHolder.actionImageView = (ImageView) viewHolder.loadedLayout.findViewById(R.id.track_action_img);

            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        TrackInfo info = getItem(position);

        switch (info.getState()) {
            case NOT_LOADED:
                viewHolder.notLoadedLayout.setVisibility(View.VISIBLE);
                viewHolder.loadingLayout.setVisibility(View.GONE);
                viewHolder.loadedLayout.setVisibility(View.GONE);
                viewHolder.uriTextView.setText(info.getUri());
                break;
            case LOADING:
                viewHolder.notLoadedLayout.setVisibility(View.GONE);
                viewHolder.loadingLayout.setVisibility(View.VISIBLE);
                viewHolder.loadedLayout.setVisibility(View.GONE);
                viewHolder.progressBar.setProgress(info.getProgress());
                viewHolder.loadingUriTextView.setText(info.getUri());
                break;
            case LOADED:
                viewHolder.notLoadedLayout.setVisibility(View.GONE);
                viewHolder.loadingLayout.setVisibility(View.GONE);
                viewHolder.loadedLayout.setVisibility(View.VISIBLE);
                viewHolder.nameTextView.setText(info.getName());
                if (info.isPlaying()) {
                    viewHolder.actionImageView.setImageResource(R.drawable.ic_action_stop);
                    viewHolder.actionImageView.setColorFilter(getColor(R.color.action_stop_tint));
                } else {
                    viewHolder.actionImageView.setImageResource(R.drawable.ic_action_play);
                    viewHolder.actionImageView.setColorFilter(getColor(R.color.action_play_tint));
                }
                break;
            default:
                throw new RuntimeException("Unknown track state: " + info.getState());
        }

        return view;
    }

    private int getColor(int colorResId) {
        return getContext().getResources().getColor(colorResId);
    }

    private static class ViewHolder {
        public View notLoadedLayout;
        public View loadingLayout;
        public View loadedLayout;
        public TextView uriTextView;
        public TextView loadingUriTextView;
        public ProgressBar progressBar;
        public TextView nameTextView;
        public ImageView actionImageView;
    }
}
