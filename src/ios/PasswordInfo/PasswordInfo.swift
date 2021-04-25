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

/**
 * Root type for all password information. This type is abstract and should not be used
 * directly.
 */
public class PasswordInfo {
    /**
     * Unique key, used to identity the password info among other.
     */
    public var key: String = ""

    /**
     * Password type, that defines the format of contained information.
     */
    public var type: PasswordType = .GENERIC_PASSWORD

    /**
     * Name used while displaying this info. Either set by users in the password manager app
     * or by apps, when saving passwords automatically.
     */
    public var displayName: String = ""

    /**
     * Package ID of the application/capsule that saved this password information.
     * READ-ONLY
     */
    public var appID: String? = nil

    /**
     * List of any kind of app-specific additional information for this password entry.
     */
    public var custom: Dictionary<String, Any>? = nil

    public func asDictionary() -> Dictionary<String, Any>? {
        var jsonObj = Dictionary<String, Any>()
        jsonObj["key"] = key
        jsonObj["type"] = type.rawValue
        jsonObj["displayName"] = displayName
        jsonObj["custom"] = custom
        jsonObj["appID"] = appID
        return jsonObj
    }

    public func fillWithJsonObject(_ jsonObj: Dictionary<String, Any>) throws {
        if !jsonObj.keys.contains("key") || !jsonObj.keys.contains("type") || !jsonObj.keys.contains("displayName") {
            throw "Invalid password info, some base fields are missing"
        }

        self.key = jsonObj["key"] as! String
        self.type = PasswordType.init(rawValue: jsonObj["type"] as! Int)!
        self.displayName = jsonObj["displayName"] as! String

        if jsonObj.keys.contains("custom") {
            self.custom = jsonObj["custom"] as? Dictionary<String, Any>
        }

        // SECURITY NOTE - Don't fill with appID. AppID is filled automatically.
    }
}
