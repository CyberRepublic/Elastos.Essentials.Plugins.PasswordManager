package org.elastos.essentials.plugins.passwordmanager;

import android.graphics.Color;

public class UIStyling {
    public static int popupMainTextColor = Color.parseColor("#FFFFFF");
    public static int popupInputHintTextColor = Color.parseColor("#CCCCCC");
    public static int popupMainBackgroundColor = Color.parseColor("#FFFFFF");
    public static int popupSecondaryBackgroundColor = Color.parseColor("#FFFFFF");

    static void prepare(boolean useDarkMode) {
        if (useDarkMode) {
            // DARK MODE
            popupMainTextColor = Color.parseColor("#fdfeff");
            popupInputHintTextColor = Color.parseColor("#fdfeff");
            popupMainBackgroundColor = Color.parseColor("#2e2f4e");
            popupSecondaryBackgroundColor = Color.parseColor("#1c1d34");
        }
        else {
            // LIGHT MODE
            popupMainTextColor = Color.parseColor( "#161740");
            popupInputHintTextColor = Color.parseColor("#161740");
            popupMainBackgroundColor = Color.parseColor("#F0F0F0");
            popupSecondaryBackgroundColor = Color.parseColor("#FFFFFF");
        }
    }
}
