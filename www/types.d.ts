/*
* Copyright (c) 2018-2020 Elastos Foundation
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/

/**
* This plugin allows dApps to save and retrieve user passwords securely. Passwords are saved using a 
* master user password. 
* 
* Passwords are sandboxed with different master passwords for each DID session.
*
* This plugin also provide UI helpers to create or input passwords, in case dApps don't want to manage
* this.
*
* <br><br>
* Usage:
* <br>
* declare let passwordManager: PasswordManagerPlugin.PasswordManager;
*/

declare namespace PasswordManagerPlugin {
    /**
     * Improved boolean type to pass more verbose information.
     */
    type BooleanWithReason = {
        /** Actual boolean value */
        value: boolean;
        /** Optional reason message to explain why this boolean got this value */
        reason?: string;
    }

    /**
     * Type defining data format stored inside a password info.
     */
    const enum PasswordType {
        /** Simple password/private key/string info. */
        GENERIC_PASSWORD = 0,
        /** Wifi network with SSID and password. */
        WIFI = 1,
        /** Bank account, national or international format. */
        BANK_ACCOUNT = 2,
        /** Bank card. */
        BANK_CARD = 3,
        /** Any kind of account make of an identifier and a password. */
        ACCOUNT = 4
    }

    /**
     * Root type for all password information. This type is abstract and should not be used
     * directly.
     */
    type PasswordInfo = {
        /**
         * Unique key, used to identity the password info among other.
         */
        key: string;

        /**
         * Password type, that defines the format of contained information.
         */
        type: PasswordType;

        /**
         * Name used while displaying this info. Either set by users in the password manager app 
         * or by apps, when saving passwords automatically.
         */
        displayName: string;

        /**
         * List of any kind of app-specific additional information for this password entry.
         */
        custom?: Map<string, any>;

        /**
         * Package ID of the application/capsule that saved this password information.
         * READ-ONLY
         */
        appID?: String;
    }

    /**
     * Simple password info containing a simple string (ex: just a password, or a private key).
     */
    type GenericPasswordInfo = PasswordInfo & {
        password: string;
    }

    /**
     * Information about a wifi network.
     */
    type WifiPasswordInfo = PasswordInfo & {
        /** Wifi network unique identifier */
        wifiSSID: string;
        /** Wifi network password */
        wifiPassword: string;
    }

    /**
     * Information about a bank account, using local or international format.
     */
    type BankAccountPasswordInfo = PasswordInfo & {
        /** Account owner's name */
        accountOwner: string;
        /** Account IBAN number (international) */
        iban?: string;
        /** Account SWIFT number */
        swift?: string;
        /** Account BIC */
        bic?: string;
    }

    /**
     * Bank card type.
     */
    const enum BankCardType {
        /** Debit card */
        DEBIT = 0,
        /** Credit card */
        CREDIT = 1
    }

    /**
     * Information about a bank debit or credit card.
     */
    type BankCardPasswordInfo = PasswordInfo & {
        /** type of card. Debit, credit... */
        cardType?: BankCardType;
        /** Card owner's name */
        accountOwner: string;
        /** Card number without spaces */
        cardNumber: string;
        /** Card expiration date in ISO 8601 format */
        expirationDate: string;
        /** Card verification number, 3 digits */
        cvv?: string;
        /** Issuing bank name */
        bankName?: string;
    }

    /**
     * Standard ID/password web/app/other account.
     */
    type AccountPasswordInfo = PasswordInfo & {
        /** Account identifier (unique id, email address...) */
        identifier: string;
        /** Account password */
        password: string;
        /** Key provided by the service (google, etc) used to generated temporary 2FA passwords. */
        twoFactorKey?: string;
    }

    /**
     * Format options for password creation requests, in order to force generating passwords
     * with a specific format.
     */
    type PasswordCreationOptions = {
        // For now, no options such as the number of uppercased letters, special symbols, etc to keep things simple.
        // This is kept for future use.
    }

    /**
     * Mode defining how often the passwords database has to be unlocked in order to access application or
     * user password info.
     */
    const enum PasswordUnlockMode {
        /**
         * After been unlocked once, password manager access is open during some time and until
         * elastOS exits. Users don't have to provide their master password again during this time,
         * and all apps can get their password information directly.
         */
        UNLOCK_FOR_A_WHILE = 0,

        /**
         * Users have to provide their master password every time an application requests a password.
         * This provides higher security in case the device is stolen, but this is less convenient
         * for users.
         */
        UNLOCK_EVERY_TIME = 1
    }

