package org.elastos.essentials.plugins.passwordmanager.passwordinfo;

import org.elastos.essentials.plugins.passwordmanager.PasswordType;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Root type for all password information. This type is abstract and should not be used
 * directly.
 */
public abstract class PasswordInfo {
    /**
     * Unique key, used to identity the password info among other.
     */
    public String key;

    /**
     * Password type, that defines the format of contained information.
     */
    public PasswordType type;

    /**
     * Name used while displaying this info. Either set by users in the password manager app
     * or by apps, when saving passwords automatically.
     */
    public String displayName;

    /**
     * Package ID of the application/capsule that saved this password information.
     * READ-ONLY
     */
    public String appID;

    /**
     * List of any kind of app-specific additional information for this password entry.
     */
    public JSONObject custom;

    public JSONObject asJsonObject() {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("key", key);
            jsonObj.put("type", type.mValue);
            jsonObj.put("displayName", displayName);
            jsonObj.put("custom", custom);
            jsonObj.put("appID", appID);
            return jsonObj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void fillWithJsonObject(JSONObject jsonObj) throws Exception {
        if (!jsonObj.has("key") || !jsonObj.has("type") || !jsonObj.has("displayName"))
            throw new Exception("Invalid password info, some base fields are missing");

        this.key = jsonObj.getString("key");
        this.type = PasswordType.fromValue(jsonObj.getInt("type"));
        this.displayName = jsonObj.getString("displayName");

        if (jsonObj.has("custom")) {
            this.custom = jsonObj.getJSONObject("custom");
        }

        // SECURITY NOTE - Don't fill with appID. AppID is filled automatically.
    }
}