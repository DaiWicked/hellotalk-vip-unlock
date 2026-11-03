# API 层架构笔记（6.4.40）

> 目的：理清请求-签名-风控全链路，评估"重放/伪造请求"与"hook 注入参数"的可行面。
> 结论先行：**签名算法全在 native（libhellotalk-tea.so）+ 服务端强校验，重放路线不可行；hook 注入参数只能改客户端行为，服务端权限类接口无效。**

## 1. 网络层结构（自上而下）

```
业务代码 (各 Request 类)
  │  继承 iiq/ygq，实现 k(JSONObject) 填充参数
  │  签名: TeaUtils.getMd5WithKey / getHtntKey / xConnInfo (native)
  ▼
sps (RegisterService)  — 注册类请求特殊通道 (buildSignV2Data + signRegisterParams)
  ▼
sps.g()/f() → eps (请求封装: url+body+callback)
  ▼
pjo (OkHttpUtils 单例)  — 真正的 HTTP 层
  ├─ e(url, headers) 通用 GET
  ├─ 全局头: a3f.f164a (HashMap)
  │    ├─ "Referer"     = an-api.hellotalk.com/{uid}
  │    └─ "User-Agent"  = ou3.z(uid)
  │  设置点: UserSessionInitTask (登录后会话初始化)
  ▼
OkHttp → api-global.hellotalk8.com / api-cn.hellotalk8.com (运行时选择)
```

- 认证: `session_key`（KV 存储, ksd.e()）—— 未发现标准 `Authorization` 头注入主链路（主要 API 用参数内 userid+htntkey 认证, web/passkey 路线才有 Bearer）
- 注解 `@dfc("path")` = 声明式端点标记（运行时反射路由），`@sxr` = 参数名

## 2. 端点清单（290 个，见 /h/htcrack/endpoints.txt）

按服务分域:
| 前缀 | 功能 | 数量 |
|---|---|---|
| /user_register_center/v2-v3 | 注册/登录/封号/注销/找回 | ~35 |
| /livehub/* | 语聊房+直播（含 voice/channel_list、stage、gift、caption） | ~60 |
| /api/ad/* | 广告 SDK 上报 | ~10 |
| /v1|v2/api/talk/* | 消息（Kakao/iOS 桥接遗留） | ~10 |
| /payment_gateway_v2/* | 支付 | ~5 |

**语聊房数据接口**（服务端区域拦截的实测对象）:
```
/livehub/channel_list/voice          房间列表（主）
/livehub/channel_list/voice_swipe    滑动 feed
/livehub/channel_recommend/voice     推荐
/livehub/batch_query_channel         批量房间信息
/livehub/create_live_channel         建房
```
接口定义: `defpackage/iyi.java`（Retrofit 风格接口）。服务端按账号区域返回空列表 → 与客户端无关。

## 3. 签名体系（4 套，全部 native）

| 签名 | 生成 | 使用场景 | 盐值/keyType |
|---|---|---|---|
| `htntkey` (getHtntKey) | `md5(keyType + ts)` → getMd5WithKey(...,1) | 通用业务接口（guest_config、purchase record 等） | type=1 |
| `htntkey` (直接调) | `getMd5WithKey(userid+ts, 3)` | lua/翻译/发音评估等 | type=3 |
| `sign` | `md5("client_version=..&deviceid=..&login_type=..&ts=.." + 盐)` | 登录/注册 auth 接口 | 盐=`Q5bw4aJ9`（ica.c, 异或 90 解码） |
| `androidApkSignature` | `xConnInfo(ctx, ts, deviceId)` | IM WebSocket 连接、auth | 内部含签名指纹 |
| `htntkey`(注册) | `signRegisterParams(json, 1, envCheck)` | /user_register_center/v2/register | 整包 JSON 签名+环境检查 |

**关键结论**: `getMd5WithKey`/`xConnInfo`/`signRegisterParams` 都是 native 实现，算法在 .so 内。纯客户端 hook 可以**调用**它（让 App 替你算任何参数的签名），但无法离线重放。且服务端校验 userid+ts 绑定 session，第三方重放没有 session 无效。

## 4. 风控字段（注册/登录通道）

| 字段 | 来源 | 说明 |
|---|---|---|
| `irisk_token` | r4e (数美/网易易盾类 SDK) | 设备风险 token |
| `watchman_token` | 固定空串（当前版本） | 留位 |
| `integrity_token` | Play Integrity | 无 GMS 恒为空，服务端容忍 |
| `login_check_id` | r4e.d | 本次登录检查会话 id |
| `behavior_validate` | ij30.d | 行为验证（滑块）结果 |
| `szm_device_id` | xkv.h | 数美设备指纹 |
| `jail_break` | bv70.d() | root 标记（byte） |
| `is_vpn` | sn20.o() | VPN 检测 |

**注册被拒的根因排序**（结合 11-02 实验）: `htntkey`(native 签名含篡改态) > `irisk_token`/设备指纹 > integrity。原版+LSPosed 不触碰以上任何字段 → 干净。

## 5. 可行面评估

| 路线 | 可行性 | 理由 |
|---|---|---|
| 离线重放/自写客户端调 API | ❌ | 签名 native + session 绑定 + 设备指纹 |
| hook 签名函数改参数 | ⚠️ 仅本地 | 服务端会重新校验数据一致性 |
| hook 响应解析（伪造服务端返回） | ✅ 本地 | UI 层欺骗（如翻译计数清零），无服务端效果 |
| hook 请求参数（如删掉 jailBreak/VPN 标记） | ✅ 本地 | 降低风控画像，用于自保而非解锁 |
| 直连 web 接口（Bearer 路线） | ⚠️ | passkey/web 登录链路，需真实 session，无收益 |

**可做的实事**: 在现有模块里加一个「风控字段清洗」开关：hook 登录/注册请求构造（x83/sps），将 `jail_break` 置 0、`is_vpn` 置 0，降低设备画像。这是 API 层探索能落地的唯一有价值的 hook。
