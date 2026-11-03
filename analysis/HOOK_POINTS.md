# Hook 点速查表（6.4.40 混淆名映射）

> ProGuard 混淆名与原始类/方法对照。**版本敏感**：App 更新后混淆名会变，需重新映射。

## VIP 核心

| 混淆名 | 原始类/方法 | 说明 | 模块动作 |
|---|---|---|---|
| `lu10` | `com.hellotalk.feature.common.business.data.utils.VipInfoUtils` | VIP 状态单例，KV 表 `vip_info` | hook 6 方法 |
| `lu10.r()` | `isVip()` | `m()>0 && h()>0` | → true |
| `lu10.u()` | `isVipPlus()` | `j()>0 && i()>0` | → true |
| `lu10.m()` | `getVipType()` | 读 KV `vip_type` | → 3 |
| `lu10.j()` | `getVipPlusType()` | 读 KV `vip_plus_type` | → 3 |
| `lu10.h()` | `getVipRemainDays()` | `e(c("vip_expireTime"))` | → 9999 |
| `lu10.i()` | `getVipPlusRemainDays()` | | → 9999 |
| — | `com.hellotalk.api.user.api.model.UserBaseInfo.getVipType()` | 服务器 JSON 字段 `vip_type` | → 100（终身） |

## VIP 判断的三个层次

1. **本地 KV**（`VipInfoUtils`）：登录后从 pay_info 接口同步 → 我们 hook 的层面
2. **服务器 JSON**（`UserBaseInfo.vipType`）：角标/资料卡显示 → 已 hook
3. **服务端接口校验**：详情数据、特权额度 → 无法绕过

## 签名/防护链（重签名方案被拒的原因）

| 组件 | 位置 | 作用 |
|---|---|---|
| `HTApkSignature.detectApkTampered` | `lib/arm64-v8a/libhellotalk-tea.so` | native 签名检测，缓存结果=1 表示篡改 |
| `TeaUtils.signRegisterParams(json, type, envCheck)` | native | 注册请求签名 `htntkey`，envCheck=true 时携带篡改状态 |
| `TeaUtils.xConnInfo(ctx, ts, deviceId)` | native | 生成 `androidApkSignature`，登录/IM 连接校验 |
| `TeaUtils.getMd5WithKey(str, keyType)` | native | 各接口 `htntkey` 参数 |
| `PlayIntegrityHelper` (`j9q`) | | Google Play Integrity token，注册接口注入 `integrity_token` |
| 签名盐值 | `ica.c` → `fp9.invoke()` case 4 | 异或解码后为 `Q5bw4aJ9`，用于登录 `sign` 字段 |

**结论：任何 dex 修改+重签名都会被服务端识别。正确路线 = 原版 APK + Xposed 内存 hook。**

## 消息撤回（防撤回候选点）

| 混淆名 | 原始 | 说明 |
|---|---|---|
| `zs` | 漫游操作记录（action_type） | `msgs_upload` / `msgs_clear` / `msgs_delete` |
| `zs.getMsgDelete()` → `vyl` | `{chat_type, chat_id, msg_id_list}` | 删除载荷 |
| `pct`（RoamingAction） | `processDeleteAction` | **真正执行删除的地方**，`f()`=clear 分支，`g()`=delete 分支 |
| `pct.e(List)` | action 分发 | 按 `getActionType()` 路由 |
| `wct`（RoamingManager） | 调度器 | 网络/空闲触发 |
| `DeleteMessagesTask` | 执行 DB 删除 | `ptv.h(msgIdList)` / `qgd.h()` |

**防撤回思路**：hook `pct.g()`（processDeleteAction）直接 no-op，或 hook `getActionType()` 把 `msgs_delete` 改成无害值。**注意**：`msgs_delete` 同时承载双向删除，需要按 `deviceId != 自己` 过滤才能区分"对方撤回"与"自己多端删除"。

## 黑名单/关系

| 类 | 说明 |
|---|---|
| `UserRelation` | 关系模型（followers/following/visitors，无 block 字段） |
| `ngi.relationType` | 关系类型 int（枚举值未定，需动态确认） |
| `BlackInfoNewActivity` | 黑名单管理页（仅管理"我拉黑的人"） |
| `yi1` | `unblockAnonymous(AnonymousBlackUser)` |

**"别人拉黑我"方向**：对方拉黑是服务端关系，客户端最多 hook `relationType` 判断点让 UI 不显示"已拉黑"，但消息发送仍会被服务端拒。待动态验证 relationType 枚举。

## 服务端功能开关（`ajx` = SwitchConfigV2，107 个 flag）

