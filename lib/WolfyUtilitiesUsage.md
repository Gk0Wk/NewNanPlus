# Document for WolfyUtilities

Source Repo: https://github.com/WolfyScript/WolfyUtilities/

## LanguageAPI 多语言接口

基于JSON文件提供多语言能力，使用`replaceKeys`等方法，通过`$路径$`的格式将该符号替换为当前语言对应的文本。API指定首选语言和次要语言，当首选语言文件中路径不存在时会尝试使用次要语言，如果还不存在就不替换符号。

使用方法：

* 通过`WolfyUtilities`实例的`getLanguageAPI()`获取`LanguageAPI`实例。
* 实例化`Language`对象，该对象将与其构造函数中的`lang`字符串指定的`插件配置文件夹/lang/xxx.json`文件绑定。
* 使用`LanguageAPI`实例的`registerLanguage(Language)`和`setFallbackLanguage(Language)`方法注册首要语言和次要语言。
  * 使用`LanguageAPI`实例的`getActiveLanguage`和`getFallbackLanguage`可以分别获得两个Language实例。
* 使用`LanguageAPI`实例的`replaceXXXX`方法族获取语言文件对应的内容(返回字符串或者字符串数组)。
  * `String replaceKeys(String msg)`中的`msg`是含有`$path$`(两边加`$`的json路径形成占位符)的字符串，如`插件版本：$version$`，占位符会被替换为语言文件中对应路径的内容，如果路径对应的是 String 数组，那么就以空格连接。
  * `String replaceColoredXXX(String msg)`会同时替换颜色码`&x`。
  * `List<String> replaceKeys(List<String> msg)`、`List<String> replaceKeys(String... msg)`和`List<String> replaceColoredKeys(List<String> msg)`同理，可以批量替换。
  * `ReplaceKey(String key)` 方法比较特殊，会获取对应路径下的字符串数组，返回`List<String>`。
    * 注意，如果这个节点不是数组而是一个 String就会返回空的`List<String>`。

Language实例拥有一个`getNodeAt`，使用节点路径(以`.`分隔)来获取指定位置的内容并返回JsonNode(com.fasterxml.jackson)对象。但是考虑到其并非是基础类型且还考虑到fallback问题，所以推荐使用`LanguageAPI`中的`replaceXXX`。

## InventoryAPI 背包交互界面接口

利用Minecraft内提供的各种类型和尺寸的背包容器来创建可点选、拖动的GUI窗体，并维持窗体会话的持久性存储，以及界面之间的跳转逻辑和按钮操作。

使用该接口 ，要了解其三大逻辑：

* 界面：窗体集 - 窗体 - 按钮
* 构建：预配置构建 or 自定义构建
* 交互：会话 - 用户缓存 - 

### 界面逻辑

每个使用 `InventoryAPI` 的插件都有一个只属于该插件的 `InventoryAPI` 的实例。每个 `InventoryAPI` 实例管理这个插件注册的一堆窗体`GuiWindow`，这些窗体被划分为若干窗体集`GuiCluster`——也就是说实例包含窗体集，窗体集包含窗体。

窗体内的控件称为按钮`Button`，是界面交互的单位。按钮有若干种，如`ActionButton`等。按钮属于某个窗体，或者属于窗体集（称为全局按钮，由窗体集内所有窗体共用，也可以由其他窗体集的窗体获取）。

### 构建逻辑

构建主要是指按钮的构建。按钮的构建分为两步，第一步是构建`ButtonState`实例，描述了按钮的外观(物品类型、头颅材质、名称等)；第二步是用`ButtonState`构建`Button`，描述了按钮的行为。

`Button`可能不只对应一个`ButtonState`，比如`ToggleButton`，`ButtonState`只是一种外观，而按钮根据其类型不同可以有一个或者更多的外观。所以二者的关系可以是一对多、多对一、多对多的映射。

一个按钮的行为只和`Button`有关，而外观则由`Button`和`ButtonState`共同确定。其规则如下：

* 如果`ButtonState`的`id`为null或空，那么其外观就是在初始化时指定的那些，和`Button`以及`Button`注册的窗体无关。不会从配置文件覆盖。
* 如果`ButtonState`的`id`不为null且不空：
  * 绑定的`Button`注册给某个窗体`guiWindow`时：
    * 如果`ButtonState`的`clusterID`不为null且非空，则会从该窗体集的全局按钮中加载配置，即从路径`inventories.<buttonState.clusterID>.global_items`中获取；
    * 反之，则从窗体的局部按钮中加载配置，即从路径`inventories.<guiWindow.clusterID>.<guiWindow.windowID>.items`中获取。
  * 绑定的`Button`注册给某个窗体集`guiCluster`时：
    * 不管`ButtonState`的`clusterID`是什么，都会从注册窗体集的全局按钮中加载配置，即从路径`inventories.<guiCluster.clusterID>.global_items`中获取。
