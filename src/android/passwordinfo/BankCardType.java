package org.elastos.essentials.plugins.passwordmanager.passwordinfo;

public enum BankCardType {
    /** Debit card */
    DEBIT(0),
    /** Credit card */
    CREDIT(1);

    public int mValue;

    BankCardType(int value) {
        mValue = value;
    }

    public static BankCardType fromValue(int value) {
        for(BankCardType t : values()) {
            if (t.mValue == value) {
                return t;
            }
        }
        return DEBIT;
    }
}