重点 key（`a1u.C(default, "key")`）：

| key | 默认 | 功能 |
|---|---|---|
| `live_voice_tab` | 0 | **语聊房 Tab**（大陆账号被关，隐藏功能主力） |
| `live_voice_swipe_switch` | 0 | 语聊房滑动切换 |
| `learn_live_tab` | 1 | 直播课 Tab |
| `display_course_tab` | | 课程 Tab |
| `search_allow` | 0 | 搜索开关 |
| `whatsapp_message` | 0 | WhatsApp 消息集成 |
| `group_voip` | 0 | 群语音通话 |
| `ai_chat_agent_entry` | 0 | AI 陪聊入口 |
| `closed_friend_switch` | 0 | 密友功能 |
| `show_moment_city` | 0 | 动态城市显示 |
| `googlePlayIntegrity` | | Play 完整性开关 |
| `tabbarDefaultTab` / `V2` | | 默认 Tab |
| `openScreenTabEntrance` | | 开屏 Tab 入口 |

**配置来源**：登录接口下发（`gnk`/`djh` 模型，`bnk` 宿主），本地 `ijh`（缓存读写）。
**语聊房权限另一层**：`t6h.t(uid, "create_voice_room_permission", false)` → KV，创建房间需服务端授权。
**hook 思路**：hook `ajx` 的 getter 让 `live_voice_tab` 返回 1，或 hook `ijh.c` 字段读取。⚠️ Tab 能否显示还依赖服务端下发数据，需动态验证。

## 其他发现

- Flutter 混合开发（`flutter.hellotalk.com/live_tab`、`game_lobby` 等路由）
- `FakeBoxConfig`（`fake_box_config`）：疑似"假消息盒子"，待查
- `ConsumptionPromptModel`（y97）：翻译/聊天次数提醒
- `StealthActivity`：隐身模式（年龄隐身等隐私开关集合）
- UI 框架：大量 Compose + 自研 KMP 组件（HTNicknameComposeView 等）

## 语聊房 Tab 完整链路（阶段1.1 深挖结果，2026-11-03）

### 判定总链
```
登录响应 switch_config
  └─ gnk/djh: learn_live_tab / live_voice_tab / display_course_tab
       └─ ijh (LaunchConfigV2缓存+更新, ot6.d0() 取单例)
            ├─ .b = learnLiveTab    (init: kjk.D().h = KV is_adult, 无关!)
            ├─ .c = liveVoiceTab
            └─ .d = displayCourseTab
                 └─ h1l (MainTabLiveViewModel).v() = parseTabConfig:
                      switch(learnLiveTab):
                        ==0 → Learn tab (g1l.d)
                        ==1 → Live(voice in{0,2})/Voice(voice in{0,1}) + Course(course==1)
                        ==2 → 同上但 voice 语义互换（待真机日志区分）
                        ==3 → ShortDrama (shortflixStatus==1)
                      e9k provider ("live voice api")  → g1l.a = Live 底层列表
                      m3k provider ("live video api")  → g1l.b = Voice 底层列表
```
### e9k/m3k 注册链
- `ud8`(module id 4) → `d30.m`(module_live_communication) → `kvu.d()` case 11 注册 e9k="live voice api"、case 10 注册 m3k="live video api"
- `ud8(4)` 在 `h3e`(BaseApplication).onCreate 注册，条件：主进程（`f==2`，进程名去包名后为空）
- 结论：provider 在主进程**总会注册**，不拦 Tab；真正决定 Tab 的是 `learnLiveTab`/`liveVoiceTab` 服务端下发值

### shouldShowLiveTab（te1 case 11, hrj=LiveTabRegistrar 工具）
```
shouldShowLiveTab() = (learnLiveTab != -1) ? true
                       : (age >= 18 ? true : false)   // 兜底: is_adult>=1(18)或已成年标志
```
注册 Tab 名: learnLiveTab==3 → "shortdrama"图标, 否则 "live_tab_name"(voice pag)

### 结论（hook 方案定稿）
1. **最小 hook**：`ijh` 字段 `c`(liveVoiceTab) 写死为 1 或 2（决定 Voice/Live 哪个显示）+ 字段 `b`(learnLiveTab) 写死为 2（开启语音/直播组）
   - 但 ijh 的值会被登录响应刷新 → 需 hook `djh` 的 getter（`c()I`=liveVoiceTab, `b()I`=learnLiveTab, `d()I`=displayCourseTab）在解析层改
2. 服务端数据风险：语聊房列表接口可能按服务端区域下发，Tab 亮了列表可能为空 → 动态验证
