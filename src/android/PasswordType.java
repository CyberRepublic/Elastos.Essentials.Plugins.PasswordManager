package org.elastos.essentials.plugins.passwordmanager;

public enum PasswordType {
    /** Simple password/private key/string info. */
    GENERIC_PASSWORD(0),
    /** Wifi network with SSID and password. */
    WIFI(1),
    /** Bank account, national or international format. */
    BANK_ACCOUNT(2),
    /** Bank card. */
    BANK_CARD(3),
    /** Any kind of account make of an identifier and a password. */
    ACCOUNT(4);

    public int mValue;

    PasswordType(int value) {
        mValue = value;
    }

    public static PasswordType fromValue(int value) {
        for(PasswordType t : values()) {
            if (t.mValue == value) {
                return t;
            }
        }
        return GENERIC_PASSWORD;
    }
}
