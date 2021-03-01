package org.elastos.essentials.plugins.passwordmanager;

import org.elastos.essentials.plugins.passwordmanager.passwordinfo.PasswordInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

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
    private static final String APPLICATIONS_KEY = "applications";
    private static final String PASSWORD_ENTRIES_KEY = "passwordentries";
    JSONObject rawJson;
    String activeMasterPassword = null;
    Date openingTime = null;

    private PasswordDatabaseInfo() {
        openingTime = new Date();
    }

    static PasswordDatabaseInfo createEmpty() {
        try {
            PasswordDatabaseInfo info = new PasswordDatabaseInfo();
            JSONObject applications = new JSONObject();
            info.rawJson = new JSONObject();
            info.rawJson.put(APPLICATIONS_KEY, applications);
            return info;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PasswordDatabaseInfo fromJson(String json) throws JSONException {
        PasswordDatabaseInfo info = new PasswordDatabaseInfo();
        info.rawJson = new JSONObject(json);
        return info;
    }

    public PasswordInfo getPasswordInfo(String appID, String key) throws Exception {
        JSONObject appIDContent = getAppIDContent(appID);
        if (appIDContent == null) {
            // No entry for this app ID yet, so we can't find the requested key
            return null;
        }

        JSONArray passwordEntries = appIDContent.getJSONArray(PASSWORD_ENTRIES_KEY);
        JSONObject entry = passwordEntry(passwordEntries, key);
        if (entry == null) {
            // No such entry exists
            return null;
        }

        PasswordInfo info = PasswordInfoBuilder.buildFromType(entry);
        if (info != null) {
            info.appID = appID;
        }
        return info;
    }

    public void setPasswordInfo(String appID, PasswordInfo info) throws JSONException {
        JSONObject appIDContent = getAppIDContent(appID);
        if (appIDContent == null) {
            // No entry for this app ID yet, create one and add it
            appIDContent = createdEmptyAppIDContent();
            JSONObject applications = rawJson.getJSONObject(APPLICATIONS_KEY);
            applications.put(appID, appIDContent);
        }

        JSONArray passwordEntries = appIDContent.getJSONArray(PASSWORD_ENTRIES_KEY);
        if (keyInPasswordEntries(passwordEntries, info.key)) {
            // This entry already exists. Delete it first before re-adding its updated version.
            deletePasswordEntryFromKey(passwordEntries, info.key);
        }
        addPasswordEntry(passwordEntries, info);
    }

    public ArrayList<PasswordInfo> getAllPasswordInfo() throws Exception {
        JSONObject applications = rawJson.getJSONObject(APPLICATIONS_KEY);

        ArrayList<PasswordInfo> infos = new ArrayList<>();
        Iterator<String> it = applications.keys();
        while (it.hasNext()) {
            String appID = it.next();
            JSONObject appIDContent = getAppIDContent(appID);
            if (appIDContent != null) {
                JSONArray passwordEntries = appIDContent.getJSONArray(PASSWORD_ENTRIES_KEY);
                for (int i=0; i<passwordEntries.length(); i++) {
                    JSONObject entry = passwordEntries.getJSONObject(i);
                    PasswordInfo info = PasswordInfoBuilder.buildFromType(entry);
                    if (info != null) {
                        info.appID = appID;
                        infos.add(info);
                    }
                }
            }
        }
        return infos;
    }

    public void deletePasswordInfo(String appID, String key) throws JSONException {
        JSONObject appIDContent = getAppIDContent(appID);
        if (appIDContent == null) {
            // No entry for this app ID yet, so we can't find the requested key
            return;
        }

        JSONArray passwordEntries = appIDContent.getJSONArray(PASSWORD_ENTRIES_KEY);
        deletePasswordEntryFromKey(passwordEntries, key);
    }

    private JSONObject getAppIDContent(String appID) throws JSONException {
        JSONObject applications = rawJson.getJSONObject(APPLICATIONS_KEY);
        if (applications.has(appID)) {
            return applications.getJSONObject(appID);
        }
        else {
            return null;
        }
    }

    private JSONObject createdEmptyAppIDContent() throws JSONException {
        JSONObject appIDContent = new JSONObject();
        appIDContent.put(PASSWORD_ENTRIES_KEY, new JSONArray());
        return appIDContent;
    }

    private JSONObject passwordEntry(JSONArray entries, String key) throws JSONException {
        for (int i=0; i<entries.length(); i++) {
            JSONObject info = entries.getJSONObject(i);
            if (info.getString("key").equals(key))
                return info;
        }
        return null;
    }

    private int passwordEntryIndex(JSONArray entries, String key) throws JSONException {
        for (int i=0; i<entries.length(); i++) {
            JSONObject info = entries.getJSONObject(i);
            if (info.getString("key").equals(key))
                return i;
        }
        return -1;
    }

    private boolean keyInPasswordEntries(JSONArray entries, String key) throws JSONException {
        return passwordEntryIndex(entries, key) >= 0;
    }

    private void deletePasswordEntryFromKey(JSONArray entries, String key) throws JSONException {
        int deletionIndex = passwordEntryIndex(entries, key);
        if (deletionIndex >= 0)
            entries.remove(deletionIndex);
    }

    private void addPasswordEntry(JSONArray entries, PasswordInfo info) throws JSONException {
        JSONObject json = info.asJsonObject();
        if (json == null) {
            throw new JSONException("Unable to create JSON object from password info");
        }

        entries.put(json);
    }

    /**
     * Closes the password database and makes things secure.
     */
    void lock() {
        rawJson = null;
        activeMasterPassword = null;
        // NOTE: nothing else to do for now.
    }
}