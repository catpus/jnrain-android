/*
 * Copyright 2013 JNRain
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jnrain.mobile.util;

import java.util.ArrayList;
import java.util.List;

import org.jnrain.mobile.updater.UpdateInfo;

import android.accounts.Account;
import android.content.Context;
import android.webkit.CookieSyncManager;


public class GlobalState {
    protected static boolean _cookieInited = false;
    protected static boolean _versionInited = false;
    protected static int _versionCode = -1;
    protected static String _versionName = "VERSION_NOT_INITED";
    protected static UpdateInfo _updInfo = null;

    protected static Account _account = null;
    protected static List<AccountStateListener> _accountListeners = null;

    public static synchronized void possiblyInitState(Context ctx) {
        // init global version info
        AppVersionHelper.ensureVersionInited(ctx);

        // cookie manager
        if (!getCookieInited()) {
            CookieSyncManager.createInstance(ctx);
            setCookieInited(true);
        }

        // account state listener
        ensureAccountListenersList();
    }

    protected static void ensureAccountListenersList() {
        if (_accountListeners == null) {
            _accountListeners = new ArrayList<AccountStateListener>();
        }
    }

    public static synchronized boolean getCookieInited() {
        return _cookieInited;
    }

    public static void setCookieInited(boolean inited) {
        _cookieInited = inited;
    }

    public static synchronized Account getAccount() {
        return _account;
    }

    public static synchronized void setAccount(Account account) {
        _account = account;

        // notify listeners
        ensureAccountListenersList();
        if (_account != null) {
            // logged in event
            for (AccountStateListener listener : _accountListeners) {
                listener.onAccountLoggedIn(_account.name);
            }
        } else {
            // logged out event
            for (AccountStateListener listener : _accountListeners) {
                listener.onAccountLoggedOut();
            }
        }
    }

    public static synchronized String getUserName() {
        if (_account == null) {
            return "";
        }

        return _account.name;
    }

    public static void registerAccountStateListener(
            AccountStateListener listener) {
        ensureAccountListenersList();
        if (_accountListeners.contains(listener)) {
            return;
        }

        _accountListeners.add(listener);
    }

    public static synchronized boolean isVersionInited() {
        return _versionInited;
    }

    public static synchronized int getVersionCode() {
        return _versionCode;
    }

    public static synchronized String getVersionName() {
        return _versionName;
    }

    public static synchronized UpdateInfo getUpdateInfo() {
        return _updInfo;
    }

    public static synchronized void setVersionInfo(
            int versionCode,
            String versionName,
            UpdateInfo updInfo) {
        _versionCode = versionCode;
        _versionName = versionName;
        _updInfo = updInfo;
        _versionInited = true;
    }

    public static synchronized void setUpdateInfo(UpdateInfo updInfo) {
        _updInfo = updInfo;
    }
}
