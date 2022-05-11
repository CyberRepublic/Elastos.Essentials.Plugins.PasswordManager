package org.elastos.essentials.plugins.passwordmanager.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.CancellationSignal;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.biometric.BiometricManager;
import androidx.cardview.widget.CardView;

import org.apache.cordova.CordovaPlugin;
import org.elastos.essentials.plugins.passwordmanager.FakeR;
import org.elastos.essentials.plugins.passwordmanager.PasswordManager;
import org.elastos.essentials.plugins.passwordmanager.UIStyling;
import org.elastos.essentials.plugins.fingerprint.FingerPrintAuthHelper;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

public class MasterPasswordPrompter extends AlertDialog {
    public interface OnCancelClickedListener {
        void onCancelClicked();
    }

    public interface OnNextClickedListener {
        void onNextClicked(String password, boolean shouldSavePasswordToBiometric);
    }

    public interface OnErrorListener {
        void onError(String error);
    }

    public static class Builder {
        private CordovaPlugin cordovaPlugin;
        private Activity activity;
        private String did;
        private PasswordManager passwordManager;
        private FingerPrintAuthHelper fingerPrintAuthHelper;
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
        TextView lblWrongPassword;
        TextView lblRecreate;
        EditText etPassword;
        Button btCancel;
        Button btNext;
        CardView cardDeny;
        CardView cardAccept;
        Switch swBiometric;
        LinearLayout llBiometric;
        TextView lblBiometricIntro;

        public Builder(CordovaPlugin cordovaPlugin, String did, PasswordManager passwordManager) {
            this.cordovaPlugin = cordovaPlugin;
            this.activity = cordovaPlugin.cordova.getActivity();
            this.did = did;
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

        public Builder prompt(boolean passwordWasWrong, boolean reCreate) {
            View view = LayoutInflater.from(this.activity).inflate(fakeR.getId("layout", "dialog_password_manager_prompt"), null);

            // Hook UI items
            llRoot = view.findViewById(fakeR.getId("id", "llRoot"));
            llMainContent = view.findViewById(fakeR.getId("id", "llMainContent"));
            lblTitle = view.findViewById(fakeR.getId("id", "lblTitle"));
            lblIntro = view.findViewById(fakeR.getId("id", "lblIntro"));
            lblWrongPassword = view.findViewById(fakeR.getId("id", "lblWrongPassword"));
            lblRecreate = view.findViewById(fakeR.getId("id", "lblRecreate"));
            etPassword = view.findViewById(fakeR.getId("id", "etPassword"));
            btCancel = view.findViewById(fakeR.getId("id", "btCancel"));
            btNext = view.findViewById(fakeR.getId("id", "btNext"));
            cardDeny = view.findViewById(fakeR.getId("id", "cardDeny"));
            cardAccept = view.findViewById(fakeR.getId("id", "cardAccept"));
            swBiometric = view.findViewById(fakeR.getId("id", "swBiometric"));
            llBiometric = view.findViewById(fakeR.getId("id", "llBiometricInitiate"));
            lblBiometricIntro = view.findViewById(fakeR.getId("id", "lblBiometricIntro"));

            // Customize colors
            llRoot.setBackgroundColor(UIStyling.popupMainBackgroundColor);
            llMainContent.setBackgroundColor(UIStyling.popupSecondaryBackgroundColor);
            lblTitle.setTextColor(UIStyling.popupMainTextColor);
            lblIntro.setTextColor(UIStyling.popupMainTextColor);
            cardDeny.setCardBackgroundColor(UIStyling.popupSecondaryBackgroundColor);
            btCancel.setTextColor(UIStyling.popupMainTextColor);
            cardAccept.setCardBackgroundColor(UIStyling.popupSecondaryBackgroundColor);
            btNext.setTextColor(UIStyling.popupMainTextColor);
            swBiometric.setTextColor(UIStyling.popupMainTextColor);
            etPassword.setTextColor(UIStyling.popupMainTextColor);
            etPassword.setHintTextColor(UIStyling.popupInputHintTextColor);
            etPassword.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
            lblBiometricIntro.setTextColor(UIStyling.popupMainTextColor);

            if (reCreate) {
                // After adding a new fingerprint, a KeyPermanentlyInvalidatedException occurs.
                lblRecreate.setVisibility(View.VISIBLE);
                lblWrongPassword.setVisibility(View.GONE);
            }
            else {
                lblRecreate.setVisibility(View.GONE);
                if (passwordWasWrong)
                    lblWrongPassword.setVisibility(View.VISIBLE);
                else
                    lblWrongPassword.setVisibility(View.GONE);
            }

            btCancel.setOnClickListener(v -> {
                cancel();
            });

            btNext.setOnClickListener(v -> {
                String password = etPassword.getText().toString();

                // Disable biometric auth for next times if user doesn't want to use that any more
                if (!swBiometric.isChecked()) {
                    passwordManager.setBiometricAuthEnabled(did, false);
                }

                boolean shouldSaveToBiometric = shouldInitiateBiometry && swBiometric.isChecked();
                if (swBiometric.isChecked() && !shouldInitiateBiometry) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        fingerPrintAuthHelper = new FingerPrintAuthHelper(this.cordovaPlugin, did);
                        fingerPrintAuthHelper.init();
                        activity.runOnUiThread(() -> {
                            fingerPrintAuthHelper.authenticateAndGetPassword(PasswordManager.MASTER_PASSWORD_BIOMETRIC_KEY, new FingerPrintAuthHelper.AuthenticationCallback() {
                                @Override
                                public void onSuccess(String password) {
                                    alertDialog.dismiss();
                                    onNextClickedListener.onNextClicked(password, shouldSaveToBiometric);
                                }

                                @Override
                                public void onFailure(String message) {
                                    alertDialog.dismiss();
                                    onErrorListener.onError(message);
                                }
                            });
                        });

                    }
                }
                else {
                    // Only allow validating the popup if some password is set
                    if (!password.equals("")) {
                        alertDialog.dismiss();
                        onNextClickedListener.onNextClicked(password, shouldSaveToBiometric);
                    }
                }
            });

