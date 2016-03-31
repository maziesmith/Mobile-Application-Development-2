package edu.neu.madcourse.dictionary;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Html;

import edu.neu.madcourse.wordgame.Constants;

/**
 * Created by rachit on 05-02-2016.
 */
public class MyDialogFragment extends DialogFragment {

    public MyDialogFragment() {

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String title = getArguments().getString(Constants.DIALOG_TITLE);
        builder.setTitle(title);
        builder.setCancelable(false);
        String acknowledge = getArguments().getString(Constants.ACKNOWLEDGEMENT);
        builder.setMessage(Html.fromHtml(acknowledge));

        if (title.equals("GAME OVER")) {
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getActivity().finish();
                    dismiss();
                }
            });
        } else {
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });
        }
        return builder.create();
    }
}
