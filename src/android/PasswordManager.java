package org.elastos.essentials.plugins.passwordmanager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.elastos.essentials.plugins.passwordmanager.dialogs.MasterPasswordCreator;
import org.elastos.essentials.plugins.passwordmanager.dialogs.MasterPasswordPrompter;
import org.elastos.essentials.plugins.passwordmanager.passwordinfo.PasswordInfo;
import org.elastos.essentials.plugins.fingerprint.FingerPrintAuthHelper;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Database format is a plain JSON file, not mysql, why? Because we want to ensure unicity when changing the
 * master password (and in a simple way). The JSON file is then re-encrypted at once. It also better matches the
 * custom password info data that we store, instead of storing JSON strings in a mysql table.
 */
public class PasswordManager {
    private static final String LOG_TAG = "PWDManager";
    private static final String SHARED_PREFS_KEY = "PWDMANAGERPREFS";

    public static final String MASTER_PASSWORD_BIOMETRIC_KEY = "masterpasswordkey";

    private static final String PREF_KEY_UNLOCK_MODE = "unlockmode";
    private static final String PREF_KEY_APPS_PASSWORD_STRATEGY = "appspasswordstrategy";

    private Activity activity;
    private final CordovaPlugin cordovaPlugin;
    private static PasswordManager instance;
    private HashMap<String, PasswordDatabaseInfo> databasesInfo = new HashMap<>();
    private String did = null;
    private MasterPasswordPrompter.Builder activeMasterPasswordPrompt = null;
    private FingerPrintAuthHelper fingerPrintAuthHelper = null;

    private interface BasePasswordManagerListener {
        void onCancel();
        void onError(String error);
    }

    private interface OnDatabaseLoadedListener extends BasePasswordManagerListener {
        void onDatabaseLoaded();
    }

    private interface OnDatabaseSavedListener extends BasePasswordManagerListener {
        void onDatabaseSaved();
    }

    public interface OnMasterPasswordCreationListener extends BasePasswordManagerListener {
        void onMasterPasswordCreated();
    }

    public interface OnMasterPasswordChangeListener extends BasePasswordManagerListener {
        void onMasterPasswordChanged();
    }

    public interface OnMasterPasswordRetrievedListener extends BasePasswordManagerListener {
        void onMasterPasswordRetrieved(String password);
    }

    public interface OnPasswordInfoRetrievedListener extends BasePasswordManagerListener {
        void onPasswordInfoRetrieved(PasswordInfo info);
    }

    public interface OnAllPasswordInfoRetrievedListener extends BasePasswordManagerListener {
        void onAllPasswordInfoRetrieved(ArrayList<PasswordInfo> info);
    }

    public interface OnPasswordInfoDeletedListener extends BasePasswordManagerListener {
        void onPasswordInfoDeleted();
    }

    public interface OnPasswordInfoSetListener extends BasePasswordManagerListener {
        void onPasswordInfoSet();
    }

    public PasswordManager(CordovaPlugin cordovaPlugin) {
        this.cordovaPlugin = cordovaPlugin;
        this.activity = cordovaPlugin.cordova.getActivity();
    }

    public static PasswordManager getSharedInstance(CordovaPlugin cordovaPlugin) {
        if (PasswordManager.instance == null) {
            PasswordManager.instance = new PasswordManager(cordovaPlugin);
        }
        return PasswordManager.instance;
    }

    public static void Destroy() {
        PasswordManager.instance = null;
    }

