# FJmode

`FJmode` 是一个基于 Fabric 的 Minecraft 1.21.11 模组项目，当前目标是实现“附魔效果”方向的扩展内容。

目前项目已经落地的第一个功能附魔为 `御剑飞行`：

- 可附加在剑上
- 持有附魔剑时可进入带惯性的自定义前向飞行
- 飞行时保持站立姿态
- 玩家脚下会渲染一把与主手同款的剑模型，剑尖朝向视线前方
- 飞行中按 `Ctrl`（冲刺键）可触发一次类似烟花的前冲加速
- 加速会额外消耗饱食度
- 飞行过程会持续消耗剑耐久
- 该附魔会使剑失去攻击用途

## 技术栈

- Minecraft `1.21.11`
- Fabric Loader `0.18.2`
- Fabric API `0.139.4+1.21.11`
- Fabric Loom `1.14-SNAPSHOT`
- Java `21`
- Mojang Official Mappings

项目配置见 [gradle.properties](C:/Users/jed/FJmode/gradle.properties)。

## 项目结构

主要源码目录：

- [src/main/java/com/fjmode](C:/Users/jed/FJmode/src/main/java/com/fjmode)
- [src/client/java/com/fjmode](C:/Users/jed/FJmode/src/client/java/com/fjmode)
- [src/main/resources](C:/Users/jed/FJmode/src/main/resources)
- [src/client/resources](C:/Users/jed/FJmode/src/client/resources)

当前核心模块：

- [FJModeMod.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/FJModeMod.java)
  服务端/通用入口，负责注册附魔效果、网络和飞行控制。

- [FJModeModClient.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/FJModeModClient.java)
  客户端入口，负责注册客户端飞行输入监听。

- [ModEnchantments.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/enchantment/ModEnchantments.java)
  附魔资源键定义，当前包含 `sword_flight`。

- [SwordFlightController.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/flight/SwordFlightController.java)
  御剑飞行的核心服务端逻辑，包括：
  飞行判定、自定义飞行状态维护、速度控制、耐久消耗、加速处理。

- [SwordFlightClient.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/flight/SwordFlightClient.java)
  客户端每 tick 监听冲刺键，飞行中发送加速网络包。

- [ModNetworking.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/network/ModNetworking.java)
  自定义 Payload 注册与服务端接收器绑定。

- [SwordFlightBoostPayload.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/network/SwordFlightBoostPayload.java)
  御剑飞行加速的 C2S 数据包。

- [ModEnchantmentEffects.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/enchantment/effect/ModEnchantmentEffects.java)
  自定义附魔效果类型注册入口，当前保留了可扩展框架。

## 当前功能实现

### 1. 御剑飞行附魔

附魔定义文件：

- [data/fjmode/enchantment/sword_flight.json](C:/Users/jed/FJmode/src/main/resources/data/fjmode/enchantment/sword_flight.json)

设计参数：

- `max_level = 1`
- `weight = 10`
- `min_cost = 1`
- `max_cost = 11`
- `anvil_cost = 1`

这表示它被定位为：

- 单级附魔
- 低附魔台门槛
- 刷新权重较高
- 偏常见的功能型附魔

适用物品标签：

- [data/fjmode/tags/item/enchantable/sword_flight.json](C:/Users/jed/FJmode/src/main/resources/data/fjmode/tags/item/enchantable/sword_flight.json)

当前标签直接指向 `#minecraft:swords`，即所有原版剑都可附魔。

附魔可见性标签：

- [data/minecraft/tags/enchantment/non_treasure.json](C:/Users/jed/FJmode/src/main/resources/data/minecraft/tags/enchantment/non_treasure.json)
- [data/minecraft/tags/enchantment/on_random_loot.json](C:/Users/jed/FJmode/src/main/resources/data/minecraft/tags/enchantment/on_random_loot.json)

当前已接入：

- 普通附魔池
- 随机战利品附魔池

### 2. 飞行行为

飞行逻辑主要实现于 [SwordFlightController.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/flight/SwordFlightController.java)。

核心规则：

- 玩家主手必须持有带 `御剑飞行` 的剑
- 玩家不能在水中、岩浆中、乘骑中或睡眠中使用
- 满足条件且处于下落状态时，会激活自定义御剑飞行状态
- 飞行使用模组维护的自定义状态，不依赖原版鞘翅 `fall flying`
- 飞行具有前冲惯性，并持续施加前向推进
- 飞行中会定期消耗剑耐久
- 飞行中按 `Ctrl` 可触发一次短 CD 加速
- 加速会额外消耗饱食度

