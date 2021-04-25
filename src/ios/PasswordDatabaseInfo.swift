/*
* Copyright (c) 2020 Elastos Foundation
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

private class PasswordDatabaseInfoApplication {
    private let appID: String
    var passwordEntries = Array<PasswordInfo>()
    
    init(appID: String) {
        self.appID = appID
    }
    
    public static func fromDictionary(_ dict: Dictionary<String, Any>, appID: String) throws -> PasswordDatabaseInfoApplication {
        let app = PasswordDatabaseInfoApplication(appID: appID)
        
        guard let entriesDict = dict["passwordentries"] as? Array<Dictionary<String, Any>> else {
            throw "Invalid dictionary for password database. No passwordentries field"
        }
        
        for entryDict in entriesDict {
            let passwordInfo = try PasswordInfoBuilder.buildFromType(jsonObject: entryDict)
            passwordInfo.appID = appID
            app.passwordEntries.append(passwordInfo)
        }
        
        return app
    }
        
    public func asDictionary() -> Dictionary<String, Any> {
        var dict = Dictionary<String, Any>()
        var passwordEntriesArray = Array<Dictionary<String, Any>>()
        for entry in passwordEntries {
            passwordEntriesArray.append(entry.asDictionary()!)
        }
        dict["passwordentries"] = passwordEntriesArray

        return dict
    }
    
    fileprivate func deletePasswordEntryFromKey(key: String) throws {
        let deletionIndex = try passwordEntryIndex(key: key)
        if deletionIndex >= 0 {
            passwordEntries.remove(at: deletionIndex)
        }
    }

    fileprivate func addPasswordEntry(info: PasswordInfo) {
        passwordEntries.append(info)
    }
    
    fileprivate func passwordEntry(key: String) throws -> PasswordInfo {
        guard let info = passwordEntries.first(where: { info in
            info.key == key
        }) else {
            throw "No password info found for key \(key)"
        }
        
        // READ-ONLY
        info.appID = appID
        
        return info
    }

    fileprivate func passwordEntryIndex(key: String) throws -> Int {
        guard let index = passwordEntries.firstIndex(where: { info in
            info.key == key
        }) else {
            throw "No password info found for key \(key)"
        }
        return index
    }

    fileprivate func keyInPasswordEntries(key: String) -> Bool {
        return ((try? passwordEntryIndex(key: key)) ?? -1) >= 0
    }
}

/**
 * Database JSON format:
 *
 * {
 *     "applications": {
 *          "APPIID1": {
 *              "passwordentries": [
 *                  {
 *                      RAW_USER_OBJECT
 *                  }
 *              ]
 *          }
 *     }
 * }
 *
 * We work directly with raw JSONObjects to make it easier later to maintain the structure, add new fields,
 * handle specific or missing items.
 */
class PasswordDatabaseInfo {
    fileprivate var applications = Dictionary<String, PasswordDatabaseInfoApplication>()
    //var rawJson: Dictionary<String, JSONObject>?
    var activeMasterPassword: String? = nil
    var openingTime: Date

    private init() {
        openingTime = Date()
    }

    static func createEmpty() -> PasswordDatabaseInfo {
        return PasswordDatabaseInfo()
    }

    public static func fromDictionary(_ dict: Dictionary<String, Any>) throws -> PasswordDatabaseInfo {
        let info = PasswordDatabaseInfo()
        
        guard let appsDict = dict["applications"] as? Dictionary<String, Dictionary<String, Any>> else {
            throw "Invalid dictionary for password database. No applications field"
        }
        
        for appDict in appsDict {
            let app = try PasswordDatabaseInfoApplication.fromDictionary(appDict.value, appID: appDict.key)
            info.applications[appDict.key] = app
        }
        
        return info
    }
    
    public func asDictionary() -> Dictionary<String, Any> {
        var dict = Dictionary<String, Any>()
        var applicationsDict = Dictionary<String, Any>()
        for app in applications {
            applicationsDict[app.key] = app.value.asDictionary()
        }
        dict["applications"] = applicationsDict
        
        return dict
    }

    public func getPasswordInfo(appID: String, key: String) throws -> PasswordInfo? {
        if let appIDContent = try getAppIDContent(appID) {
            let entry = (try? appIDContent.passwordEntry(key: key)) ?? nil
            if (entry != nil) {
                entry?.appID = appID
            }
            return entry
        }
        else {
            // No entry for this app ID yet, so we can't find the requested key
            return nil
        }
    }

    public func setPasswordInfo(appID: String, info: PasswordInfo) throws {
        var appIDContent = try getAppIDContent(appID)
        if (appIDContent == nil) {
            // No entry for this app ID yet, create one and add it
            appIDContent = PasswordDatabaseInfoApplication(appID: appID)
            applications[appID] = appIDContent
        }

        if (appIDContent!.keyInPasswordEntries(key: info.key)) {
            // This entry already exists. Delete it first before re-adding its updated version.
            try appIDContent!.deletePasswordEntryFromKey(key: info.key)
        }
        appIDContent!.addPasswordEntry(info: info)
    }

    public func getAllPasswordInfo() throws -> [PasswordInfo] {
        var infos = [PasswordInfo]()
        var it = applications.keys.makeIterator()
        while let appID = it.next() {
            if let appIDContent = try getAppIDContent(appID) {
                for entry in appIDContent.passwordEntries {
                    entry.appID = appID
                    infos.append(entry)
                }
            }
        }
        return infos
    }

    public func deletePasswordInfo(appID: String, key: String) throws {
        if let appIDContent = try getAppIDContent(appID) {
            try appIDContent.deletePasswordEntryFromKey(key: key)
        }
        else {
            // No entry for this app ID yet, so we can't find the requested key
        }
    }

    private func getAppIDContent(_ appID: String) throws -> PasswordDatabaseInfoApplication? {
        if (applications.keys.contains(appID)) {
            return applications[appID]
        }
        else {
            return nil
        }
    }
    
    /**
     * Closes the password database and makes things secure.
     */
    func lock() {
        applications.removeAll()
        activeMasterPassword = nil
        // NOTE: nothing else to do for now.
    }
}
