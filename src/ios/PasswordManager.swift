/*
* Copyright (c) 2021 Elastos Foundation
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

import Foundation
import CryptorRSA
import PopupDialog
import RNCryptor

public protocol BasePasswordManagerListener {
    func onCancel()
    func onError(_ error: String)
}

private protocol OnDatabaseLoadedListener : BasePasswordManagerListener {
    func onDatabaseLoaded()
}

private protocol OnDatabaseSavedListener : BasePasswordManagerListener {
    func onDatabaseSaved()
}

public protocol OnMasterPasswordCreationListener : BasePasswordManagerListener {
    func onMasterPasswordCreated()
}

public protocol OnMasterPasswordChangeListener : BasePasswordManagerListener {
    func onMasterPasswordChanged()
}

public protocol OnMasterPasswordRetrievedListener : BasePasswordManagerListener {
    func onMasterPasswordRetrieved(password: String)
}

public protocol OnPasswordInfoRetrievedListener : BasePasswordManagerListener {
    func onPasswordInfoRetrieved(info: PasswordInfo)
}

public protocol OnAllPasswordInfoRetrievedListener : BasePasswordManagerListener {
    func onAllPasswordInfoRetrieved(info: Array<PasswordInfo>)
}

public protocol OnPasswordInfoDeletedListener : BasePasswordManagerListener {
    func onPasswordInfoDeleted()
}

public protocol OnPasswordInfoSetListener : BasePasswordManagerListener {
    func onPasswordInfoSet()
}


/**
 * Database format is a plain JSON file, not mysql, why? Because we want to ensure unicity when changing the
 * master password (and in a simple way). The JSON file is then re-encrypted at once. It also better matches the
 * custom password info data that we store, instead of storing JSON strings in a mysql table.
 */
public class PasswordManager {
    private static let LOG_TAG = "PWDManager"
    private static let SHARED_PREFS_KEY = "PWDMANAGERPREFS"

    public static let MASTER_PASSWORD_BIOMETRIC_KEY = "masterpasswordkey"

    private static let PREF_KEY_UNLOCK_MODE = "unlockmode"
    private static let PREF_KEY_APPS_PASSWORD_STRATEGY = "appspasswordstrategy"

    private static var instance: PasswordManager? = nil
    private var viewController: CDVViewController?;
    private var databasesInfo = Dictionary<String, PasswordDatabaseInfo>()
    private var activeMasterPasswordPrompt: PopupDialog? = nil

    init() {
    }

    public static func getSharedInstance() -> PasswordManager {
        if (PasswordManager.instance == nil) {
            PasswordManager.instance = PasswordManager();
        }
        return PasswordManager.instance!;
    }

    func setViewController(_ viewController: CDVViewController) {
//        listenerReady = false;
        self.viewController = viewController;
//        let fiters = viewController.settings["internalintentfilter"] as? String;
//        let items = fiters!.split(separator: " ");
//        for item in items {
//            internalIntentFilters.append(String(item))
//        }
//        intentRedirecturlFilter = viewController.settings["intentredirecturlfilter"] as! String;
    }

