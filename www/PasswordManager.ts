/*
* Copyright (c) 2018 Elastos Foundation
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

let exec = cordova.exec;

enum NativeErrorCode {
    NATIVE_ERROR_CODE_INVALID_PASSWORD = -1,
    NATIVE_ERROR_CODE_INVALID_PARAMETER = -2,
    NATIVE_ERROR_CODE_CANCELLED = -3,
    NATIVE_ERROR_CODE_UNSPECIFIED = -4,
}

class PasswordManagerImpl implements PasswordManagerPlugin.PasswordManager {
    setPasswordInfo(info: PasswordManagerPlugin.PasswordInfo): Promise<PasswordManagerPlugin.BooleanWithReason> {
        return new Promise((resolve, reject)=>{
            exec((result: { couldSet: boolean, reason?: string })=>{
                resolve({
                    value: result.couldSet,
                    reason: result.reason
                });
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.setPasswordInfo()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'setPasswordInfo', [info]);
        });
    }

    getPasswordInfo(key: string, options?: PasswordManagerPlugin.GetPasswordInfoOptions): Promise<PasswordManagerPlugin.PasswordInfo> {
        return new Promise((resolve, reject)=>{
            exec((result: { passwordInfo: PasswordManagerPlugin.PasswordInfo })=>{
                resolve(result.passwordInfo);
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.getPasswordInfo()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'getPasswordInfo', [key, options]);
        });
    }

    getAllPasswordInfo(): Promise<PasswordManagerPlugin.PasswordInfo[]> {
        return new Promise((resolve, reject)=>{
            exec((result: { allPasswordInfo: PasswordManagerPlugin.PasswordInfo[] })=>{
                resolve(result.allPasswordInfo);
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.getAllPasswordInfo()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'getAllPasswordInfo', []);
        });
    }

    deletePasswordInfo(key: string): Promise<PasswordManagerPlugin.BooleanWithReason> {
        return new Promise((resolve, reject)=>{
            exec((result: { couldDelete: boolean, reason?: string })=>{
                resolve({
                    value: result.couldDelete,
                    reason: result.reason
                });
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.deletePasswordInfo()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'deletePasswordInfo', [key]);
        });
    }

    generateRandomPassword(options?: PasswordManagerPlugin.PasswordCreationOptions): Promise<string> {
        return new Promise((resolve, reject)=>{
            exec((result: { generatedPassword: string })=>{
                resolve(result.generatedPassword);
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.generateRandomPassword()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'generateRandomPassword', [options]);
        });
    }

    changeMasterPassword(): Promise<PasswordManagerPlugin.BooleanWithReason> {
        return new Promise((resolve, reject)=>{
            exec((result: { couldChange: boolean, reason?: string })=>{
                resolve({
                    value: result.couldChange,
                    reason: result.reason
                });
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.changeMasterPassword()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'changeMasterPassword', []);
        });
    }

    lockMasterPassword(): Promise<void> {
        return new Promise((resolve, reject)=>{
            exec(()=>{
                resolve();
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.lockMasterPassword()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'lockMasterPassword', []);
        });
    }

    deleteAll(): Promise<void> {
        return new Promise((resolve, reject)=>{
            exec(()=>{
                resolve();
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.deleteAll()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'deleteAll', []);
        });
    }

    setUnlockMode(mode: PasswordManagerPlugin.PasswordUnlockMode): Promise<void> {
        return new Promise((resolve, reject)=>{
            exec(()=>{
                resolve();
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.setUnlockMode()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'setUnlockMode', [mode]);
        });
    }

    deleteAppPasswordInfo(targetAppId: string, key: string): Promise<PasswordManagerPlugin.BooleanWithReason> {
        return new Promise((resolve, reject)=>{
            exec((result: { couldDelete: boolean, reason?: string })=>{
                resolve({
                    value: result.couldDelete,
                    reason: result.reason
                });
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.deleteAppPasswordInfo()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'deleteAppPasswordInfo', [targetAppId, key]);
        });
    }

    setVirtualDIDContext(didString: string): Promise<void> {
        return new Promise((resolve, reject)=>{
            exec(()=>{
                resolve();
            }, (err)=>{
                console.error("Error while calling PasswordManagerPlugin.setVirtualDIDContext()", err);
                reject(this.nativeToTSException(err));
            }, 'PasswordManagerPlugin', 'setVirtualDIDContext', [didString]);
        });
    }

    /**
     * Tries to convert a native error into a better TS error type for app convenience.
     */
    private nativeToTSException(nativeErr) {
        if (!nativeErr.code) {
            // Not our custom format, just return the raw exception
            return nativeErr;
        }

        switch (nativeErr.code) {
            case NativeErrorCode.NATIVE_ERROR_CODE_INVALID_PASSWORD:
                return new InvalidPasswordExceptionImpl(nativeErr.reason);
            case NativeErrorCode.NATIVE_ERROR_CODE_INVALID_PARAMETER:
                return new InvalidParameterExceptionImpl(nativeErr.reason);
            case NativeErrorCode.NATIVE_ERROR_CODE_CANCELLED:
                return new CancellationExceptionImpl(nativeErr.reason);
            case NativeErrorCode.NATIVE_ERROR_CODE_UNSPECIFIED:
                return new UnspecifiedExceptionImpl(nativeErr.reason);
            default:
                return nativeErr;
        }
    }
}

class InvalidPasswordExceptionImpl extends Error implements PasswordManagerPlugin.InvalidPasswordException {
    constructor(message?: string) {
        super(message);
    }
}

class InvalidParameterExceptionImpl extends Error implements PasswordManagerPlugin.InvalidParameterException {
    constructor(message?: string) {
        super(message);
    }
}

class CancellationExceptionImpl extends Error implements PasswordManagerPlugin.CancellationException {
    constructor(message?: string) {
        super(message);
    }
}

class UnspecifiedExceptionImpl extends Error implements PasswordManagerPlugin.UnspecifiedException {
    constructor(message?: string) {
        super(message);
    }
}

export = new PasswordManagerImpl();