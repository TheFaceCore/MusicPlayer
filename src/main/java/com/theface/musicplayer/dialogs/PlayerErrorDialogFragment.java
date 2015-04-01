package com.theface.musicplayer.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import com.theface.musicplayer.R;

/**
 * Created by TheFace Date: 26.03.2015 Time: 11:41
 */
public class PlayerErrorDialogFragment extends DialogFragment{
    public interface Listener {
        void onReloadClick();
        void onSkipClick();
        void onBackButtonClick(DialogFragment dialog);
    }

    private static final String KEY_ERROR_MSG = "error_msg";
    private static final String KEY_INFO_MODE = "info_mode";

    private Listener listener;
    private int errorMsgId;
    private boolean infoMode;

    public static DialogFragment newInstance(int errorMsgId, boolean infoMode) {
        PlayerErrorDialogFragment f = new PlayerErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_ERROR_MSG, errorMsgId);
        args.putBoolean(KEY_INFO_MODE, infoMode);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null || !getArguments().containsKey(KEY_ERROR_MSG)) {
            throw new RuntimeException("Use PlaylistLoaderErrorDialogFragment.newInstance for fragment instantiation");
        }
        errorMsgId = getArguments().getInt(KEY_ERROR_MSG);
        infoMode = getArguments().getBoolean(KEY_INFO_MODE);
        if (!infoMode) {
            setCancelable(false);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement PlayerErrorDialogFragment.Listener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(errorMsgId);
        if (infoMode) {
            builder.setPositiveButton(R.string.ok, null);
        }  else {
            builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.onSkipClick();
                }
            })
                    .setPositiveButton(R.string.track_reload, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onReloadClick();
                        }
                    })
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                                listener.onBackButtonClick(PlayerErrorDialogFragment.this);
                            }
                            return true;
                        }
                    });
        }
        return builder.create();
    }

}