    type GetPasswordInfoOptions = {
        /** 
         * If true, the master password is asked (popup) in case the database is locked. If false,
         * getPasswordInfo() fails silently and throws a cancellation exception. Default: true.
         */
        promptPasswordIfLocked?: boolean

        /**
         * Forces the user to re-enter his master password for the current operation, even if the database
         * is already unlocked. This is useful for security reasons, for example for payments, when we want to 
         * confirm that the acting user is really the device owner.
         * Default: false.
         */
        forceMasterPasswordPrompt?: boolean
    }

    /** The provided password is invalid */
    interface InvalidPasswordException extends Error {}

    /** Some parameters are invalid */
    interface InvalidParameterException extends Error {}

    /** User cancelled the operation */
    interface CancellationException extends Error {}

    /** Other kind of exception without additional typing information */
    interface UnspecifiedException extends Error {}

    interface PasswordManager {
        /**
         * Saves or updates a password information into the secure database.
         * The passwordInfo's key field is checked to match existing content. Existing content
         * is overwritten.
         * 
         * Password info could fail to be saved in case user cancels the master password creation or enters
         * a wrong master password then cancels.
         * 
         * @returns True if the password info was saved, false otherwise.
         */
        setPasswordInfo(info: PasswordInfo): Promise<BooleanWithReason>;

        /**
         * Using a key identifier, returns a previously saved password info.
         * 
         * @param key Unique key identifying the password info to retrieve.
         * 
         * @returns The password info, or null if nothing was found.
         */
        getPasswordInfo(key: string, options?: GetPasswordInfoOptions): Promise<PasswordInfo>;

        /**
         * Deletes an existing password information from the secure database.
         * 
         * @param key Unique identifier for the password info to delete.
         * 
         * @returns True if something could be deleted, false otherwise.
         */
        deletePasswordInfo(key: string): Promise<BooleanWithReason>;

        /**
         * Convenience method to generate a random password based on given criteria (options).
         * Used by applications to quickly generate new user passwords.
         * 
         * @param options 
         */
        generateRandomPassword(options?: PasswordCreationOptions): Promise<string>;

        /**
         * RESTRICTED
         * 
         * Sets the new master password for the current DID session. This master password locks the whole
         * database of password information.
         * 
         * In case of a master password change, the password info database is re-encrypted with this new password.
         * 
         * Only the password manager application is allowed to call this API.
         * 
         * @returns True if the master password was successfully changed, false otherwise.
         */
        changeMasterPassword(): Promise<BooleanWithReason>;
        
        /**
         * RESTRICTED
         * 
         * If the master password has ben unlocked earlier, all passwords are accessible for a while.
         * This API re-locks the passwords database and further requests from applications to this password
         * manager will require user to provide his master password again.
         */
        lockMasterPassword();

        /**
         * RESTRICTED
         * 
         * Deletes all password information for the active DID session. The encrypted passwords database
         * is deleted without any way to recover it.
         */
        deleteAll(): Promise<void>;

        /**
         * RESTRICTED 
         * 
         * Sets the unlock strategy for the password info database. By default, once the master password
         * if provided once by the user, the whole database is unlocked for a while, until elastOS exits,
         * or if one hour has passed, or if it's manually locked again.
         * 
         * For increased security, user can choose to get prompted for the master password every time using
         * this API.
         * 
         * This API can be called only by the password manager application.
         * 
         * @param mode Unlock strategy to use.
         */
        setUnlockMode(mode: PasswordUnlockMode);

        /**
         * RESTRICTED
         * 
         * Returns the whole list of password information contained in the password database.
         * 
         * Only the password manager application is allowed to call this API.
         * 
         * @returns The list of existing password information.
         */
        getAllPasswordInfo(): Promise<PasswordInfo[]>;
        
        /**
         * RESTRICTED 
         * 
         * Deletes an existing password information from the secure database, for a given application.
         * 
         * Only the password manager application can call this api.
         * 
         * @param key Unique identifier for the password info to delete.
         * 
         * @returns True if something could be deleted, false otherwise.
         */
        deleteAppPasswordInfo(targetAppId: string, key: string): Promise<BooleanWithReason>;

        /**
         * RESTRICTED
         * 
         * Used by the DID session application to toggle DID contexts and deal with DID creation, sign in,
         * sign out. When a virtual context is set, api call such as getPasswordInfo() don't use the currently
         * signed in DID, but they use this virtual DID instead.
         * 
         * @param didString The DID context to use for all further api calls. Pass null to clear the virtual context.
         */
        setVirtualDIDContext(didString: string): Promise<void>;
    }
}