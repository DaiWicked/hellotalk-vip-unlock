package com.coldcoffee.htvip;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookLoadPackage {

    private static final String TAG = "[HTVIP] ";
    private static final String TARGET_PKG = "com.hellotalk";
    private static final String VIP_UTILS = "lu10";          // VipInfoUtils (6.4.40)
    private static final String USER_BASE = "com.hellotalk.api.user.api.model.UserBaseInfo";

    private static void hookConst(final String name, final Object value) {
        try {
            XposedBridge.log(TAG + "hooking " + name + " -> " + value);
        } catch (Throwable ignored) {}
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpp) {
        if (!TARGET_PKG.equals(lpp.packageName)) {
            return;
        }
        XposedBridge.log(TAG + "loaded into " + lpp.packageName);

        // --- 1) VipInfoUtils: 本地 VIP 状态判断 ---
        try {
            Class<?> vip = XposedHelpers.findClass(VIP_UTILS, lpp.classLoader);
            XposedHelpers.findAndHookMethod(vip, "r", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p) { return Boolean.TRUE; }
            });
            XposedHelpers.findAndHookMethod(vip, "u", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p) { return Boolean.TRUE; }
            });
            XposedHelpers.findAndHookMethod(vip, "m", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p) { return Integer.valueOf(3); }
            });
            XposedHelpers.findAndHookMethod(vip, "j", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p) { return Integer.valueOf(3); }
            });
            XposedHelpers.findAndHookMethod(vip, "h", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p) { return Integer.valueOf(9999); }
            });
            XposedHelpers.findAndHookMethod(vip, "i", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p) { return Integer.valueOf(9999); }
            });
            XposedBridge.log(TAG + "hooked " + VIP_UTILS + " r/u/m/j/h/i OK");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook " + VIP_UTILS + " FAILED: " + t);
        }

        // --- 2) UserBaseInfo: 服务器 JSON 的 vipType -> 终身VIP(100)，点亮角标 ---
        try {
            Class<?> ub = XposedHelpers.findClass(USER_BASE, lpp.classLoader);
            XposedHelpers.findAndHookMethod(ub, "getVipType", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p) {
                    return Integer.valueOf(100);
                }
            });
            XposedBridge.log(TAG + "hooked UserBaseInfo.getVipType OK");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook UserBaseInfo FAILED: " + t);
        }
    }
}