* **注意：`Button`的`id`不会用于路径中，不会影响配置，且和`ButtonState`的`id`无关。**`Button`的`id`是窗口逻辑中的`id`，`ButtonState`的`id`是构建逻辑中的`id`。

---

再说一下配置文件的格式：

```json
{
    // 语言文件版本
    "version": "...",
    // 语言文件作者，String或者String数组
    "author": "...",
    
    // InventoriesAPI用
    "inventories": {
        // 默认(无名)窗体组，一般用于全局
        "none": {
            // 窗体集的全局信息
            "global_messages": {},
            // 窗体组的全局内容
            "global_items": {...}，
            "myWindow": {
                // 窗体标题
                "gui_name": "",
                // 窗体使用帮助
                "gui_help": [],
                // 窗体拥有的信息
                "messages": {}
                // 按钮样式配置
                "item": {
                    "aButton": {
                        "name": "", // 显示名称
                        "help": [], // 帮助信息
                        "lore": []  // 说明
                    },
                    ...
                },
                ...
            },
            ...
        }，
        "my_cluster": {...}
    }，
    ...
}
```

`ButtonState`的`id`可以带`.`，效果就是你可以用这个在`items`或者`global_items`中创建更深的结构。

---

以上是按钮的初始化，然后是窗体的初始化，窗体需要继承`guiWindow`虚基类，实例化时调用基类的构造函数，并且要实现`void onInit()`方法，该方法初始化窗口模板，一般在这个方法中注册按钮。

而`guiCluster`无需初始化，只需要使用`InventoryAPI`实例的`getOrRegisterGuiCluster(String)`就可以。

### 交互逻辑

API中注册了窗体集、窗体集中注册了窗体、窗体中注册了按钮，都不是真正显示给玩家的界面，而是一种“模板”，方便在玩家使用的时候能够快速生成一个真正的界面。

每一个`InventoryAPI`实例都维护了一组会话(`guiHandler`实例)，每个用户拥有唯一的会话，交互时利用这个会话来识别和处理交互。

会话中包含一个缓存类，用于持久化储存用户的会话信息，这个缓存存在于内存中，暂时不会保存于磁盘，所以只要不关服，玩家无论是关闭了窗口还是退出了游戏，这个会话依旧在持续保存，在下次可以直接打开上次关闭的窗口。库提供的`CustomCache`基类已包含和GUI持久化相关的内容，可以直接用，但是还是建议继承它实现自己的类，以便在缓存自定义的信息。

---

如何进行页面的交互呢？在按钮的行为中我们一般会对会话的缓存进行一定的修改，然后使用会话对象进行窗口操作：

* `changeToInv`切换至某个窗体(切换至同一个窗体不会增加历史记录)；
* `verifyInventory`验证玩家当前的窗体是不是会话指向的窗体；
* `reloadInv`重载窗体(其实就是不增加历史记录的`changeToInv`，由于`changeToInv`对于同窗体也不会增加历史记录所以这个主要用于覆盖历史记录的浏览)；
* `getPreviousInv`返回上一步/若干步的窗体；

接下来，`changeToInv`这几个会调用`guiWindow`的`update`方法，`update`方法会产生一个`GuiUpdateEvent`事件，然后先后调用`onUpdateSync`同步更新方法和`onUpdateAsync`异步更新方法。所以对于窗口我们只需要实现一下`onUpdateAsync(GuiUpdate)`方法就可以。

* `onUpdateSync`同步更新方法由`update`直接调用；
* `onUpdateAsync`异步更新方法由`update`使用`Bukkit.getScheduler().runTaskAsynchronously`异步调用(若非指定强制同步渲染的话)；
* 所以最好使用异步更新方法以提高并行度。

同时要注意的是，玩家需要`<pluginName>.inv.<clsuterID>.<windowID> (小写化)`权限才能打开对应的窗口。

对于`GuiUpdate`类的说明：使用这个对象进行界面渲染，当`onUpdateAsync`方法返回后会将渲染后的界面展示给玩家。常用方法：

* `getItem`和`setItem`可以将生的`ItemStack`放置或者获取。
* `setButton`放置被注册于窗体、窗体集或其他窗体集的按钮，也可以直接接受一个按钮对象。

---

### 接下来讲一些实现细节

#### ButtonState 物理按钮

