package org.elastos.essentials.plugins.passwordmanager.passwordinfo;

import org.json.JSONException;
import org.json.JSONObject;

public class BankAccountPasswordInfo extends PasswordInfo {
    String accountOwner = null;
    String iban = null;
    String swift = null;
    String bic = null;

    public static PasswordInfo fromJsonObject(JSONObject jsonObject) throws Exception {
        BankAccountPasswordInfo info = new BankAccountPasswordInfo();

        info.fillWithJsonObject(jsonObject);

        return info;
    }

    @Override
    public JSONObject asJsonObject() {
        try {
            JSONObject jsonObject = super.asJsonObject();

            jsonObject.put("accountOwner", accountOwner);
            jsonObject.put("iban", iban);
            jsonObject.put("swift", swift);
            jsonObject.put("bic", bic);

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
        if (jsonObject.has("accountOwner")) {
            this.accountOwner = jsonObject.getString("accountOwner");
        }
        if (jsonObject.has("iban")) {
            this.iban = jsonObject.getString("iban");
        }
        if (jsonObject.has("swift")) {
            this.swift = jsonObject.getString("swift");
        }
        if (jsonObject.has("bic")) {
            this.bic = jsonObject.getString("bic");
        }
    }
}
