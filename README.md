# NewNanPlus

该插件目前仅用于Minecraft牛腩小镇服务器。

欢迎来 [牛腩小镇](https://newnan.city/) 玩耍！

牛腩小镇服务器是一个高冷而有爱的MC小服，主营原版生存、建筑和红石，运行版本为Java国际版1.14.4。无论你是大佬还是小白，我们都欢迎你的到来，希望你在这里玩得开心。

## To-do List

- [x] 付费飞行
- [x] 自定义阶梯式死亡惩罚
- [x] 创造区域注册与传送
- [x] 新人入服提醒与自动授权
- [x] 启动/关闭服务期时自动执行指令
- [ ] 卡服分析与优化
  - [x] 漏斗分析
  - [ ] 实体分析
  - [ ] 红石分析
- [x] 定时执行指令
- [ ] 小镇模块
  - [x] 小镇属性
  - [ ] 小镇效果
  - [ ] 小镇共用仓库与公用资金账号
  - [ ] 小镇等级与消耗
  - [ ] 小镇画在地图上，并有链接
- [ ] 真动态货币与浮动物价(依据关键物资开采和消耗量来确定货币购买力)
- [ ] 玩家自动登记&信息统计
- [ ] 小游戏插件
- [ ] 指令粒度命令方块权限管理：设置(修改)、使用和移除 - 可继承自玩家权限节点，基于正则，可设置自己的组别


---

## 模块开发规范

如果你想来帮忙，请遵循以下规范：

1. 功能请以模块为单位，一个模块包含一系列相似相关的功能，且最好由一个人完成——如果你想改善的功能所辖另一个人负责的模块，请与其协商合作。
2. 模块必须实现`city.newnan.newnanplus.NewNanPlusModule`接口，且构造函数有且仅有一个`city.newnan.newnanplus.GlobalData`参数。且必须有一个`private final GlobalData globalData`。
3. 模块利用`GlobalData`中的`configManager`管理 **YAML** 资源文件。
4. 模块不要自己注册命令，而是先在`plugin.yml`中定义好命令和对应权限，再利用`commandManager`注册命令。
5. 模块使用`GlobalData`中的几个方法输出消息，且消息输出不能直接使用内置的字符串如`globalData.senMessage(sender, "xxxx")`而是需要调用`globalData.wolfyLanguageAPI.replaceKeys()`方法，获取当前语言文件配置下的文本内容。文本内容在`lang/xxx.json`中先注册。
6. 提交代码审核通过后，会在`GlobalData`中注册你的模块。