            if (reCreate) {
                swBiometric.setChecked(true);
            } else {
                swBiometric.setChecked(passwordManager.isBiometricAuthEnabled(did));
            }

            // If biometric auth is not enabled, we will follow the flow to initiate it during this prompter session.
            shouldInitiateBiometry = !passwordManager.isBiometricAuthEnabled(did);

            if (canUseBiometrictAuth()) {
                if (shouldInitiateBiometry) {
                    setTextPasswordVisible(true);
                    setBiometryLayoutVisible(false);
                }
                else {
                    setTextPasswordVisible(false);
                    setBiometryLayoutVisible(true);
                    updateBiometryIntroText();
                }

                swBiometric.setOnCheckedChangeListener((compoundButton, checked) -> {
                    if (checked) {
                        // Willing to enable biometric auth?
                        setBiometryLayoutVisible(false);
                        updateBiometryIntroText();
                    }
                    else {
                        // Willing to disable biometric auth?
                        shouldInitiateBiometry = true;
                        setBiometryLayoutVisible(false);
                        setTextPasswordVisible(true);

                        // Focus the password input
                        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        etPassword.requestFocus();
                    }
                });
            }
            else {
                // No biometric auth mechanism available - hide the feature
                llBiometric.setVisibility(View.GONE);
                swBiometric.setVisibility(View.GONE);
            }

            alertDialogBuilder.setView(view);
            alertDialog = alertDialogBuilder.create();
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            alertDialog.show();

            return this;
        }

        public void cancel() {
            alertDialog.dismiss();
            onCancelClickedListener.onCancelClicked();
        }

        public FingerPrintAuthHelper getFingerPrintAuthHelper() {
            return fingerPrintAuthHelper;
        }

        private void setTextPasswordVisible(boolean shouldShow) {
            if (shouldShow)
                etPassword.setVisibility(View.VISIBLE);
            else
                etPassword.setVisibility(View.GONE);
        }

        private void setBiometryLayoutVisible(boolean shouldShow) {
            if (shouldShow)
                llBiometric.setVisibility(View.VISIBLE);
            else
                llBiometric.setVisibility(View.GONE);
        }

        private void updateBiometryIntroText() {
            lblBiometricIntro.setText(fakeR.getId("string", "pwm_prompt_continue_with_biometry"));
        }

        private boolean canUseBiometrictAuth() {
            BiometricManager biometricManager = BiometricManager.from(activity.getApplicationContext());
            switch (biometricManager.canAuthenticate()) {
                case BiometricManager.BIOMETRIC_SUCCESS:
                    return true;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                default:
                    return false;
            }
        }
    }


    protected MasterPasswordPrompter(Context context, int themeResId) {
        super(context, themeResId);
    }
}