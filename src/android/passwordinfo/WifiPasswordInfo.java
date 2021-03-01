package org.elastos.essentials.plugins.passwordmanager.passwordinfo;

import org.json.JSONException;
import org.json.JSONObject;

public class WifiPasswordInfo extends PasswordInfo {
    String wifiSSID = null;
    String wifiPassword = null;

    public static PasswordInfo fromJsonObject(JSONObject jsonObject) throws Exception {
        WifiPasswordInfo info = new WifiPasswordInfo();

        info.fillWithJsonObject(jsonObject);

        return info;
    }

    @Override
    public JSONObject asJsonObject() {
        try {
            JSONObject jsonObject = super.asJsonObject();

            jsonObject.put("wifiSSID", wifiSSID);
            jsonObject.put("wifiPassword", wifiPassword);

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
        if (jsonObject.has("wifiSSID")) {
            this.wifiSSID = jsonObject.getString("wifiSSID");
        }
        if (jsonObject.has("wifiPassword")) {
            this.wifiPassword = jsonObject.getString("wifiPassword");
        }
    }
}