    /**
     * Saves or updates a password information into the secure database.
     * The passwordInfo's key field is checked to match existing content. Existing content
     * is overwritten.
     *
     * Password info could fail to be saved in case user cancels the master password creation or enters
     * a wrong master password then cancels.
     */
    public void setPasswordInfo(PasswordInfo info, String did, String appID, OnPasswordInfoSetListener listener) throws Exception {
        checkMasterPasswordCreationRequired(did, new OnMasterPasswordCreationListener() {
            @Override
            public void onMasterPasswordCreated() {
                loadDatabase(did, new OnDatabaseLoadedListener() {
                    @Override
                    public void onDatabaseLoaded() {
                        try {
                            setPasswordInfoReal(info, did, appID);
                            listener.onPasswordInfoSet();
                        }
                        catch (Exception e) {
                            listener.onError(e.getMessage());
                        }
                    }

                    @Override
                    public void onCancel() {
                        listener.onCancel();
                    }

                    @Override
                    public void onError(String error) {
                        listener.onError(error);
                    }
                }, false);
            }

            @Override
            public void onCancel() {
                listener.onCancel();
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    /**
     * Using a key identifier, returns a previously saved password info.
     *
     * A regular application can only access password info that it created itself.
     * The password manager application is able to access information from all applications.
     *
     * @param key Unique key identifying the password info to retrieve.
     *
     * @returns The password info, or null if nothing was found.
     */
    public void getPasswordInfo(String key, String did, String appID, PasswordGetInfoOptions options, OnPasswordInfoRetrievedListener listener) throws Exception {
        checkMasterPasswordCreationRequired(did, new OnMasterPasswordCreationListener() {
            @Override
            public void onMasterPasswordCreated() {
                // In case caller doesn't want to show the password prompt if the database is locked, we return a cancellation exception.
                if (!isDatabaseLoaded(did) && !options.promptPasswordIfLocked) {
                    listener.onCancel();
                    return;
                }

                loadDatabase(did, new OnDatabaseLoadedListener() {
                    @Override
                    public void onDatabaseLoaded() {
                        try {
                            PasswordInfo info = getPasswordInfoReal(key, did, appID);
                            listener.onPasswordInfoRetrieved(info);
                        }
                        catch (Exception e) {
                            listener.onError(e.getMessage());
                        }
                    }

                    @Override
                    public void onCancel() {
                        listener.onCancel();
                    }

                    @Override
                    public void onError(String error) {
                        listener.onError(error);
                    }
                }, false, options.forceMasterPasswordPrompt, false);
            }

            @Override
            public void onCancel() {
                listener.onCancel();
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    /**
     * Returns the whole list of password information contained in the password database.
     *
     * Only the password manager application is allowed to call this API.
     *
     * @returns The list of existing password information.
     */
    public void getAllPasswordInfo(String did, String appID, OnAllPasswordInfoRetrievedListener listener) throws Exception {
        checkMasterPasswordCreationRequired(did, new OnMasterPasswordCreationListener() {
            @Override
            public void onMasterPasswordCreated() {
                loadDatabase(did, new OnDatabaseLoadedListener() {
                    @Override
                    public void onDatabaseLoaded() {
                        try {
                            ArrayList<PasswordInfo> infos = getAllPasswordInfoReal(did);
                            listener.onAllPasswordInfoRetrieved(infos);
                        }
                        catch (Exception e) {
                            listener.onError(e.getMessage());
                        }
                    }

                    @Override
                    public void onCancel() {
                        listener.onCancel();
                    }

                    @Override
                    public void onError(String error) {
                        listener.onError(error);
                    }
                }, false);
            }

            @Override
            public void onCancel() {
                listener.onCancel();
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    /**
     * Deletes an existing password information from the secure database.
     *
     * A regular application can only delete password info that it created itself.
     * The password manager application is able to delete information from all applications.
     *
     * @param key Unique identifier for the password info to delete.
     */
    public void deletePasswordInfo(String key, String did, String appID, String targetAppID, OnPasswordInfoDeletedListener listener) throws Exception {
        loadDatabase(did, new OnDatabaseLoadedListener() {
            @Override
            public void onDatabaseLoaded() {
                try {
                    deletePasswordInfoReal(key, did, targetAppID);
                    listener.onPasswordInfoDeleted();
                }
                catch (Exception e) {
                    listener.onError(e.getMessage());
                }
            }

            @Override
            public void onCancel() {
                listener.onCancel();
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        }, false);
    }

    /**
     * Convenience method to generate a random password based on given criteria (options).
     * Used by applications to quickly generate new user passwords.
     *
     * @param options unused for now
     */
    public String generateRandomPassword(PasswordCreationOptions options) {
        int sizeOfRandomString = 8;

        String allowedCharacters = "";
        allowedCharacters += "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        allowedCharacters += "abcdefghijklmnopqrstuvwxyz";
        allowedCharacters += "0123456789";
        allowedCharacters += "!@#$%^&*()_-+=<>?/{}~|";

        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);

        for(int i=sb.length();i < sizeOfRandomString;++i){
            sb.append(allowedCharacters.charAt(random.nextInt(allowedCharacters.length())));
        }

        return sb.toString();
    }

    /**
     * Sets the new master password for the current DID session. This master password locks the whole
     * database of password information.
     *
     * In case of a master password change, the password info database is re-encrypted with this new password.
     *
     * Only the password manager application is allowed to call this API.
     */
    public void changeMasterPassword(String did, String appID, OnMasterPasswordChangeListener listener) throws Exception {
        loadDatabase(did, new OnDatabaseLoadedListener() {
            @Override
            public void onDatabaseLoaded() {
                // No database exists. Start the master password creation flow
                new MasterPasswordCreator.Builder(activity, PasswordManager.this)
                    .setOnNextClickedListener(password -> {
                        // Master password was provided and confirmed. Now we can use it.

                        try {
                            PasswordDatabaseInfo dbInfo = databasesInfo.get(did);

                            // Changing the master password means re-encrypting the database with a different password
                            encryptAndSaveDatabase(did, password);

                            // Remember the new password locally
                            dbInfo.activeMasterPassword = password;

                            // Disable biometric auth to force re-activating it, as the password has changed.
                            setBiometricAuthEnabled(did, false);

                            listener.onMasterPasswordChanged();
                        }
                        catch (Exception e) {
                            listener.onError(e.getMessage());
                        }
                    })
                    .setOnCancelClickedListener(listener::onCancel)
                    .setOnErrorListener(listener::onError)
                    .prompt();
            }

            @Override
            public void onCancel() {
                listener.onCancel();
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        }, false);
    }

    /**
     * If the master password has ben unlocked earlier, all passwords are accessible for a while.
     * This API re-locks the passwords database and further requests from applications to this password
     * manager will require user to provide his master password again.
     */
    public void lockMasterPassword(String did) throws Exception {
        lockDatabase(did);
    }

    /**
     * Deletes all password information for the active DID session. The encrypted passwords database
     * is deleted without any way to recover it.
     */
    public void deleteAll(String did) throws Exception {
        // Lock currently opened database
        lockDatabase(did);

        // Delete the permanent storage
        deleteDatabase(did);
    }

    /**
     * Sets the unlock strategy for the password info database. By default, once the master password
     * if provided once by the user, the whole database is unlocked for a while, until elastOS exits,
     * or if one hour has passed, or if it's manually locked again.
     *
     * For increased security, user can choose to get prompted for the master password every time using
     * this API.
     *
     * This API can be called only by the password manager application.
     *
     * @param unlockMode Unlock strategy to use.
     */
    public void setUnlockMode(PasswordUnlockMode unlockMode, String did, String appID) throws Exception {
        getPrefs(did).edit().putInt(PREF_KEY_UNLOCK_MODE, unlockMode.ordinal()).apply();

        // if the mode becomes UNLOCK_EVERY_TIME, we lock the database
        if (getUnlockMode(did) != PasswordUnlockMode.UNLOCK_EVERY_TIME && unlockMode == PasswordUnlockMode.UNLOCK_EVERY_TIME) {
            lockDatabase(did);
        }
    }

    private PasswordUnlockMode getUnlockMode(String did) throws Exception {
        int savedUnlockModeAsInt = getPrefs(did).getInt(PREF_KEY_UNLOCK_MODE, PasswordUnlockMode.UNLOCK_FOR_A_WHILE.ordinal());
        return PasswordUnlockMode.fromValue(savedUnlockModeAsInt);
    }

    private void loadDatabase(String did, OnDatabaseLoadedListener listener, boolean isPasswordRetry) {
        loadDatabase(did, listener, isPasswordRetry, false, false);
    }

    private void loadDatabase(String did, OnDatabaseLoadedListener listener, boolean isPasswordRetry, boolean forcePasswordPrompt, boolean reCreate) {
        try {
            if (isDatabaseLoaded(did) && !sessionExpired(did) && !forcePasswordPrompt) {
                listener.onDatabaseLoaded();
            } else {
                if (sessionExpired(did)) {
                    lockDatabase(did);
                }

                // Master password is locked - prompt it to user
                // First make sure to cancel any on going popup instance.
                if (activeMasterPasswordPrompt != null) {
                    activeMasterPasswordPrompt.cancel();
                    activeMasterPasswordPrompt = null;
                }

                activeMasterPasswordPrompt = new MasterPasswordPrompter.Builder(cordovaPlugin, did, this)
                        .setOnNextClickedListener((password, shouldSavePasswordToBiometric) -> {
                            activeMasterPasswordPrompt = null;
                            try {
                                // Force loading the database even if it's already loaded. That's the way to check if the
                                // possibly forced password input is right or not. Reloading the database will not break
                                // anything.
                                loadEncryptedDatabase(did, password);

                                if (isDatabaseLoaded(did)) {
                                    // User chose to enable biometric authentication (was not enabled before). So we save the
                                    // master password to the biometric crypto space.
                                    if (shouldSavePasswordToBiometric) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            activity.runOnUiThread(() -> {
                                                fingerPrintAuthHelper = new FingerPrintAuthHelper(this.cordovaPlugin, did);
                                                fingerPrintAuthHelper.init();
                                                fingerPrintAuthHelper.authenticateAndSavePassword(MASTER_PASSWORD_BIOMETRIC_KEY, password, new FingerPrintAuthHelper.AuthenticationCallback() {
                                                    @Override
                                                    public void onSuccess(String password) {
                                                        // Save user's choice to use biometric auth method next time
                                                        setBiometricAuthEnabled(did, true);

                                                        listener.onDatabaseLoaded();
                                                    }

                                                    @Override
                                                    public void onFailure(String message) {
                                                        Log.e(LOG_TAG, "Biometric authentication failed to initiate");
                                                        Log.e(LOG_TAG, message);

                                                        // In case of authentication failed, try again
                                                        if (message.contains("Authentication failed")) {
                                                            setBiometricAuthEnabled(did, false);
                                                        }

                                                        // Biometric save failed, but we still could open the database, so we return a success here.
                                                        // Though, we don't save user's choice to enable biometric auth.
                                                        listener.onDatabaseLoaded();
                                                    }
                                                });
                                            });
                                        }
                                    } else {
                                        listener.onDatabaseLoaded();
                                    }
                                } else
                                    listener.onError("Unknown error while trying to load the passwords database");
                            } catch (Exception e) {
                                // In case of wrong password exception, try again
                                if (e.getMessage().contains("BAD_DECRYPT")) {
                                    loadDatabase(did, listener, true, forcePasswordPrompt, false);
                                } else {
                                    // Other exceptions are passed raw
                                    listener.onError(e.getMessage());
                                }
                            }
                        })
                        .setOnCancelClickedListener(() -> {
                            activeMasterPasswordPrompt = null;
                            listener.onCancel();
                        })
                        .setOnErrorListener((err) -> {
                            activeMasterPasswordPrompt = null;
                            // After adding a new fingerprint, a KeyPermanentlyInvalidatedException occurs.
                            if (err.contains("Key Permanently Invalidated")) {
                                setBiometricAuthEnabled(did,false);
                                loadDatabase(did, listener, true, forcePasswordPrompt, true);
                            } else if (err.contains("Authentication failed")) {
                                loadDatabase(did, listener, true, forcePasswordPrompt, false);
                            } else {
                                listener.onError(err);
                            }
                        })
                        .prompt(isPasswordRetry, reCreate);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * A "session" is when a database is unlocked. This session can be considered as expired for further calls,
     * in case user wants to unlock the database every time, or in case it's been first unlocked a too long time ago (auto relock
     * for security).
     */
    private boolean sessionExpired(String did) throws Exception {
        if (getUnlockMode(did) == PasswordUnlockMode.UNLOCK_EVERY_TIME)
            return true;

        PasswordDatabaseInfo dbInfo = databasesInfo.get(did);
        if (dbInfo == null)
            return true;

        // Last opened more than 1 hour ago? -> Expired
        long oneHourMs = (60*60*1000L);
        return dbInfo.openingTime.getTime() < (new Date().getTime() - oneHourMs);
    }

    private boolean isDatabaseLoaded(String did) {
        return (databasesInfo.get(did) != null);
    }

    private void lockDatabase(String did) {
        PasswordDatabaseInfo dbInfo = databasesInfo.get(did);
        if (dbInfo != null) {
            dbInfo.lock();
            databasesInfo.remove(did);
        }
    }

    private String getDatabaseFilePath(String did) {
        String dataDir = activity.getFilesDir() + "/data/pwm/" + did;
        return dataDir + "/store.db";
    }

    private void ensureDbPathExists(String dbPath) {
        new File(dbPath).getParentFile().mkdirs();
    }

    private boolean databaseExists(String did) {
        return new File(getDatabaseFilePath(did)).exists();
    }

    private void createEmptyDatabase(String did, String masterPassword) {
        // No database exists yet. Return an empty database info.
        PasswordDatabaseInfo dbInfo = PasswordDatabaseInfo.createEmpty();
        databasesInfo.put(did, dbInfo);

        // Save the master password
        dbInfo.activeMasterPassword = masterPassword;
    }

    private void deleteDatabase(String did) {
        String dbPath = getDatabaseFilePath(did);
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    /**
     * Using user's master password, decrypt the passwords list from disk and load it into memory.
     */
    private void loadEncryptedDatabase(String did, String masterPassword) throws Exception {
        if (masterPassword == null || masterPassword.equals("")) {
            throw new Exception("Empty master password is not allowed");
        }

        String dbPath = getDatabaseFilePath(did);
        ensureDbPathExists(dbPath);

        File file = new File(dbPath);

        if (!file.exists()) {
            createEmptyDatabase(did, masterPassword);
        }
        else {
            // Read the saved serialized hashmap as object
            FileInputStream fis = new FileInputStream(dbPath);
            ObjectInputStream ois = new ObjectInputStream(fis);
            HashMap<String, byte[]> map = (HashMap<String, byte[]>) ois.readObject();

            // Now that we've loaded the file, try to decrypt it
            byte[] decrypted = null;
            try {
                decrypted = decryptData(map, masterPassword);

                // We can now load the database content as a JSON object
                try {
                    String jsonData = new String(decrypted, StandardCharsets.UTF_8);
                    PasswordDatabaseInfo dbInfo = PasswordDatabaseInfo.fromJson(jsonData);
                    databasesInfo.put(did, dbInfo);

                    // Decryption was successful, saved master password in memory for a while.
                    dbInfo.activeMasterPassword = masterPassword;
                } catch (JSONException e) {
                    throw new Exception("Passwords database JSON content for did " + did + " is corrupted");
                }
            } catch (IOException e) {
                throw new Exception("Passwords database file for did " + did + " is corrupted");
            }
        }
    }

    /**
     * Decryption attempt, multi-steps (different iteration counts) for backward compatibility with
     * older keys.
     */
     // NOTE: implementation not used, because 327680 is a bit slow on some devices - not worth the change for now
   /*  private byte[] decryptData(HashMap<String, byte[]> map, String masterPassword) throws Exception
    {
        Exception decryptionError = null;
        int[] iterationCounts = {327680, 1324}; // Most probable iteration counts should be first in the list
        for (int i=0;i<iterationCounts.length; i++) {
            try {
                byte[] decrypted = _decryptData(map, masterPassword, iterationCounts[i]);
                // If we are here, this means we could decrypt.
                return decrypted;
            }
            catch (Exception e) {
                if (e.getMessage().contains("BAD_DECRYPT")) {
                    // Remember one of the decrypt exceptions to re-throw it in the end if none of the methods worked
                    decryptionError = e;
                }
                else {
                    // Other exception than decryption: immediately forward the exception and stop.
                    throw e;
                }
            }
        }

        // If we are here, this means none of the above attempts have succeeded
        throw decryptionError;
    } */

    // private byte[] _decryptData(HashMap<String, byte[]> map, String masterPassword, int iterationCount) throws Exception
    private byte[] decryptData(HashMap<String, byte[]> map, String masterPassword) throws Exception
    {
        byte[] decrypted = null;

        byte[] salt = map.get("salt");
        byte[] iv = map.get("iv");
        byte[] encrypted = map.get("encrypted");

        // Regenerate key from password
        char[] passwordChar = masterPassword.toCharArray();
        PBEKeySpec pbKeySpec = new PBEKeySpec(passwordChar, salt, 1324, 256);
        // PBEKeySpec pbKeySpec = new PBEKeySpec(passwordChar, salt, iterationCount, 256);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = secretKeyFactory.generateSecret(pbKeySpec).getEncoded();
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        // Decrypt
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        decrypted = cipher.doFinal(encrypted);

        return decrypted;
    }

    private void encryptAndSaveDatabase(String did, String masterPassword) throws Exception {
        String dbPath = getDatabaseFilePath(did);
        ensureDbPathExists(dbPath);

        // Make sure the database is open
        PasswordDatabaseInfo dbInfo = databasesInfo.get(did);
        if (dbInfo == null) {
            throw new Exception("Can't save a closed database");
        }

        // Convert JSON data into bytes
        byte[] data = dbInfo.rawJson.toString().getBytes();

        // Encrypt and get result
        HashMap<String, byte[]> result = encryptData(data, masterPassword);

        // Save Salt, IV and encrypted data as serialized hashmap object in the database file.
        FileOutputStream fos = new FileOutputStream(new File(dbPath));
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(result);
        oos.close();
    }

    private HashMap<String, byte[]> encryptData(byte[] plainTextBytes, String masterPassword) throws Exception
    {
        HashMap<String, byte[]> map = new HashMap<String, byte[]>();

        // Random salt for next step
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[256];
        random.nextBytes(salt);

        // PBKDF2 - derive the key from the password, don't use passwords directly
        char[] passwordChar = masterPassword.toCharArray(); // Turn password into char[] array
        PBEKeySpec pbKeySpec = new PBEKeySpec(passwordChar, salt, 1324, 256);
        // PBEKeySpec pbKeySpec = new PBEKeySpec(passwordChar, salt, 327680, 256);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = secretKeyFactory.generateSecret(pbKeySpec).getEncoded();
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        // Create initialization vector for AES
        SecureRandom ivRandom = new SecureRandom(); // Not caching previous seeded instance of SecureRandom
        byte[] iv = new byte[16];
        ivRandom.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Encrypt
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(plainTextBytes);

        map.put("salt", salt);
        map.put("iv", iv);
        map.put("encrypted", encrypted);

        return map;
    }

    private void setPasswordInfoReal(PasswordInfo info, String did, String appID) throws Exception {
        PasswordDatabaseInfo dbInfo = databasesInfo.get(did);
        dbInfo.setPasswordInfo(appID, info);
        encryptAndSaveDatabase(did, dbInfo.activeMasterPassword);
    }

    private PasswordInfo getPasswordInfoReal(String key, String did, String appID) throws Exception {
        return databasesInfo.get(did).getPasswordInfo(appID, key);
    }

    private ArrayList<PasswordInfo> getAllPasswordInfoReal(String did) throws Exception {
        return  databasesInfo.get(did).getAllPasswordInfo();
    }

    private void deletePasswordInfoReal(String key, String did, String targetAppID) throws Exception {
        PasswordDatabaseInfo dbInfo = databasesInfo.get(did);
        databasesInfo.get(did).deletePasswordInfo(targetAppID, key);
        encryptAndSaveDatabase(did, dbInfo.activeMasterPassword);
    }

    private SharedPreferences getPrefs(String did) {
        return activity.getSharedPreferences(SHARED_PREFS_KEY+did, Context.MODE_PRIVATE);
    }

    /**
     * Checks if a password database exists (master password was set). If not, starts the master password
     * creation flow. After completion, calls the listener so that the base flow can continue.
     */
    private void checkMasterPasswordCreationRequired(String did, OnMasterPasswordCreationListener listener) {
        if (databaseExists(did)) {
            listener.onMasterPasswordCreated();
        }
        else {
            // No database exists. Start the master password creation flow
            new MasterPasswordCreator.Builder(activity, this)
                .setOnNextClickedListener(password -> {
                    // Master password was provided and confirmed. Now we can use it.

                    // Create an empty database
                    createEmptyDatabase(did, password);

                    try {
                        // Save this empty database to remember that we have defined a master password
                        encryptAndSaveDatabase(did, password);

                        listener.onMasterPasswordCreated();
                    }
                    catch (Exception e) {
                        listener.onError(e.getMessage());
                    }
                })
                .setOnCancelClickedListener(listener::onCancel)
                .setOnErrorListener(listener::onError)
                .prompt();
        }
    }

    public boolean isBiometricAuthEnabled(String did) {
        return getPrefs(did).getBoolean("biometricauth", false);
    }

    public void setBiometricAuthEnabled(String did, boolean useBiometricAuth) {
        getPrefs(did).edit().putBoolean("biometricauth", useBiometricAuth).apply();
    }

    public FingerPrintAuthHelper getFingerPrintAuthHelper() {
        if (activeMasterPasswordPrompt != null) {
            return activeMasterPasswordPrompt.getFingerPrintAuthHelper();
        } else {
            return fingerPrintAuthHelper;
        }
    }
}
