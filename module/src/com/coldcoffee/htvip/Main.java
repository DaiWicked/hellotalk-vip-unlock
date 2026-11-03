package com.coldcoffee.htvip;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Main implements IXposedHookLoadPackage {

    private static final String TAG = "[HTVIP] ";
    private static final String TARGET_PKG = "com.hellotalk";
    private static final String VIP_UTILS = "lu10";          // VipInfoUtils (6.4.40)
    private static final String USER_BASE = "com.hellotalk.api.user.api.model.UserBaseInfo";
    private static final String LAUNCH_CFG = "djh";          // LaunchConfigV2 model (6.4.40)

    private static XSharedPreferences sPrefs;

    private static boolean on(String key) {
        try {
            if (sPrefs == null) {
                sPrefs = new XSharedPreferences("com.coldcoffee.htvip", "htvip");
                sPrefs.makeWorldReadable();
            }
            sPrefs.reload();
            return sPrefs.getBoolean(key, true);
        } catch (Throwable t) {
            return true;  // 读不到默认全开
        }
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpp) {
        if (!TARGET_PKG.equals(lpp.packageName)) {
            return;
        }
        XposedBridge.log(TAG + "loaded into " + lpp.packageName);

        // --- 1) VipInfoUtils: 本地 VIP 状态判断 ---
        if (on("vip"))
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
        if (on("badge"))
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

        // --- 3) LaunchConfigV2(djh): 解锁语聊房 Tab（大陆 learnLiveTab=0 被关）---
        if (on("voice"))
        // learnLiveTab=2 + liveVoiceTab=1 → parseTabConfig 显示 Voice/Live Tab
        try {
            Class<?> cfg = XposedHelpers.findClass(LAUNCH_CFG, lpp.classLoader);
            // b() = getLearnLiveTab() -> 2
            XposedHelpers.findAndHookMethod(cfg, "b", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p) {
                    return Integer.valueOf(2);
                }
            });
            // c() = getLiveVoiceTab() -> 1
            XposedHelpers.findAndHookMethod(cfg, "c", new XC_MethodReplacement() {
                @Override protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p) {
                    return Integer.valueOf(1);
                }
            });
            XposedBridge.log(TAG + "hooked djh b/c (voice tab) OK");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook djh FAILED: " + t);
        }

        // --- 4) 防撤回: pct.g(vyl, jps) = RoamingAction.processDeleteAction no-op ---
        if (on("revoke"))
        // 保留 msgs_clear(自己清空)与 msgs_upload；只拦 msgs_delete。
        // 注意：服务端 msgs_delete 同时承载"自己多端删除同步"，全 no-op 会导致
        // 自己在其他设备删除的消息在本机复活。默认开启，UI 开关可关。
        try {
            Class<?> pct = XposedHelpers.findClass("pct", lpp.classLoader);
            XposedHelpers.findAndHookMethod(pct, "g",
                XposedHelpers.findClass("vyl", lpp.classLoader),
                XposedHelpers.findClass("jps", lpp.classLoader),
                new XC_MethodReplacement() {
                    @Override protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam p) {
                        // 调用回调保持计数一致，但不执行删除
                        try {
                            Object cb = p.args[1];
                            if (cb != null) {
                                java.lang.reflect.Method m = cb.getClass().getMethod("invoke",
                                        Object.class, Object.class);
                                m.invoke(cb, Integer.valueOf(0), null);
                            }
                        } catch (Throwable ignored) {}
                        return null;
                    }
                });
            XposedBridge.log(TAG + "hooked pct.g (anti-revoke) OK");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "hook pct FAILED: " + t);
        }
    }
}