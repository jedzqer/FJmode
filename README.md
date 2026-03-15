# FJmode

`FJmode` 是一个基于 Fabric 的 Minecraft `1.21.11` 模组项目，当前方向是附魔效果扩展。现阶段已实现两个剑类附魔：`御剑飞行` 与 `万剑归宗`。

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
- 飞行时隐藏玩家手中的剑，并在脚下居中渲染同款剑模型
- 飞行中按 `Ctrl` 可触发接近原版烟花的持续助推
- 助推会额外消耗饱食度
- 飞行过程会持续消耗剑耐久

### 万剑归宗

- 可附加在剑上
- 附魔台最高 `1` 级，附魔权重为 `10`，属于中等偏稀有的普通附魔
- 右键长按时使用原版三叉戟式蓄力动作，松手后发射飞剑
- 发射时会把主手中的真实剑取走，飞行阶段由服务端维护虚拟飞剑对象池
- 可同时存在多把飞剑
- 虚拟飞剑会在玩家上方较大范围内环绕飞行，并保留带俯仰的群飞轨迹
- 玩家在已有飞剑存在时，近战命中或远程投射物命中实体后，可将该实体设为当前追踪目标
- 同一轮追击中，必须等飞剑完成命中回弹并重新回到群飞轨道后，才允许再次锁定新目标
- 追踪目标时会选取一把样本飞剑计算必中冲刺解，其余飞剑直接复用该结果同步扑向目标
- 飞剑命中目标时，按飞剑池内所有参与攻击的剑伤害总和除以 `3` 结算一次伤害
- 单把剑伤害会尽量带上剑本体攻击力和附魔伤害修正
- 命中后会触发附魔后攻击效果结算，随后先沿当前突进方向继续穿出，带少量上扬惯性，再回到活动范围并恢复原有集群飞行
- 在追踪、命中回弹、回群等特殊阶段会暂停寿命计时，避免过渡过程中意外进入掉落/消失路径
- 飞行满 `60` 秒后会转为原始剑物品的掉落物实体，保留少量惯性后掉落在世界中，等待玩家自行回收

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
  通用入口，负责注册附魔、网络、御剑飞行与万剑归宗控制器。

- [FJModeModClient.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/FJModeModClient.java)
  客户端入口，负责注册飞行输入监听和万剑归宗客户端渲染/同步接收。

- [ModEnchantments.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/enchantment/ModEnchantments.java)
  附魔资源键定义，当前包含 `sword_flight` 与 `myriad_swords_return`。

- [SwordFlightController.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/flight/SwordFlightController.java)
  服务端飞行控制核心，负责飞行判定、滑翔速度更新、助推、耐久和饱食度消耗。

- [MyriadSwordsController.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/flight/MyriadSwordsController.java)
  万剑归宗服务端核心，负责蓄力发射、飞剑池管理、Boids 运动、近战/远程目标锁定、追踪攻击、命中后返航、伤害结算与寿命结束后的实体化。

- [SwordFlightClient.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/flight/SwordFlightClient.java)
  客户端每 tick 监听冲刺键，并在飞行时发送助推包。

- [MyriadSwordsClient.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/flight/MyriadSwordsClient.java)
  客户端接收飞剑快照并在世界中渲染虚拟飞剑。

- [ModNetworking.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/network/ModNetworking.java)
  自定义 Payload 注册与服务端接收器绑定。

- [SwordFlightBoostPayload.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/network/SwordFlightBoostPayload.java)
  御剑飞行助推的 C2S 数据包。

- [MyriadSwordsSyncPayload.java](C:/Users/jed/FJmode/src/main/java/com/fjmode/network/MyriadSwordsSyncPayload.java)
  万剑归宗飞剑状态同步的 S2C 数据包。

## 数据与资源

附魔定义：

- [data/fjmode/enchantment/sword_flight.json](C:/Users/jed/FJmode/src/main/resources/data/fjmode/enchantment/sword_flight.json)
- [data/fjmode/enchantment/myriad_swords_return.json](C:/Users/jed/FJmode/src/main/resources/data/fjmode/enchantment/myriad_swords_return.json)

附魔适用物品标签：

- [data/fjmode/tags/item/enchantable/sword_flight.json](C:/Users/jed/FJmode/src/main/resources/data/fjmode/tags/item/enchantable/sword_flight.json)
- [data/fjmode/tags/item/enchantable/myriad_swords_return.json](C:/Users/jed/FJmode/src/main/resources/data/fjmode/tags/item/enchantable/myriad_swords_return.json)

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
- [MyriadSwordsClient.java](C:/Users/jed/FJmode/src/client/java/com/fjmode/flight/MyriadSwordsClient.java)

当前渲染行为：

- 飞行时玩家改为站立姿态
- 飞行时隐藏主手显示，避免手里和脚下同时出现同一把剑
- 在玩家脚部中线附近渲染主手物品同款剑模型
- 剑模型会跟随飞行朝向旋转，并按视角俯仰同步上扬或下压
- 万剑归宗的虚拟飞剑通过世界渲染事件直接绘制，不创建客户端实体
- 客户端根据服务端同步的位置和速度快照做平滑跟随，并直接根据速度向量生成四元数朝向，减轻模型抖动与阶段性翻面
- 万剑归宗飞剑会按飞行方向调整姿态，并保留一个顺时针 `45` 度的模型补偿，让剑尖正确对齐运动方向

## Mixin 配置

通用 mixin：

- [fjmode.mixins.json](C:/Users/jed/FJmode/src/main/resources/fjmode.mixins.json)

客户端 mixin：

- [fjmode.client.mixins.json](C:/Users/jed/FJmode/src/client/resources/fjmode.client.mixins.json)

用途：

- 禁止附魔剑近战攻击与蓄力横扫
- 客户端拦截不应触发的附魔剑实体攻击发起，并保留万剑归宗的目标指定
- 给带 `万剑归宗` 的剑补充原版三叉戟式蓄力/释放行为
- 在投射物命中实体时补充万剑归宗目标锁定
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

### 万剑归宗实现说明

- 虚拟飞剑存在于服务端内存，不是原版实体，因此默认不参与方块碰撞，也不会自行触发原版投掷物命中流程
- 飞剑追踪目标和伤害结算都在服务端完成，客户端只负责表现
- 当前目标设定方式是：玩家在已有飞剑存在时，近战命中实体或玩家发射的投射物命中实体后，将其登记为该玩家飞剑池的当前目标；必须等这一轮追击结束并回到轨道后，才能再次指定新目标
- 当前追踪方式是：每 tick 随机取一把样本飞剑计算一次追踪冲刺解，其余飞剑直接套用这次结果，保证整群同步扑向目标
- 当前伤害规则是：参与本次命中的所有飞剑，逐把统计剑本体攻击力与附魔伤害修正，再将总和除以 `3`
- 当前命中后不会消耗飞剑，而是先沿命中前的速度方向穿出并保留少量上扬惯性，再延迟返航到活动范围，随后重新接入原有群飞逻辑
- 当前在锁定追踪、命中回弹和回群阶段会暂停寿命计时；寿命结束时会把虚拟飞剑转换为对应剑物品的掉落物实体，并掉落在世界中，由玩家自行拾取

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
