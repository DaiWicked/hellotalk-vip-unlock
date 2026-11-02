# 构建说明

模块 APK 用"手搓流水线"构建（无 Gradle 依赖，可复现）：

## 依赖

- JDK 17+
- Android build-tools 36.0.0（`aapt2`、`d8`、`zipalign`、`apksigner`）
- android.jar（任一 platform，本项目用 android-37.0）

## 流程

```bash
MOD=module
OUT=build

# 1. 编译（Xposed API 用编译期 stub，运行时由 LSPosed 注入真实类；
#    stub 源码在 module/src/de/robv/... 会被 d8 排除，只打 com/ 下的类）
javac -encoding UTF-8 -source 8 -target 8 -nowarn -d $OUT/classes $(find $MOD/src -name "*.java")

# 2. dex（⚠️ 只收 com/ 下的 class，绝不能把 de.robv stub 打进 APK，
#    否则 LSPosed 报 "Xposed API classes are compiled into the module's APK" 拒绝加载）
d8 --release --min-api 27 --output $OUT $(find $OUT/classes/com -name "*.class")

# 3. 资源编译 + 链接
aapt2 compile --dir $MOD/res -o res_c.zip
aapt2 link -o $OUT/base.apk -I android.jar --manifest $MOD/AndroidManifest.xml \
    -A $MOD/assets res_c.zip --min-sdk-version 27 --target-sdk-version 34 \
    --version-code 2 --version-name 1.1

# 4. 塞入 classes.dex
python add_dex.py   # zipfile append classes.dex (DEFLATED)

# 5. 对齐 + 签名
zipalign -f 4 $OUT/base.apk $OUT/aligned.apk
apksigner sign --ks debug.jks --ks-pass pass:android --key-pass pass:android \
    --out HTVIPUnlock.apk $OUT/aligned.apk
```

## 关键坑

1. **stub 类不能进 dex**（LSPosed 加载失败，见 logs）
2. **stub 方法签名必须与真实 Xposed API 完全一致**（如 `findAndHookMethod` 返回 `XC_MethodHook.Unhook` 而非 `void`/`Member`，否则运行时 `NoSuchMethodError`）
3. javac 必须 `-encoding UTF-8`（Windows 默认 GBK）
4. aapt2 `compile --dir res` 而非单文件（Windows 路径分隔符问题）
