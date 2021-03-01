package org.elastos.essentials.plugins.passwordmanager.passwordinfo;

import org.json.JSONException;
import org.json.JSONObject;

public class AccountPasswordInfo extends PasswordInfo {
    String identifier = null;
    String password = null;
    String twoFactorKey = null;

    public static PasswordInfo fromJsonObject(JSONObject jsonObject) throws Exception {
        AccountPasswordInfo info = new AccountPasswordInfo();

        info.fillWithJsonObject(jsonObject);

        return info;
    }

    @Override
    public JSONObject asJsonObject() {
        try {
            JSONObject jsonObject = super.asJsonObject();

            jsonObject.put("identifier", identifier);
            jsonObject.put("password", password);
            jsonObject.put("twoFactorKey", twoFactorKey);

            return jsonObject;
        } catch (
                JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void fillWithJsonObject(JSONObject jsonObject) throws Exception {
        // Fill base fields
        super.fillWithJsonObject(jsonObject);

        // Fill specific fields
        if (jsonObject.has("identifier")) {
            this.identifier = jsonObject.getString("identifier");
        }
        if (jsonObject.has("password")) {
            this.password = jsonObject.getString("password");
        }
        if (jsonObject.has("twoFactorKey")) {
            this.twoFactorKey = jsonObject.getString("twoFactorKey");
        }
    }
}
