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
public class PlaylistLoaderErrorDialogFragment extends DialogFragment{
    public interface Listener {
        void onFinishingClick();
        void onReloadClick();
        void onCancelClick();
        void onBackButtonClick(DialogFragment dialog);
    }

    private static final String KEY_ERROR_MSG = "error_msg";
    private static final String KEY_FINISHING_MODE = "finishing_mode";

    private Listener listener;
    private int errorMsgId;
    private boolean finishingMode;

    public static DialogFragment newInstance(int errorMsgId, boolean finishingMode) {
        PlaylistLoaderErrorDialogFragment f = new PlaylistLoaderErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_ERROR_MSG, errorMsgId);
        args.putBoolean(KEY_FINISHING_MODE, finishingMode);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        if (getArguments() == null || !getArguments().containsKey(KEY_ERROR_MSG)) {
            throw new RuntimeException("Use PlaylistLoaderErrorDialogFragment.newInstance for fragment instantiation");
        }
        errorMsgId = getArguments().getInt(KEY_ERROR_MSG);
        finishingMode = getArguments().getBoolean(KEY_FINISHING_MODE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            listener = (Listener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement PlaylistLoaderErrorDialogFragment.Listener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(errorMsgId);
        if (finishingMode) {
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.onFinishingClick();
                }
            })
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                                dialog.dismiss();
                                listener.onFinishingClick();
                            }
                            return true;
                        }
                    });
        } else {
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.onCancelClick();
                }
            })
                    .setPositiveButton(R.string.playlist_reload, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            listener.onReloadClick();
                        }
                    })
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                                listener.onBackButtonClick(PlaylistLoaderErrorDialogFragment.this);
                            }
                            return true;
                        }
                    });
        }
        return builder.create();
    }

}
