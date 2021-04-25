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

@objc(PasswordManagerPlugin)
class PasswordManagerPlugin : CDVPlugin {
    private static let NATIVE_ERROR_CODE_INVALID_PASSWORD = -1
    private static let NATIVE_ERROR_CODE_INVALID_PARAMETER = -2
    private static let NATIVE_ERROR_CODE_CANCELLED = -3
    private static let NATIVE_ERROR_CODE_UNSPECIFIED = -4

    override func pluginInitialize() {
        PasswordManager.getSharedInstance().setViewController(viewController as! CDVViewController);
    }

    func success(_ command: CDVInvokedUrlCommand) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK)
        self.commandDelegate.send(result, callbackId: command.callbackId)
    }

    func success(_ command: CDVInvokedUrlCommand, _ retAsDict: Dictionary<String, Any>) {
        let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: retAsDict)
        self.commandDelegate.send(result, callbackId: command.callbackId)
    }

    func error(_ command: CDVInvokedUrlCommand, _ retAsString: String) {
        let result = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                     messageAs: retAsString)

        self.commandDelegate.send(result, callbackId: command.callbackId)
    }

    func error(_ command: CDVInvokedUrlCommand, _ retAsDict: Dictionary<String, Any>) {
        let result = CDVPluginResult(status: CDVCommandStatus_ERROR,
                                     messageAs: retAsDict)

        self.commandDelegate.send(result, callbackId: command.callbackId)
    }

    private func buildCancellationError() -> Dictionary<String, Any> {
        var result = Dictionary<String, Any>()
        result["code"] = PasswordManagerPlugin.NATIVE_ERROR_CODE_CANCELLED
        result["reason"] = "MasterPasswordCancellation"
        return result
    }

    private func buildGenericError(message: String) -> Dictionary<String, Any>{
        var result = Dictionary<String, Any>()
        if message.contains("BAD_DECRYPT") { // TODO: not like android!
            result["code"] = PasswordManagerPlugin.NATIVE_ERROR_CODE_INVALID_PASSWORD
        }
        else {
            result["code"] = PasswordManagerPlugin.NATIVE_ERROR_CODE_UNSPECIFIED
        }
        result["reason"] = message
        return result;
    }

    @objc public func setPasswordInfo(_ command: CDVInvokedUrlCommand) {
        do {
            if let info = command.arguments[0] as? Dictionary<String, Any> {
                let passwordInfo = try PasswordInfoBuilder.buildFromType(jsonObject: info)

                var result = Dictionary<String, Any>()
                try PasswordManager.getSharedInstance().setPasswordInfo(info: passwordInfo, did: "", appID: "", onPasswordInfoSet: {

                    result["couldSet"] = true
                    self.success(command, result)

                }, onCancel: {
                    self.error(command, self.buildCancellationError())
                }, onError: { error in
                    self.error(command, self.buildGenericError(message: error))
                })
            }
            else {
                self.error(command, buildGenericError(message: "Password info must be provided"))
            }
        }
        catch (let error) {
            self.error(command, buildGenericError(message: error.localizedDescription))
        }
    }

    @objc public func getPasswordInfo(_ command: CDVInvokedUrlCommand) {
        do {
            if let key = command.arguments[0] as? String {
                let optionsJson = command.arguments[1] as? Dictionary<String, Any>
                var options: PasswordGetInfoOptions? = nil

                if optionsJson != nil {
                    options = PasswordGetInfoOptions.fromDictionary(optionsJson!)
                }

                if options == nil {
                    options = PasswordGetInfoOptions() // default options
                }

                var result = Dictionary<String, Any>()
                try PasswordManager.getSharedInstance().getPasswordInfo(key: key, did: "", appID: "", options: options!, onPasswordInfoRetrieved: { info in

                    if info != nil {
                        result["passwordInfo"] = info!.asDictionary()
                    }
                    else {
                        result["passwordInfo"] = nil
                    }
                    self.success(command, result)

                }, onCancel: {
                    self.error(command, self.buildCancellationError())
                }, onError: { error in
                    self.error(command, self.buildGenericError(message: error))
                })
            }
            else {
                self.error(command, buildGenericError(message: "Password info key must be provided"))
            }
        }
        catch (let error) {
            self.error(command, buildGenericError(message: error.localizedDescription))
        }
    }

    @objc public func getAllPasswordInfo(_ command: CDVInvokedUrlCommand) {
        var result = Dictionary<String, Any>()
        do {
            try PasswordManager.getSharedInstance().getAllPasswordInfo(did: "", appID: "", onAllPasswordInfoRetrieved: { infos in

                var allPasswordInfo = Array<Dictionary<String, Any>>()
                for info in infos {
                    if let jsonInfo = info.asDictionary() {
                        allPasswordInfo.append(jsonInfo)
                    }
                }

                result["allPasswordInfo"] = allPasswordInfo

                self.success(command, result)

            }, onCancel: {
                self.error(command, self.buildCancellationError())
            }, onError: { error in
                self.error(command, self.buildGenericError(message: error))
            })
        }
        catch (let error) {
            self.error(command, buildGenericError(message: error.localizedDescription))
        }
    }

    @objc public func deletePasswordInfo(_ command: CDVInvokedUrlCommand) {
        do {
            if let key = command.arguments[0] as? String {
                var result = Dictionary<String, Any>()
                try PasswordManager.getSharedInstance().deletePasswordInfo(key: key, did: "", appID: "", targetAppID: "", onPasswordInfoDeleted: {

                    result["couldDelete"] = true
                    self.success(command, result)

                }, onCancel: {
                    self.error(command, self.buildCancellationError())
                }, onError: { error in
                    self.error(command, self.buildGenericError(message: error))
                })
            }
            else {
                self.error(command, buildGenericError(message: "Password info key must be provided"))
            }
        }
        catch (let error) {
            self.error(command, buildGenericError(message: error.localizedDescription))
        }
    }

    @objc public func deleteAppPasswordInfo(_ command: CDVInvokedUrlCommand) {
        do {
            guard let targetAppId = command.arguments[0] as? String else {
                self.error(command, buildGenericError(message: "Target app id must be provided"))
                return
            }

            guard let key = command.arguments[1] as? String else {
                self.error(command, buildGenericError(message: "Password info key must be provided"))
                return
            }

            var result = Dictionary<String, Any>()
            try PasswordManager.getSharedInstance().deletePasswordInfo(key: key, did: "", appID: "", targetAppID: targetAppId, onPasswordInfoDeleted: {

                result["couldDelete"] = true
                self.success(command, result)

            }, onCancel: {
                self.error(command, self.buildCancellationError())
            }, onError: { error in
                self.error(command, self.buildGenericError(message: error))
            })
        }
        catch (let error) {
            self.error(command, buildGenericError(message: error.localizedDescription))
        }
    }

    @objc public func generateRandomPassword(_ command: CDVInvokedUrlCommand) {
        let _ = command.arguments[0] as? Dictionary<String, Any> // Options - currently unused

        let password = PasswordManager.getSharedInstance().generateRandomPassword(options: nil)

        var result = Dictionary<String, Any>()
        result["generatedPassword"] = password

        self.success(command, result)
    }

    @objc public func changeMasterPassword(_ command: CDVInvokedUrlCommand) {
        do {
            var result = Dictionary<String, Any>()
            try PasswordManager.getSharedInstance().changeMasterPassword(did: "", appID: "", onMasterPasswordChanged: {

                result["couldChange"] = true
                self.success(command, result)

            }, onCancel: {
                self.error(command, self.buildCancellationError())
            }, onError: { error in
                self.error(command, self.buildGenericError(message: error))
            })
        }
        catch (let error) {
            self.error(command, buildGenericError(message: error.localizedDescription))
        }
    }

    @objc public func lockMasterPassword(_ command: CDVInvokedUrlCommand) {
        do {
            try PasswordManager.getSharedInstance().lockMasterPassword(did: "")

            let result = Dictionary<String, Any>()
            self.success(command, result)
        }
        catch (let error) {
            self.error(command, buildGenericError(message: error.localizedDescription))
        }
    }

    @objc public func deleteAll(_ command: CDVInvokedUrlCommand) {
        do {
            try PasswordManager.getSharedInstance().deleteAll(did: "")

            let result = Dictionary<String, Any>()
            self.success(command, result)
        }
        catch (let error) {
            self.error(command, buildGenericError(message: error.localizedDescription))
        }
    }

    @objc public func setUnlockMode(_ command: CDVInvokedUrlCommand) {
        guard let unlockModeAsInt = command.arguments[0] as? Int else {
            self.error(command, buildGenericError(message: "Unlock mode must be provided"))
            return
        }

        if let unlockMode = PasswordUnlockMode(rawValue: unlockModeAsInt) {
            do {
                try PasswordManager.getSharedInstance().setUnlockMode(unlockMode: unlockMode, did: "", appID: "")
            }
            catch (let error) {
                self.error(command, buildGenericError(message: error.localizedDescription))
            }
        }
        else {
            self.error(command, buildGenericError(message: "No known unlock mode for value \(unlockModeAsInt)"))
        }

        let result = Dictionary<String, Any>()
        self.success(command, result)
    }

    @objc public func setDarkMode(_ command: CDVInvokedUrlCommand) {
        guard let darkModeAsBool = command.arguments[0] as? Bool else {
            self.error(command, buildGenericError(message: "Dark mode must be provided"))
            return
        }


        UIStyling.prepare(useDarkMode: darkModeAsBool);

        let result = Dictionary<String, Any>()
        self.success(command, result)
    }

    @objc public func setLanguage(_ command: CDVInvokedUrlCommand) {
        guard let language = command.arguments[0] as? String else {
           self.error(command, buildGenericError(message: "Language must be provided"))
           return
        }

         UserDefaults.standard.set(language, forKey: "i18n_language")

        let result = Dictionary<String, Any>()
        self.success(command, result)
    }
}
