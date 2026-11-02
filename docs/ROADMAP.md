# 项目计划表

## 阶段 0：已完成 ✅

- [x] 反编译 + 定位 VIP 判断（VipInfoUtils/lu10）
- [x] 重签名方案（被服务端识别，弃用，教训记录于 analysis/HOOK_POINTS.md）
- [x] 转向 LSPosed hook 方案
- [x] 模块 v1.1：VIP 状态 + 角标，真机验证通过
- [x] 翻译无限使用（用户实测）

## 阶段 1：隐藏功能挖掘与验证（进行中）

| # | 功能 | 判断点 | 难度 | 风险 | 状态 |
|---|---|---|---|---|---|
| 1.1 | 语聊房 Tab（大陆隐藏） | `ajx.liveVoiceTab` / `ijh.c` / `create_voice_room_permission` | 中 | 低（纯显示） | 🔍 已定位，待动态验证 |
| 1.2 | 消息防撤回 | `pct.g()`（msgs_delete 处理） | 中 | 低（本地 DB） | 🔍 已定位 |
| 1.3 | 黑名单反向可见 | `relationType` 判断点 | 高 | 中（服务端拒发消息，仅 UI 层） | 🔍 枚举未知 |
| 1.4 | WhatsApp 消息集成 | `whatsapp_message` flag | 低 | 低 | 🔍 |
| 1.5 | 群语音通话 | `group_voip` flag | 中 | 低 | 🔍 |
| 1.6 | AI 陪聊入口 | `ai_chat_agent_entry` | 低 | 中（服务端额度） | 🔍 |
| 1.7 | 密友功能 | `closed_friend_switch` | 中 | 低 | 🔍 |
| 1.8 | FakeBox（假消息盒） | `fake_box_config` | 低 | ？ | 🔍 需调研用途 |

**方法论**：每个功能先静态定位判断点 → hook 后动态验证 → 服务端强校验的标记为"仅 UI"并记录。

## 阶段 2：模块框架化

- [ ] 功能开关持久化（XSharedPreferences，模块配置页写、hook 内读）
- [ ] 每功能独立开关，默认全关（降低风控面）
- [ ] 版本自适应：混淆名映射表 + 启动时校验，不匹配则禁用并写日志
- [ ] 错误隔离：每个 hook 独立 try/catch，一个失败不影响其他

## 阶段 3：配置 UI（Liquid Glass 风格）

- [ ] 形式 A：模块独立配置 App（LSPosed 点开配置）—— 简单，先做
- [ ] 形式 B：注入 HelloTalk 设置页入口（参照 NewHookVip 的"拓展应用"模式）
- [ ] UI 参考 `Desktop\工程文件2`：liquid-glass-js / miuix / colorui 素材
- [ ] 开关列表 = 阶段 1 功能表 + VIP 开关组

## 阶段 4：工程化收尾

- [ ] Gradle 化构建（替换手搓 javac/d8 流程）
- [ ] GitHub 上传（README/.gitignore 已就绪）
- [ ] Releases 发 APK

## 风险登记

| 风险 | 缓解 |
|---|---|
| App 更新混淆名变化 | 版本检测 + 映射表机制（阶段 2） |
| 服务端风控升级检测 Xposed | 开关默认关闭；Shamiko/HMA 隐藏 root |
| 账号封禁 | 不 hook 上报接口；不伪造服务端状态 |