### 3. 禁止攻击

攻击拦截位于 [PlayerAttackMixin.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/mixin/PlayerAttackMixin.java)。

当前规则是：

- 只要主手剑带有 `御剑飞行`
- 玩家对实体的普通近战攻击会被直接取消

这意味着该剑在设计上是纯飞行工具，而不是战斗武器。

### 4. 客户端表现

客户端表现由渲染 mixin 和自定义渲染层组成：

- [AvatarRendererMixin.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/mixin/client/AvatarRendererMixin.java)
- [PlayerModelMixin.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/mixin/client/PlayerModelMixin.java)
- [SwordFlightSwordLayer.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/client/render/SwordFlightSwordLayer.java)
- [AvatarRenderStateMixin.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/mixin/client/AvatarRenderStateMixin.java)

当前表现包括：

- 飞行时玩家改为站立姿态
- 脚下渲染主手物品同款剑模型
- 如果主手是附魔钻石剑，则脚下显示附魔钻石剑
- 剑朝向玩家当前视线方向

语言文件：

- [zh_cn.json](C:/Users/jed/FJmode/src/main/resources/assets/fjmode/lang/zh_cn.json)
- [en_us.json](C:/Users/jed/FJmode/src/main/resources/assets/fjmode/lang/en_us.json)

## Mixin 配置

通用 mixin：

- [fjmode.mixins.json](C:/Users/jed/FJmode/src/main/resources/fjmode.mixins.json)

客户端 mixin：

- [fjmode.client.mixins.json](C:/Users/jed/FJmode/src/client/resources/fjmode.client.mixins.json)

已使用的 mixin 目的：

- 禁止附魔剑近战攻击
- 向玩家渲染状态挂接额外数据
- 给玩家渲染器添加御剑模型层
- 强制玩家飞行姿态为站立

## 开发约定

### 1. 当前项目使用 Mojang Official Mappings

这不是 Yarn 命名环境。

直接影响：

- 常见 Fabric 教程中的类名不一定可直接复制
- 一些方法和类路径需要以当前项目代码和编译器结果为准

已经确认过的几个典型差异：

- `ServerPlayerEntity` 对应 `ServerPlayer`
- `Text` 对应 `Component`
- `Identifier.of(...)` 在当前环境下应使用 `Identifier.fromNamespaceAndPath(...)`

### 2. Fabric 事件与网络包位置要以当前版本为准

例如：

- `ServerPlayConnectionEvents` 的导入位于 `net.fabricmc.fabric.api.networking.v1`
- `ServerTickEvents` 位于 `net.fabricmc.fabric.api.event.lifecycle.v1`

如果遇到“找不到符号”，优先检查当前版本 API 路径，而不是直接照搬旧示例。

### 3. 新增内容时建议分层

当前项目已经形成了比较清晰的分层方式，建议继续沿用：

- 数据定义放 `resources/data`
- 资源文本放 `resources/assets`
- 运行时控制逻辑放 `flight` / `network` / `enchantment`
- 侵入式改动放 `mixin`
- 客户端专用渲染逻辑放 `src/client/java`

## 构建与运行

在项目根目录执行：

```powershell
.\gradlew.bat build
```

启动客户端开发环境：

```powershell
.\gradlew.bat runClient
```

建议：

- 改了 Java 代码后至少执行一次 `build`
- 改了资源、附魔 JSON、mixin、渲染逻辑后，最好执行一次 `runClient`

因为某些注册期错误只会在真实启动时暴露，而不一定在单纯编译阶段暴露。

## 后续可扩展方向

当前项目已经具备继续扩展的基础框架，后续可以继续做：

- 新增更多附魔
- 为附魔加入村民交易等更多自然获取渠道
- 增加更多自定义附魔效果类型
- 增加命令或调试工具，方便测试飞行参数
- 细调御剑模型的位置、角度、缩放和飞行手感

## 当前状态

截至当前版本，项目至少已验证：

- `.\gradlew.bat build` 可通过
- 附魔、附魔标签、网络、服务端飞行逻辑、客户端渲染和 mixin 已完成接线

如果后续继续开发，建议优先保持 README 与实际源码同步更新。
