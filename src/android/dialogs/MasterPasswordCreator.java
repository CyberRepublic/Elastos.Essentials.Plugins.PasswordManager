package org.elastos.essentials.plugins.passwordmanager.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.CancellationSignal;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import org.elastos.essentials.plugins.passwordmanager.FakeR;
import org.elastos.essentials.plugins.passwordmanager.PasswordManager;
import org.elastos.essentials.plugins.passwordmanager.UIStyling;

public class MasterPasswordCreator extends AlertDialog {
    public interface OnCancelClickedListener {
        void onCancelClicked();
    }

    public interface OnNextClickedListener {
        void onNextClicked(String password);
    }

    public interface OnDontUseMasterPasswordListener {
        void onDontUseMasterPassword();
    }

    public interface OnErrorListener {
        void onError(String error);
    }

    public static class Builder {
        private Activity activity;
        private PasswordManager passwordManager;
        private AlertDialog.Builder alertDialogBuilder;
        private AlertDialog alertDialog;
        private OnCancelClickedListener onCancelClickedListener;
        private OnNextClickedListener onNextClickedListener;
        private OnErrorListener onErrorListener;
        private boolean shouldInitiateBiometry; // Whether biometry should be prompted to save password, or just used (previously saved)

        private FakeR fakeR;

        // UI items
        LinearLayout llRoot;
        LinearLayout llMainContent;
        TextView lblTitle;
        TextView lblIntro;
        EditText etPassword;
        EditText etPasswordRepeat;
        TextView lblWrongPassword;
        Button btCancel;
        Button btNext;
        CardView cardDeny;
        CardView cardAccept;

        public Builder(Activity activity, PasswordManager passwordManager) {
            this.activity = activity;
            this.passwordManager = passwordManager;

            alertDialogBuilder = new android.app.AlertDialog.Builder(activity);
            alertDialogBuilder.setCancelable(false);

            fakeR = new FakeR(this.activity);
        }

        public Builder setOnCancelClickedListener(OnCancelClickedListener listener) {
            this.onCancelClickedListener = listener;
            return this;
        }

        public Builder setOnNextClickedListener(OnNextClickedListener listener) {
            this.onNextClickedListener = listener;
            return this;
        }

        public Builder setOnErrorListener(OnErrorListener listener) {
            this.onErrorListener = listener;
            return this;
        }

        public void prompt() {
            //TODO
//            Context localizedContext = PreferenceManager.getShareInstance().getLocalizedContext(activity);
            View view = LayoutInflater.from(this.activity).inflate(fakeR.getId("layout", "dialog_password_manager_create"), null);

            // Hook UI items
            llRoot = view.findViewById(fakeR.getId("id", "llRoot"));
            llMainContent = view.findViewById(fakeR.getId("id", "llMainContent"));
            lblTitle = view.findViewById(fakeR.getId("id", "lblTitle"));
            lblIntro = view.findViewById(fakeR.getId("id", "lblIntro"));
            lblWrongPassword = view.findViewById(fakeR.getId("id", "lblWrongPassword"));
            etPassword = view.findViewById(fakeR.getId("id", "etPassword"));
            etPasswordRepeat = view.findViewById(fakeR.getId("id", "etPasswordRepeat"));
            btCancel = view.findViewById(fakeR.getId("id", "btCancel"));
            btNext = view.findViewById(fakeR.getId("id", "btNext"));
            cardDeny = view.findViewById(fakeR.getId("id", "cardDeny"));
            cardAccept = view.findViewById(fakeR.getId("id", "cardAccept"));

            // Customize colors
            llRoot.setBackgroundColor(UIStyling.popupMainBackgroundColor);
            llMainContent.setBackgroundColor(UIStyling.popupSecondaryBackgroundColor);
            lblTitle.setTextColor(UIStyling.popupMainTextColor);
            lblIntro.setTextColor(UIStyling.popupMainTextColor);
            cardDeny.setCardBackgroundColor(UIStyling.popupSecondaryBackgroundColor);
            btCancel.setTextColor(UIStyling.popupMainTextColor);
            cardAccept.setCardBackgroundColor(UIStyling.popupSecondaryBackgroundColor);
            btNext.setTextColor(UIStyling.popupMainTextColor);
            etPassword.setTextColor(UIStyling.popupMainTextColor);
            etPassword.setHintTextColor(UIStyling.popupInputHintTextColor);
            etPasswordRepeat.setTextColor(UIStyling.popupMainTextColor);
            etPasswordRepeat.setHintTextColor(UIStyling.popupInputHintTextColor);

            lblWrongPassword.setVisibility(View.GONE);

            btCancel.setOnClickListener(v -> {
                alertDialog.dismiss();
                onCancelClickedListener.onCancelClicked();
            });

            btNext.setOnClickListener(v -> {
                String password = etPassword.getText().toString();
                String passwordRepeat = etPasswordRepeat.getText().toString();

                // Only allow validating the popup if some password is set
                if (!password.equals("") && password.equals(passwordRepeat)) {
                    alertDialog.dismiss();
                    onNextClickedListener.onNextClicked(password);
                }
                else if (!password.equals(passwordRepeat)) {
                    lblWrongPassword.setVisibility(View.VISIBLE);
                }
            });

            etPassword.setOnFocusChangeListener((view12, b) -> {
                lblWrongPassword.setVisibility(View.GONE);
            });
            etPasswordRepeat.setOnFocusChangeListener((view12, b) -> {
                lblWrongPassword.setVisibility(View.GONE);
            });

            alertDialogBuilder.setView(view);
            alertDialog = alertDialogBuilder.create();
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            alertDialog.show();
        }
    }

    public MasterPasswordCreator(Context context, int themeResId) {
        super(context, themeResId);
    }
}