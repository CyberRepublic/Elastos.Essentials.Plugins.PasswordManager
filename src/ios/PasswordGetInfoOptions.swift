//
//  PasswordGetInfoOptions.swift
//  elastOS
//
//  Created by Benjamin Piette on 24/07/2020.
//

import Foundation

public class PasswordGetInfoOptions {
    public var promptPasswordIfLocked = true
    public var forceMasterPasswordPrompt = false

    public static func fromDictionary(_ dict: Dictionary<String, Any>) -> PasswordGetInfoOptions {
        let options = PasswordGetInfoOptions()

        if dict.keys.contains("promptPasswordIfLocked") {
            options.promptPasswordIfLocked = dict["promptPasswordIfLocked"] as? Bool ?? false
        }

        if dict.keys.contains("forceMasterPasswordPrompt") {
            options.forceMasterPasswordPrompt = dict["forceMasterPasswordPrompt"] as? Bool ?? false
        }

        return options
    }
}
