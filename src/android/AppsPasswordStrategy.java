package org.elastos.essentials.plugins.passwordmanager;

/**
 * Global strategy for the password manager, whether to use a master password or not.
 */
public enum AppsPasswordStrategy {
    /** Use a master password to save and encrypt all app password info. Default. */
    LOCK_WITH_MASTER_PASSWORD(0),
    /** Don't store app password info in the password manager. Users manually input in-app passwords every time. */
    DONT_USE_MASTER_PASSWORD(1);

    private int mValue;

    AppsPasswordStrategy(int value) {
        mValue = value;
    }

    public static AppsPasswordStrategy fromValue(int value) {
        for(AppsPasswordStrategy t : values()) {
            if (t.mValue == value) {
                return t;
            }
        }
        return LOCK_WITH_MASTER_PASSWORD;
    }
}