    /**
     * Saves or updates a password information into the secure database.
     * The passwordInfo's key field is checked to match existing content. Existing content
     * is overwritten.
     *
     * Password info could fail to be saved in case user cancels the master password creation or enters
     * a wrong master password then cancels.
     */
    public func setPasswordInfo(info: PasswordInfo, did: String, appID: String,
                                onPasswordInfoSet: @escaping ()->Void,
                                onCancel: @escaping ()->Void,
                                onError: @escaping (_ error: String)->Void) throws {
        checkMasterPasswordCreationRequired(did: did, onMasterPasswordCreated: {
            self.loadDatabase(did: did, onDatabaseLoaded: {
                do {
                    try self.setPasswordInfoReal(info: info, did: did, appID: appID)
                    onPasswordInfoSet()
                }
                catch (let error) {
                    onError(error.localizedDescription)
                }
            }, onCancel: {
                onCancel()
            }, onError: { error in
                onError(error)
            }, isPasswordRetry: false)
        }, onCancel: {
            onCancel()
        }, onError: { error in
            onError(error)
        })
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
    public func getPasswordInfo(key: String, did: String, appID: String,
                                options: PasswordGetInfoOptions,
                                onPasswordInfoRetrieved: @escaping (_ password: PasswordInfo?)->Void,
                                onCancel: @escaping ()->Void,
                                onError: @escaping (_ error: String)->Void) throws {
        checkMasterPasswordCreationRequired(did: did, onMasterPasswordCreated: {
            // In case caller doesn't want to show the password prompt if the database is locked, we return a cancellation exception.
            if !self.isDatabaseLoaded(did: did) && !options.promptPasswordIfLocked {
                onCancel()
                return
            }

            self.loadDatabase(did: did, onDatabaseLoaded: {
                do {
                    let info = try self.getPasswordInfoReal(key: key, did: did, appID: appID)
                    onPasswordInfoRetrieved(info)
                }
                catch (let error) {
                    onError(error.localizedDescription)
                }
            }, onCancel: {
                onCancel()
            }, onError: { error in
                onError(error)
            }, isPasswordRetry: false, forcePasswordPrompt: options.forceMasterPasswordPrompt)
        }, onCancel: {
            onCancel()
        }, onError: { error in
            onError(error)
        })
    }

    /**
     * Returns the whole list of password information contained in the password database.
     *
     * @returns The list of existing password information.
     */
    public func getAllPasswordInfo(did: String, appID: String,
                                   onAllPasswordInfoRetrieved: @escaping (_ info: [PasswordInfo])->Void,
                                   onCancel: @escaping ()->Void,
                                   onError: @escaping (_ error: String)->Void) throws {
        checkMasterPasswordCreationRequired(did: did, onMasterPasswordCreated: {
            self.loadDatabase(did: did, onDatabaseLoaded: {
                do {
                    let infos = try self.getAllPasswordInfoReal(did: did)
                    onAllPasswordInfoRetrieved(infos)
                }
                catch (let error) {
                    onError(error.localizedDescription)
                }
            }, onCancel: {
                onCancel()
            }, onError: { error in
                onError(error)
            }, isPasswordRetry: false)

        }, onCancel: onCancel, onError: onError)
    }

    /**
     * Deletes an existing password information from the secure database.
     *
     * A regular application can only delete password info that it created itself.
     * The password manager application is able to delete information from all applications.
     *
     * @param key Unique identifier for the password info to delete.
     */
    public func deletePasswordInfo(key: String, did: String, appID: String, targetAppID: String,
                                   onPasswordInfoDeleted: @escaping ()->Void,
                                   onCancel: @escaping ()->Void,
                                   onError: @escaping (_ error: String)->Void) throws {
        loadDatabase(did: did, onDatabaseLoaded: {
            do {
                try self.deletePasswordInfoReal(key: key, did: did, targetAppID: targetAppID)
                onPasswordInfoDeleted()
            }
            catch (let error) {
                onError(error.localizedDescription)
            }
        }, onCancel: onCancel, onError: onError, isPasswordRetry: false)
    }

    /**
     * Convenience method to generate a random password based on given criteria (options).
     * Used by applications to quickly generate new user passwords.
     *
     * @param options unused for now
     */
    public func generateRandomPassword(options: PasswordCreationOptions?) -> String {
        let sizeOfRandomString = 8

        var allowedCharacters = ""
        allowedCharacters += "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        allowedCharacters += "abcdefghijklmnopqrstuvwxyz"
        allowedCharacters += "0123456789"
        allowedCharacters += "!@#$%^&*()_-+=<>?/{}~|"

        return String((0..<sizeOfRandomString).map{ _ in allowedCharacters.randomElement()! })
    }

    /**
     * Sets the new master password for the current DID session. This master password locks the whole
     * database of password information.
     *
     * In case of a master password change, the password info database is re-encrypted with this new password.
     *
     * Only the password manager application is allowed to call this API.
     */
    public func changeMasterPassword(did: String, appID: String,
                                     onMasterPasswordChanged: @escaping ()->Void,
                                     onCancel: @escaping ()->Void,
                                     onError: @escaping (_ error: String)->Void) throws {
        loadDatabase(did: did, onDatabaseLoaded: {
            let creatorController = MasterPasswordCreatorAlertController(nibName: "MasterPasswordCreator", bundle: Bundle.main)

            creatorController.setCanDisableMasterPasswordUse(false)

            let popup = PopupDialog(viewController: creatorController, buttonAlignment: .horizontal, transitionStyle: .fadeIn, preferredWidth: 340, tapGestureDismissal: false, panGestureDismissal: false, hideStatusBar: false, completion: nil)

            popup.view.backgroundColor = UIColor.clear // For rounded corners
            self.viewController!.present(popup, animated: false, completion: nil)

            creatorController.setOnPasswordCreatedListener { password in
                popup.dismiss()

                // Master password was provided and confirmed. Now we can use it.

                do {
                    if let dbInfo = self.databasesInfo[did] {
                        // Changing the master password means re-encrypting the database with a different password
                        try self.encryptAndSaveDatabase(did: did, masterPassword: password)

                        // Remember the new password locally
                        dbInfo.activeMasterPassword = password

                        // Disable biometric auth to force re-activating it, as the password has changed.
                        self.setBiometricAuthEnabled(did: did, false)

                        onMasterPasswordChanged()
                    }
                    else {
                        throw "No active database for DID \(did)"
                    }
                }
                catch (let error) {
                    onError(error.localizedDescription)
                }
            }

            creatorController.setOnCancelListener {
                popup.dismiss()
                onCancel()
            }
        }, onCancel: onCancel, onError: onError, isPasswordRetry: false)
    }

    /**
     * If the master password has ben unlocked earlier, all passwords are accessible for a while.
     * This API re-locks the passwords database and further requests from applications to this password
     * manager will require user to provide his master password again.
     */
    public func lockMasterPassword(did: String) throws {
        lockDatabase(did: did)
    }

    /**
     * Deletes all password information for the active DID session. The encrypted passwords database
     * is deleted without any way to recover it.
     */
    public func deleteAll(did: String) throws {
        // Lock currently opened database
        lockDatabase(did: did)

        // Delete the permanent storage
        deleteDatabase(did: did)
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
    public func setUnlockMode(unlockMode: PasswordUnlockMode, did: String, appID: String) throws {
        saveToPrefs(did: did, key: PasswordManager.PREF_KEY_UNLOCK_MODE, value: unlockMode.rawValue)

        // if the mode becomes UNLOCK_EVERY_TIME, we lock the database
        if (try getUnlockMode(did: did) != .UNLOCK_EVERY_TIME && unlockMode == PasswordUnlockMode.UNLOCK_EVERY_TIME) {
            lockDatabase(did: did)
        }
    }

    private func getUnlockMode(did: String) throws -> PasswordUnlockMode {
        let unlockModeAsInt = getPrefsInt(did: did, key: PasswordManager.PREF_KEY_UNLOCK_MODE, defaultValue: PasswordUnlockMode.UNLOCK_FOR_A_WHILE.rawValue)
        return PasswordUnlockMode(rawValue: unlockModeAsInt) ?? PasswordUnlockMode.UNLOCK_FOR_A_WHILE
    }

    private func loadDatabase(did: String,
                              onDatabaseLoaded: @escaping ()->Void,
                              onCancel: @escaping ()->Void,
                              onError: @escaping (_ error: String)->Void,
                              isPasswordRetry: Bool) {
        loadDatabase(did:did, onDatabaseLoaded: onDatabaseLoaded, onCancel: onCancel, onError: onError, isPasswordRetry: isPasswordRetry, forcePasswordPrompt: false);
    }

    private func loadDatabase(did: String,
                              onDatabaseLoaded: @escaping ()->Void,
                              onCancel: @escaping ()->Void,
                              onError: @escaping (_ error: String)->Void,
                              isPasswordRetry: Bool,
                              forcePasswordPrompt: Bool) {

        if (isDatabaseLoaded(did: did) && !sessionExpired(did: did) && !forcePasswordPrompt) {
            onDatabaseLoaded()
        }
        else {
            if (sessionExpired(did: did)) {
                lockDatabase(did: did)
            }

            if activeMasterPasswordPrompt != nil {
                print("Another password prompt is already active. Cancelling this request.");
                onCancel()
                return
            }

            // Master password is locked - prompt it to user
            let prompterController = MasterPasswordPrompterAlertController(nibName: "MasterPasswordPrompter", bundle: Bundle.main)

            prompterController.setDID(did)
            prompterController.setPasswordManager(self)
            prompterController.setPreviousAttemptWasWrong(isPasswordRetry)

            activeMasterPasswordPrompt = PopupDialog(viewController: prompterController, buttonAlignment: .horizontal, transitionStyle: .fadeIn, preferredWidth: 340, tapGestureDismissal: false, panGestureDismissal: false, hideStatusBar: false, completion: nil)

            activeMasterPasswordPrompt!.view.backgroundColor = UIColor.clear // For rounded corners
            DispatchQueue.main.async {
                self.viewController!.present(self.activeMasterPasswordPrompt!, animated: false, completion: nil)
            }

            prompterController.setOnPasswordTypedListener { password, shouldSavePasswordToBiometric in
                self.activeMasterPasswordPrompt!.dismiss()
                self.activeMasterPasswordPrompt = nil

                do {
                    // Happens in case the password could not be retrieved
                    if password == nil {
                        throw RNCryptor.Error.hmacMismatch
                    }

                    try self.loadEncryptedDatabase(did: did, masterPassword: password)
                    if (self.isDatabaseLoaded(did: did)) {
                        // User chose to enable biometric authentication (was not enabled before). So we save the
                        // master password to the biometric crypto space.
                        if (shouldSavePasswordToBiometric) {
                            let fingerPrintAuthHelper = FingerPrintAuthHelper(did: did)
                            fingerPrintAuthHelper.authenticateAndSavePassword(passwordKey: PasswordManager.MASTER_PASSWORD_BIOMETRIC_KEY, password: password!) { error in
                                if error == nil {
                                    // Save user's choice to use biometric auth method next time
                                    self.setBiometricAuthEnabled(did: did, true)

                                    onDatabaseLoaded()
                                }
                                else {
                                    // Biometric save failed, but we still could open the database, so we return a success here.
                                    // Though, we don't save user's choice to enable biometric auth.
                                    print("Biometric authentication failed to initiate")
                                    print(error!)
                                    onDatabaseLoaded()
                                }
                            }
                        }
                        else {
                            onDatabaseLoaded()
                        }
                    }
                    else {
                        onError("Unknown error while trying to load the passwords database")
                    }
                }
                catch RNCryptor.Error.hmacMismatch {
                    // In case of wrong password exception, try again
                    self.loadDatabase(did: did, onDatabaseLoaded: onDatabaseLoaded, onCancel: onCancel, onError: onError, isPasswordRetry: true, forcePasswordPrompt: forcePasswordPrompt)
                }
                catch (let error) {
                    // Other exceptions are passed raw
                    onError(error.localizedDescription)
                }
            }

            prompterController.setOnCancelListener {
                self.activeMasterPasswordPrompt!.dismiss()
                self.activeMasterPasswordPrompt = nil
                onCancel()
            }

            prompterController.setOnErrorListener { error in
                self.activeMasterPasswordPrompt!.dismiss()
                self.activeMasterPasswordPrompt = nil
                onError(error)
            }
        }
    }

    /**
     * A "session" is when a database is unlocked. This session can be considered as expired for further calls,
     * in case user wants to unlock the database every time, or in case it's been first unlocked a too long time ago (auto relock
     * for security).
     */
    private func sessionExpired(did: String) -> Bool {
        guard let unlockMode = try? getUnlockMode(did: did) else {
            return true
        }

        if unlockMode == .UNLOCK_EVERY_TIME {
            return true
        }

        guard let dbInfo = databasesInfo[did] else {
            return true
        }

        // Last opened more than 1 hour ago? -> Expired
        let oneHourMs = TimeInterval(60*60)
        return dbInfo.openingTime.timeIntervalSinceNow > oneHourMs
    }

    private func isDatabaseLoaded(did: String) -> Bool {
        return databasesInfo[did] != nil
    }

    private func lockDatabase(did: String) {
        if let dbInfo = databasesInfo[did] {
            dbInfo.lock()
            databasesInfo.removeValue(forKey: did)
        }
    }

    private func getDatabaseDirectory(did: String) -> String {
        return NSHomeDirectory() + "/Documents/data/pwm" + did
    }

    private func getDatabaseFilePath(did: String) -> String {
        let dbPath = getDatabaseDirectory(did: did) + "/store.db"
        ensureDbPathExists(did: did)
        return dbPath
    }

    private func ensureDbPathExists(did: String) {
        // Create folder in case it's missing
        try? FileManager.default.createDirectory(atPath: getDatabaseDirectory(did: did), withIntermediateDirectories: true, attributes: nil)
    }

    private func databaseExists(did: String) -> Bool {
        return FileManager.default.fileExists(atPath: getDatabaseFilePath(did: did))
    }

    private func createEmptyDatabase(did: String, masterPassword: String) {
        // No database exists yet. Return an empty database info.
        let dbInfo = PasswordDatabaseInfo.createEmpty()
        databasesInfo[did] = dbInfo

        // Save the master password
        dbInfo.activeMasterPassword = masterPassword;
    }

    private func deleteDatabase(did: String) {
        let dbPath = getDatabaseFilePath(did: did)
        if FileManager.default.fileExists(atPath: dbPath) {
            try? FileManager.default.removeItem(atPath: dbPath)
        }
    }

    /**
     * Using user's master password, decrypt the passwords list from disk and load it into memory.
     */
    private func loadEncryptedDatabase(did: String, masterPassword: String?) throws {
        guard let masterPassword = masterPassword else {
            throw "Master password is undefined"
        }

        let dbPath = getDatabaseFilePath(did: did)

        if (!databaseExists(did: did)) {
            createEmptyDatabase(did: did, masterPassword: masterPassword)
        }
        else {
            let encodedData = try Data(contentsOf: URL(fileURLWithPath: dbPath))

            // Now that we've loaded the file, try to decrypt it
            let decodedData = try decryptData(data: encodedData, masterPassword: masterPassword)

            // We can now load the database content as a JSON object
            do {
                if let jsonData = String(data: decodedData, encoding: .utf8), let jsonDict = jsonData.toDict() {
                    let dbInfo = try PasswordDatabaseInfo.fromDictionary(jsonDict)
                    databasesInfo[did] = dbInfo

                    // Decryption was successful, saved master password in memory for a while.
                    dbInfo.activeMasterPassword = masterPassword
                }
                else {
                    throw "Passwords database JSON content for did \(did) is corrupted: Can't decode to json string"
                }
            } catch (let error) {
                throw "Passwords database JSON content for did \(did) is corrupted: \(error.localizedDescription)"
            }
        }
    }

    private func decryptData(data: Data, masterPassword: String) throws -> Data {
        let decryptor = RNCryptor.Decryptor(password: masterPassword)
        let plaintext = NSMutableData()

        try plaintext.append(decryptor.update(withData: data))
        try plaintext.append(decryptor.finalData())

        return plaintext.copy() as! Data
    }

    private func encryptAndSaveDatabase(did: String, masterPassword: String) throws {
        let dbPath = getDatabaseFilePath(did: did)

        // Make sure the database is open
        guard let dbInfo = databasesInfo[did] else {
            throw "Can't save a closed database"
        }

        // Convert JSON data into bytes
        guard let jsonString = dbInfo.asDictionary().toString() else {
            throw "Unable to convert database json to json string"
        }

        // Encrypt and get result
        let data = Data(jsonString.utf8)
        let result = try encryptData(plainTextBytes: data, masterPassword: masterPassword)

        // Save encrypted data to the database file
        try result.write(to: URL(fileURLWithPath: dbPath))
    }

    private func encryptData(plainTextBytes: Data, masterPassword: String) throws -> Data {
        let encryptor = RNCryptor.Encryptor(password: masterPassword)
        let ciphertext = NSMutableData()

        ciphertext.append(encryptor.update(withData: plainTextBytes))
        ciphertext.append(encryptor.finalData())

        return ciphertext.copy() as! Data
    }

    private func setPasswordInfoReal(info: PasswordInfo, did: String, appID:String) throws {
        if let dbInfo = databasesInfo[did] {
            try dbInfo.setPasswordInfo(appID: appID, info: info)
            try encryptAndSaveDatabase(did: did, masterPassword: dbInfo.activeMasterPassword!)
        }
    }

    private func getPasswordInfoReal(key: String, did: String, appID: String) throws -> PasswordInfo? {
        return try databasesInfo[did]!.getPasswordInfo(appID: appID, key: key)
    }

    private func getAllPasswordInfoReal(did: String) throws -> [PasswordInfo]  {
        return try databasesInfo[did]!.getAllPasswordInfo()
    }

    private func deletePasswordInfoReal(key: String, did: String, targetAppID: String) throws {
        if let dbInfo = databasesInfo[did] {
            try dbInfo.deletePasswordInfo(appID: targetAppID, key: key)
            try encryptAndSaveDatabase(did: did, masterPassword: dbInfo.activeMasterPassword!)
        }
    }

    private func getUserDefaults(did: String) -> UserDefaults {
        return UserDefaults(suiteName: PasswordManager.SHARED_PREFS_KEY+did)!
    }

    private func saveToPrefs(did: String, key: String, value: Int) {
        getUserDefaults(did: did).set(value, forKey: key)
    }

    private func saveToPrefs(did: String, key: String, value: Bool) {
        getUserDefaults(did: did).set(value, forKey: key)
    }

    private func getPrefsInt(did: String, key: String, defaultValue: Int) -> Int {
        if getUserDefaults(did: did).object(forKey: key) == nil {
            return defaultValue
        } else {
            return getUserDefaults(did: did).integer(forKey: key)
        }
    }

    private func getPrefsBool(did: String, key: String, defaultValue: Bool) -> Bool {
        if getUserDefaults(did: did).object(forKey: key) == nil {
            return defaultValue
        } else {
            return getUserDefaults(did: did).bool(forKey: key)
        }
    }

    /**
     * Checks if a password database exists (master password was set). If not, starts the master password
     * creation flow. After completion, calls the listener so that the base flow can continue.
     */
    private func checkMasterPasswordCreationRequired(did: String,
                                                     onMasterPasswordCreated: @escaping ()->Void,
                                                     onCancel: @escaping ()->Void,
                                                     onError: @escaping (_ error: String)->Void) {
        if (databaseExists(did: did)) {
            onMasterPasswordCreated()
        }
        else {
           let creatorController = MasterPasswordCreatorAlertController(nibName: "MasterPasswordCreator", bundle: Bundle.main)

            let popup = PopupDialog(viewController: creatorController, buttonAlignment: .horizontal, transitionStyle: .fadeIn, preferredWidth: 340, tapGestureDismissal: false, panGestureDismissal: false, hideStatusBar: false, completion: nil)

            popup.view.backgroundColor = UIColor.clear // For rounded corners

            self.viewController!.present(popup, animated: false, completion: nil)

            creatorController.setOnPasswordCreatedListener { password in
                popup.dismiss()

                // Master password was provided and confirmed. Now we can use it.

                // Create an empty database
                self.createEmptyDatabase(did: did, masterPassword: password)

                do {
                    // Save this empty database to remember that we have defined a master password
                    try self.encryptAndSaveDatabase(did: did, masterPassword: password)

                    onMasterPasswordCreated()
                }
                catch (let error) {
                    onError(error.localizedDescription)
                }
            }

            creatorController.setOnCancelListener {
                popup.dismiss()
                onCancel()
            }
        }
    }

    public func isBiometricAuthEnabled(did: String) -> Bool {
        return getPrefsBool(did: did, key: "biometricauth", defaultValue: false)
    }

    public func setBiometricAuthEnabled(did: String, _ useBiometricAuth: Bool) {
        saveToPrefs(did: did, key: "biometricauth", value: useBiometricAuth)
    }
}
