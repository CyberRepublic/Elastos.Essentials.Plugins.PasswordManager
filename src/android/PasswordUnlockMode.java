package org.elastos.essentials.plugins.passwordmanager;

public enum PasswordUnlockMode {
    /**
     * After been unlocked once, password manager access is open during some time and until
     * elastOS exits. Users don't have to provide their master password again during this time,
     * and all apps can get their password information directly.
     */
    UNLOCK_FOR_A_WHILE(0),

    /**
     * Users have to provide their master password every time an application requests a password.
     * This provides higher security in case the device is stolen, but this is less convenient
     * for users.
     */
    UNLOCK_EVERY_TIME(1);

    private int mValue;

    PasswordUnlockMode(int value) {
        mValue = value;
    }

    public static PasswordUnlockMode fromValue(int value) {
        for(PasswordUnlockMode t : values()) {
            if (t.mValue == value) {
                return t;
            }
        }
        return UNLOCK_FOR_A_WHILE;
    }
}