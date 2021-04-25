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

public class BankAccountPasswordInfo : PasswordInfo {
    var accountOwner: String? = nil
    var iban: String? = nil
    var swift: String? = nil
    var bic: String? = nil

    public static func fromJsonObject(_ jsonObject: Dictionary<String, Any>) throws -> PasswordInfo {
        let info = BankAccountPasswordInfo()

        try info.fillWithJsonObject(jsonObject)

        return info;
    }

    public override func asDictionary() -> Dictionary<String, Any>? {
        if var jsonObject = super.asDictionary() {
            jsonObject["accountOwner"] = accountOwner
            jsonObject["iban"] = iban
            jsonObject["swift"] = swift
            jsonObject["bic"] = bic

            return jsonObject
        }

        return nil
    }

    public override func fillWithJsonObject(_ jsonObject: Dictionary<String, Any>) throws {
        // Fill base fields
        try super.fillWithJsonObject(jsonObject)

        // Fill specific fields
        if (jsonObject.keys.contains("accountOwner")) {
            self.accountOwner = jsonObject["accountOwner"] as? String
        }
        if (jsonObject.keys.contains("iban")) {
            self.iban = jsonObject["iban"]  as? String
        }
        if (jsonObject.keys.contains("swift")) {
            self.swift = jsonObject["swift"]  as? String
        }
        if (jsonObject.keys.contains("bic")) {
            self.bic = jsonObject["bic"]  as? String
        }
    }
}
