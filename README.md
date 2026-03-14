# FJmode

`FJmode` 是一个基于 Fabric 的 Minecraft `1.21.11` 模组项目，当前方向是附魔效果扩展。现阶段已实现的核心内容为剑类附魔 `御剑飞行`。

## 技术栈

- Minecraft `1.21.11`
- Fabric Loader `0.18.2`
- Fabric API `0.139.4+1.21.11`
- Fabric Loom `1.14-SNAPSHOT`
- Java `21`
- Mojang Official Mappings

项目配置见 [gradle.properties](C:/Users/jed/FJmode/gradle.properties)。

## 当前实现

### 御剑飞行

- 可附加在剑上
- 持有附魔剑时可进入自定义飞行
- 飞行基础速度公式对齐原版鞘翅滑翔
- 飞行时保持站立姿态
- 玩家脚下渲染与主手一致的剑模型
- 飞行中按 `Ctrl` 可触发接近原版烟花的持续助推
- 助推会额外消耗饱食度
- 飞行过程会持续消耗剑耐久

### 攻击限制

- 持有带 `御剑飞行` 的剑时，实体近战攻击被禁用
- 客户端不会发起实体攻击，避免残留攻击表现
- 蓄力横扫被禁用

## 项目结构

主要源码目录：

- [src/main/java/com/fjmode](C:/Users/jed/FJmode/src/main/java/com/fjmode)
- [src/client/java/com/fjmode](C:/Users/jed/FJmode/src/client/java/com/fjmode)
- [src/main/resources](C:/Users/jed/FJmode/src/main/resources)
- [src/client/resources](C:/Users/jed/FJmode/src/client/resources)

核心模块：

- [FJModeMod.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/FJModeMod.java)
  通用入口，负责注册附魔、网络和飞行控制。

- [FJModeModClient.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/FJModeModClient.java)
  客户端入口，负责注册飞行输入监听。

- [ModEnchantments.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/enchantment/ModEnchantments.java)
  附魔资源键定义，当前包含 `sword_flight`。

- [SwordFlightController.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/flight/SwordFlightController.java)
  服务端飞行控制核心，负责飞行判定、滑翔速度更新、助推、耐久和饱食度消耗。

- [SwordFlightClient.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/flight/SwordFlightClient.java)
  客户端每 tick 监听冲刺键，并在飞行时发送助推包。

- [ModNetworking.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/network/ModNetworking.java)
  自定义 Payload 注册与服务端接收器绑定。

- [SwordFlightBoostPayload.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/network/SwordFlightBoostPayload.java)
  御剑飞行助推的 C2S 数据包。

## 数据与资源

附魔定义：

- [data/fjmode/enchantment/sword_flight.json](C:/Users/jed/FJmode/src/main/resources/data/fjmode/enchantment/sword_flight.json)

附魔适用物品标签：

- [data/fjmode/tags/item/enchantable/sword_flight.json](C:/Users/jed/FJmode/src/main/resources/data/fjmode/tags/item/enchantable/sword_flight.json)

附魔可见性标签：

- [data/minecraft/tags/enchantment/non_treasure.json](C:/Users/jed/FJmode/src/main/resources/data/minecraft/tags/enchantment/non_treasure.json)
- [data/minecraft/tags/enchantment/on_random_loot.json](C:/Users/jed/FJmode/src/main/resources/data/minecraft/tags/enchantment/on_random_loot.json)

语言文件：

- [zh_cn.json](C:/Users/jed/FJmode/src/main/resources/assets/fjmode/lang/zh_cn.json)
- [en_us.json](C:/Users/jed/FJmode/src/main/resources/assets/fjmode/lang/en_us.json)

## 客户端渲染

客户端表现由渲染 mixin 和自定义渲染层组成：

- [AvatarRendererMixin.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/mixin/client/AvatarRendererMixin.java)
- [PlayerModelMixin.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/mixin/client/PlayerModelMixin.java)
- [SwordFlightSwordLayer.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/client/render/SwordFlightSwordLayer.java)
- [AvatarRenderStateMixin.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/mixin/client/AvatarRenderStateMixin.java)
- [MinecraftAttackMixin.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/mixin/client/MinecraftAttackMixin.java)

当前渲染行为：

- 飞行时玩家改为站立姿态
- 脚下渲染主手物品同款剑模型
- 剑模型会按飞行方向做旋转和位置调整

## Mixin 配置

通用 mixin：

- [fjmode.mixins.json](C:/Users/jed/FJmode/src/main/resources/fjmode.mixins.json)

客户端 mixin：

- [fjmode.client.mixins.json](C:/Users/jed/FJmode/src/client/resources/fjmode.client.mixins.json)

用途：

- 禁止附魔剑近战攻击与蓄力横扫
- 客户端拦截附魔剑实体攻击发起
- 向渲染状态挂接御剑飞行数据
- 给玩家渲染器添加御剑模型层
- 强制飞行姿态为站立

## 开发约定

### Mojang Official Mappings

项目使用 Mojang Official Mappings，而不是 Yarn。

直接影响：

- 常见 Fabric 教程中的类名不一定可直接复制
- 方法名、字段名和导入路径要以当前项目和编译器结果为准

当前已确认的典型差异：

- `ServerPlayerEntity` 对应 `ServerPlayer`
- `Text` 对应 `Component`
- `Identifier.of(...)` 应使用 `Identifier.fromNamespaceAndPath(...)`

### 分层建议

- 数据定义放 `resources/data`
- 资源文本放 `resources/assets`
- 运行时控制逻辑放 `flight` / `network` / `enchantment`
- 侵入式改动放 `mixin`
- 客户端专用渲染逻辑放 `src/client/java`

## 构建与运行

构建：

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
