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

package org.elastos.essentials.plugins.passwordmanager;

import android.app.Activity;
import android.content.res.Configuration;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.elastos.essentials.plugins.passwordmanager.passwordinfo.PasswordInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class PasswordManagerPlugin extends CordovaPlugin {
    private static final int NATIVE_ERROR_CODE_INVALID_PASSWORD = -1;
    private static final int NATIVE_ERROR_CODE_INVALID_PARAMETER = -2;
    private static final int NATIVE_ERROR_CODE_CANCELLED = -3;
    private static final int NATIVE_ERROR_CODE_UNSPECIFIED = -4;

    private Activity activity;

    public class BooleanWithReason {
        public boolean value;
        public String reason;

        BooleanWithReason(boolean value, String reason) {
            this.value = value;
            this.reason = reason;
        }
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        this.activity = this.cordova.getActivity();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            switch (action) {
                case "setPasswordInfo":
                    this.setPasswordInfo(args, callbackContext);
                    break;
                case "getPasswordInfo":
                    this.getPasswordInfo(args, callbackContext);
                    break;
                case "getAllPasswordInfo":
                    this.getAllPasswordInfo(args, callbackContext);
                    break;
                case "deletePasswordInfo":
                    this.deletePasswordInfo(args, callbackContext);
                    break;
                case "deleteAppPasswordInfo":
                    this.deleteAppPasswordInfo(args, callbackContext);
                    break;
                case "generateRandomPassword":
                    this.generateRandomPassword(args, callbackContext);
                    break;
                case "changeMasterPassword":
                    this.changeMasterPassword(args, callbackContext);
                    break;
                case "lockMasterPassword":
                    this.lockMasterPassword(args, callbackContext);
                    break;
                case "deleteAll":
                    this.deleteAll(args, callbackContext);
                    break;
                case "setUnlockMode":
                    this.setUnlockMode(args, callbackContext);
                    break;
                case "setVirtualDIDContext":
                    this.setVirtualDIDContext(args, callbackContext);
                    break;
                case "setCurrentDID":
                    this.setCurrentDID(args, callbackContext);
                    break;
                case "setDarkMode":
                    this.setDarkMode(args, callbackContext);
                    break;
                case "setLanguage":
                    this.setLanguage(args, callbackContext);
                    break;
                default:
                    return false;
            }
        }
        catch (Exception e) {
            callbackContext.error(e.getLocalizedMessage());
        }
        return true;
    }

    private void sendSuccess(CallbackContext callbackContext, JSONObject jsonObj) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, jsonObj));
    }

    private void sendError(CallbackContext callbackContext, JSONObject jsonObj) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, jsonObj));
    }

    private void sendError(CallbackContext callbackContext, String method, String message) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, method+": "+message));
    }

    private JSONObject buildCancellationError() {
        try {
            JSONObject result = new JSONObject();
            result.put("code", NATIVE_ERROR_CODE_CANCELLED);
            result.put("reason", "MasterPasswordCancellation");
            return result;
        }
        catch (Exception e) {
            return null;
        }
    }

    private JSONObject buildGenericError(String error) {
        try {
            JSONObject result = new JSONObject();
            if (error.contains("BAD_DECRYPT") || error.contains("Authentication failed") || error.contains("Authentication error"))
                result.put("code", NATIVE_ERROR_CODE_INVALID_PASSWORD);
            else
                result.put("code", NATIVE_ERROR_CODE_UNSPECIFIED);
            result.put("reason", error);
            return result;
        }
        catch (Exception e) {
            return null;
        }
    }

    private void setPasswordInfo(JSONArray args, CallbackContext callbackContext) throws Exception {
        JSONObject info = args.getJSONObject(0);

        PasswordInfo passwordInfo = PasswordInfoBuilder.buildFromType(info);
        if (passwordInfo == null) {
            sendError(callbackContext, "setPasswordInfo", "Invalid JSON object for password info");
            return;
        }

        JSONObject result = new JSONObject();
        PasswordManager.getSharedInstance(this.activity).setPasswordInfo(passwordInfo, null, "", new PasswordManager.OnPasswordInfoSetListener(){
            @Override
            public void onPasswordInfoSet() {
                try {
                    result.put("couldSet", true);
                }
                catch (JSONException ignored) {}
                sendSuccess(callbackContext, result);
            }

            @Override
            public void onCancel() {
                sendError(callbackContext, buildCancellationError());
            }

            @Override
            public void onError(String error) {
                sendError(callbackContext, buildGenericError(error));
            }
        });
    }

    private void getPasswordInfo(JSONArray args, CallbackContext callbackContext) throws Exception {
        String key = args.getString(0);
        JSONObject optionsJson = args.isNull(1) ? null : args.getJSONObject(1);
        PasswordGetInfoOptions options = null;

        try {
            if (optionsJson != null) {
                options = PasswordGetInfoOptions.fromJsonObject(optionsJson);
            }
        }
        catch (Exception e) {
            // Invalid options passed? We'll use default options
        }

        if (options == null) {
            options = new PasswordGetInfoOptions(); // default options
        }

        JSONObject result = new JSONObject();
        PasswordManager.getSharedInstance(this.activity).getPasswordInfo(key, null, "", options, new PasswordManager.OnPasswordInfoRetrievedListener() {
            @Override
            public void onPasswordInfoRetrieved(PasswordInfo info) {
                try {
                    if (info != null)
                        result.put("passwordInfo", info.asJsonObject());
                    else
                        result.put("passwordInfo", null);
                }
                catch (JSONException ignored) {}
                sendSuccess(callbackContext, result);
            }

            @Override
            public void onCancel() {
                sendError(callbackContext, buildCancellationError());
            }

            @Override
            public void onError(String error) {
                sendError(callbackContext, buildGenericError(error));
            }
        });
    }

    private void getAllPasswordInfo(JSONArray args, CallbackContext callbackContext) throws Exception {
        JSONObject result = new JSONObject();
        PasswordManager.getSharedInstance(this.activity).getAllPasswordInfo(null, "", new PasswordManager.OnAllPasswordInfoRetrievedListener() {
            @Override
            public void onAllPasswordInfoRetrieved(ArrayList<PasswordInfo> infos) {
                try {
                    JSONArray allPasswordInfo = new JSONArray();
                    for (PasswordInfo info : infos) {
                        allPasswordInfo.put(info.asJsonObject());
                    }

                    result.put("allPasswordInfo", allPasswordInfo);

                    sendSuccess(callbackContext, result);
                }
                catch (Exception e) {
                    sendError(callbackContext, "getAllPasswordInfo", e.getMessage());
                }
            }

            @Override
            public void onCancel() {
                sendError(callbackContext, buildCancellationError());
            }

            @Override
            public void onError(String error) {
                sendError(callbackContext, buildGenericError(error));
            }
        });
    }

    private void deletePasswordInfo(JSONArray args, CallbackContext callbackContext) throws Exception {
        String key = args.getString(0);

        JSONObject result = new JSONObject();
        PasswordManager.getSharedInstance(this.activity).deletePasswordInfo(key, null, "", "", new PasswordManager.OnPasswordInfoDeletedListener() {
            @Override
            public void onPasswordInfoDeleted() {
                try {
                    result.put("couldDelete", true);
                }
                catch (JSONException ignored) {}
                sendSuccess(callbackContext, result);
            }

            @Override
            public void onCancel() {
                sendError(callbackContext, buildCancellationError());
            }

            @Override
            public void onError(String error) {
                sendError(callbackContext, buildGenericError(error));
            }
        });
    }

    private void deleteAppPasswordInfo(JSONArray args, CallbackContext callbackContext) throws Exception {
        String targetAppID = args.getString(0);
        String key = args.getString(1);

        JSONObject result = new JSONObject();
        PasswordManager.getSharedInstance(this.activity).deletePasswordInfo(key, null, "", targetAppID, new PasswordManager.OnPasswordInfoDeletedListener() {
            @Override
            public void onPasswordInfoDeleted() {
                try {
                    result.put("couldDelete", true);
                }
                catch (JSONException ignored) {}
                sendSuccess(callbackContext, result);
            }

            @Override
            public void onCancel() {
                sendError(callbackContext, buildCancellationError());
            }

            @Override
            public void onError(String error) {
                sendError(callbackContext, buildGenericError(error));
            }
        });
    }

    private void generateRandomPassword(JSONArray args, CallbackContext callbackContext) throws Exception {
        JSONObject options = args.isNull(0) ? null : args.getJSONObject(0); // Currently unused

        String password = PasswordManager.getSharedInstance(this.activity).generateRandomPassword(null);

        JSONObject result = new JSONObject();
        result.put("generatedPassword", password);

        sendSuccess(callbackContext, result);
    }

    private void changeMasterPassword(JSONArray args, CallbackContext callbackContext) throws Exception {
        JSONObject result = new JSONObject();

        PasswordManager.getSharedInstance(this.activity).changeMasterPassword(null, "", new PasswordManager.OnMasterPasswordChangeListener() {
            @Override
            public void onMasterPasswordChanged() {
                try {
                    result.put("couldChange", true);
                }
                catch (JSONException ignored) {}
                sendSuccess(callbackContext, result);
            }

            @Override
            public void onCancel() {
                sendError(callbackContext, buildCancellationError());
            }

            @Override
            public void onError(String error) {
                sendError(callbackContext, buildGenericError(error));
            }
        });
    }

    private void lockMasterPassword(JSONArray args, CallbackContext callbackContext) throws Exception {
        PasswordManager.getSharedInstance(this.activity).lockMasterPassword(null);

        JSONObject result = new JSONObject();
        sendSuccess(callbackContext, result);
    }

    private void deleteAll(JSONArray args, CallbackContext callbackContext) throws Exception {
        PasswordManager.getSharedInstance(this.activity).deleteAll(null);

        JSONObject result = new JSONObject();
        sendSuccess(callbackContext, result);
    }

    private void setUnlockMode(JSONArray args, CallbackContext callbackContext) throws Exception {
        int unlockModeAsInt = args.getInt(0);

        PasswordUnlockMode unlockMode = PasswordUnlockMode.fromValue(unlockModeAsInt);

        PasswordManager.getSharedInstance(this.activity).setUnlockMode(unlockMode, null, "");

        JSONObject result = new JSONObject();
        sendSuccess(callbackContext, result);
    }

    private void setVirtualDIDContext(JSONArray args, CallbackContext callbackContext) throws Exception {
        String virtualDIDStringContext = args.isNull(0) ? null : args.getString(0);

        PasswordManager.getSharedInstance(this.activity).setVirtualDIDContext(virtualDIDStringContext);

        JSONObject result = new JSONObject();
        sendSuccess(callbackContext, result);
    }

    private void setCurrentDID(JSONArray args, CallbackContext callbackContext) throws Exception {
        String did = args.isNull(0) ? null : args.getString(0);

        PasswordManager.getSharedInstance(this.activity).setDID(did);

        JSONObject result = new JSONObject();
        sendSuccess(callbackContext, result);
    }

    private void setDarkMode(JSONArray args, CallbackContext callbackContext) throws Exception {
        boolean useDarkMode = args.isNull(0) ? false : args.getBoolean(0);

        UIStyling.prepare(useDarkMode);

        JSONObject result = new JSONObject();
        sendSuccess(callbackContext, result);
    }

    private void setLanguage(JSONArray args, CallbackContext callbackContext) throws Exception {
        String language = args.isNull(0) ? null : args.getString(0);
        if (language == null) {
            sendError(callbackContext, "setLanguage", "Invalid language");
            return;
        }

        Configuration config = this.activity.getResources().getConfiguration();
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        config.locale = locale;
        this.activity.getResources().updateConfiguration(config, this.activity.getResources().getDisplayMetrics());

        JSONObject result = new JSONObject();
        sendSuccess(callbackContext, result);
    }
}
