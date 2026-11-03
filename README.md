# HelloTalk 6.4.40 逆向研究项目

> ⚠️ **仅供学习研究**：本项目研究 Android App 的客户端架构、防护机制与 Xposed hook 技术。
> 请勿用于商业用途或违反当地法律法规。使用本项目产生的任何后果由使用者自行承担。
> 请支持正版软件。

对 HelloTalk 6.4.40（`com.hellotalk`）的静态分析与 LSPosed 模块开发全记录。

## 项目结构

```
hellotalk-crack/
├── module/                     # LSPosed 模块（HT VIP Unlock）
│   ├── src/com/coldcoffee/htvip/Main.java   # hook 入口
│   ├── AndroidManifest.xml
│   ├── assets/xposed_init
│   ├── res/values/arrays.xml   # 作用域声明
│   └── HTVIPUnlock.apk         # 构建产物 v1.1
├── analysis/                   # 分析笔记（hook 点、类名映射）
├── docs/                       # 文档（隐藏功能调研、计划）
├── tools/                      # 构建工具说明（不提交二进制）
│   └── BUILD.md
└── out/                        # jadx 反编译产物（不提交，见 .gitignore）
```

## 模块功能（v1.1，已验证）

| Hook 点 | 效果 | 状态 |
|---|---|---|
| `lu10.r()` / `u()`（VipInfoUtils.isVip/isVipPlus） | 解锁本地 VIP 功能墙（谁看过我、附近的人、搜索筛选等） | ✅ |
| `lu10.m()` / `j()` → 3 | VIP 类型伪装 | ✅ |
| `lu10.h()` / `i()` → 9999 | VIP 剩余天数 | ✅ |
| `UserBaseInfo.getVipType()` → 100 | 头像/资料页终身 VIP 角标（仅本机显示） | ✅ |
| 翻译次数限制 | 用户实测已无限使用 | ✅ |
| 语聊房 Tab（djh.b→2, djh.c→1） | Tab 已注册（UI可见） | ⚠️ 仅UI，列表空：服务端按区域拦截房间数据 |

**已知的不可行项**（服务端二次校验，客户端无法绕过）：
- 城市/语伴详情页"没有匹配的语伴"（详情接口服务端校验 VIP）
- 其他用户看到你的 VIP 状态

## 快速开始

### 环境要求
- 已 root 的设备 + KernelSU/Magisk + LSPosed
- JDK 17+、Android build-tools（aapt2/d8/zipalign/apksigner）

### 构建
```bash
# 详见 tools/BUILD.md，核心流程：
javac -encoding UTF-8 -source 8 -target 8 -d build/classes $(find module/src -name "*.java")
d8 --release --min-api 27 --output build $(find build/classes/com -name "*.class")
aapt2 link -o base.apk -I android.jar --manifest module/AndroidManifest.xml -A module/assets module/res_c.zip ...
# zipalign + apksigner 签名
```

### 安装使用
1. 安装 `module/HTVIPUnlock.apk`
2. LSPosed → 模块 → 启用 HT VIP Unlock → 作用域勾选 HelloTalk
3. 强制停止 HelloTalk 重开
4. 验证：LSPosed 日志中搜 `HTVIP`，看到 `hooked lu10 r/u/m/j/h/i OK` 即成功

## 反编译流程（复现分析）

```bash
jadx -d out --no-res HelloTalk_6.4.40.apk   # Java 源码
apktool d -f -o smali_out HelloTalk_6.4.40.apk  # smali
```

## License

仅供学习。代码部分 MIT，分析结论不对准确性作担保。
