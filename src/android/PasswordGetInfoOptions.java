package org.elastos.essentials.plugins.passwordmanager;

import org.json.JSONObject;

public class PasswordGetInfoOptions {
    public boolean promptPasswordIfLocked = true;
    public boolean forceMasterPasswordPrompt = false;

    public PasswordGetInfoOptions() {
    }

    public static PasswordGetInfoOptions fromJsonObject(JSONObject jsonObject) throws Exception {
        PasswordGetInfoOptions options = new PasswordGetInfoOptions();

        if (jsonObject.has("promptPasswordIfLocked"))
            options.promptPasswordIfLocked = jsonObject.getBoolean("promptPasswordIfLocked");

        if (jsonObject.has("forceMasterPasswordPrompt"))
            options.forceMasterPasswordPrompt = jsonObject.getBoolean("forceMasterPasswordPrompt");

        return options;
    }
}