实际来讲ButtonState才是所谓的“物理按钮”，其描述了某一个物品栏内的按钮的实际状态。

`ButtonState`的构造函数还是很多的，这里按照参数来做说明：

* `key`是指物理按钮的标识符，用于在配置文件中的存储和按钮的查找，相当于一个uuid。
* 按钮名称在哪里？在语言文件的`inventories.[ClusterID].[WindowID].items.[ButtonKey].name`，如果是窗体组全局的按钮，那么就在`inventories.[ClusterID].global_item.[ButtonKey].name`。
* `presentIcon`是指物理按钮的外观，使用具体物品或者物品名称。
* `customModelData`好像是给自定材质物品使用的，暂时不深究。
* `action`、`render`是按钮用来处理点击、渲染的方法接口，在使用的时候我们需要实现其子类：
 * `ButtonAction` 用于处理点击事件`execute`，具体规范见下。
 * `ButtonRender` 用于渲染按钮`render`，具体规范见下。
 * `ButtonActionRender` 同时实现两个方法。
 * 只需要实现`execute`就可以，如果不定义`render`就会直接使用初始化的`presentIcon`显示。`render`是提供给更自由的渲染策略。
* `clusterID`绑定的Cluster，如果指定，那么按钮就作为Cluster的全局按钮而不是某一个Window的。
* `helpLore` 移到物品上显示的帮助文字。不给的话会去`.help`找。
* `normalLore` 显示的一般文字。不给的话会去`.lore`找。

#### ButtonAction和ButtonRender

`ButtonAction`制定了一个接口：

```java
ItemStack render(
    HashMap<String, Object> values, // 跟插件有关的一些信息
    GuiHandler guiHandler,          // 会话实例
    Player player,                  // 玩家实例
    ItemStack icon,                 // 默认图标
    int slot,                       // 格子位置
    boolean helpEnabled);           // 是否有
```

接口的目的是渲染出一个自定义的`ItemStack`并返回，而不是将其直接放到界面上(这一步由其Caller`Button.appluItem`来做。

---

#### Button 逻辑按钮

物品槽(中的物品)是背包GUI的“控件”，作为按钮、图标、装饰或者空格(填入物品)使用。

Button有五种类型，在`ButtonType`枚举中列出：

* `ButtonType.DUMMY` 假的按钮，作为背景或者图标，点了没有反应。类比图标/文字。
* `ButtonType.TOGGLE` 翻转，就是点选会有两种不同的状态，一般用于开关。类比复选框。
* `ButtonType.NORMAL` 一般按钮，点了就会触发事件。类比按钮。
* `ButtonType.CHOICES` 选择按钮，点击会循环切换几种状态。类比选择器。
* `ButtonType.ITEM_SLOT` 空的物品槽，用户往里放物品(比如自定义合成表用的那个)作为输入。类比输入框。

Button类是一个虚拟类，有如下接口：

* `void init(GuiWindow guiWindow)`
 * 与一个GuiWindow绑定。不需要手动调用。
* `void init(String clusterID, WolfyUtilities api)`
 * 另一种绑定方式，提供ClusterID和api实例。不需要手动调用。
* `boolean execute(GuiHandler guiHandler, Player player, Inventory inventory, int slot, InventoryClickEvent event)`
 * 按钮(激活后)要执行的方法。不需要手动调用。
 * guiHandler是某一用户当前对于Gui交互的Session的一个实例
 * slot是button在界面中的位置(格子的索引，自左到右自上而下)
* `void render(GuiHandler guiHandler, Player player, Inventory inventory, int slot, boolean help);`
 * 渲染在界面上的方法。不需要手动调用。
 
下面来看其继承类：
 
##### ActionButton

能够触发Action的按钮，构造函数：

* `ActionButton(String id, ButtonType type, ButtonState state)`
* `ActionButton(String id, ButtonState state)` 默认为NORMAL按钮

要说明的是这里的`id`和`ButtonStats`的`key`并不一样。可以这样理解，`Button`和`ButtonState`不是一一对应的，一个`ButtonState`可以被用于多个`Button`，反之一个`Button`也可以切换其他的`ButtonState`。所以这个`id`是用来标识`Button`的，注册于`Cluster`或者`Window`中。而不与配置文件挂钩。

##### DummyButton

假按钮，继承于`ActionButton`，一般作为物品栏中不可点选的项：

* `DummyButton(String id, ButtonState state)`
 * 只用这种方法来构造，下面那个是留给其继承类`ChatInputButton`用的。
 * state中的`action`永远不会被执行。
* `DummyButton(String id)`不要用这个

##### ChatInputButton

聊天栏的按钮，继承于`DummyButton`。