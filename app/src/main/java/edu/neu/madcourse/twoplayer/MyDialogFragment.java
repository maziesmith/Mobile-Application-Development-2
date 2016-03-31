package edu.neu.madcourse.twoplayer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * @author rachit on 25-03-2016.
 */
public class MyDialogFragment extends DialogFragment {

    public MyDialogFragment() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        Bundle bundle = getArguments();
        if (bundle != null) {
            final String title = bundle.getString(Constants.DIALOG_TITLE);
            builder.setTitle(title);
            builder.setMessage(bundle.getString(Constants.DIALOG_MESSAGE));
        }
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        return builder.create();
    }
}
