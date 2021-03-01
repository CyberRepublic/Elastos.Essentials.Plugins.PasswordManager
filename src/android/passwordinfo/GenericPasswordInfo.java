package org.elastos.essentials.plugins.passwordmanager.passwordinfo;

import org.json.JSONException;
import org.json.JSONObject;

public class GenericPasswordInfo extends PasswordInfo {
    public String password = null;

    public static PasswordInfo fromJsonObject(JSONObject jsonObject) throws Exception {
        GenericPasswordInfo info = new GenericPasswordInfo();

        info.fillWithJsonObject(jsonObject);

        return info;
    }

    @Override
    public JSONObject asJsonObject() {
        try {
            JSONObject jsonObject = super.asJsonObject();

            jsonObject.put("password", password);

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
        if (jsonObject.has("password")) {
            this.password = jsonObject.getString("password");
        }
    }
}
