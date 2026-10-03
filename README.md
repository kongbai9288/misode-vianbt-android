# misode-vianbt-android

把两个 MIT 许可的 Minecraft 上游项目做成可直接嵌入安卓 App 的库：

| 模块 | 上游 | 形态 | 产物 |
|---|---|---|---|
| `vianbt` | [ViaVersion/ViaNBT](https://github.com/ViaVersion/ViaNBT) (MIT) | 零依赖 Android Library | `vianbt-release.aar` |
| `misode-android` | [misode/misode.github.io](https://github.com/misode/misode.github.io) (MIT) | 离线 WebView 组件 | `misode-android-release.aar`（内含完整离线网页包） |
| `misode-web` | 同上 | 移动化改造后的前端源码 | `misode-web-assets.zip` |

两者都保留了上游的 MIT 许可，许可文件分别位于 `vianbt/LICENSE.txt` 与 `misode-web/LICENSE`。

---

## 一、vianbt：NBT 读写库

上游 ViaNBT 依赖 `fastutil`（数 MB）与 JetBrains 注解。本模块已把 `fastutil` 全部替换为等价 JDK 实现，做到**零运行时依赖**：

| 原实现 | 替换实现 |
|---|---|
| `it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap` | `java.util.HashMap` + `getOrDefault` |
| `it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap` | `java.util.HashMap<Integer, …>` |
| `it.unimi.dsi.fastutil.io.FastBufferedInputStream/OutputStream` | `java.io.BufferedInputStream/OutputStream` |
| `it.unimi.dsi.fastutil.ints.IntArrayList` | 自带 `com.viaversion.nbt.internal.IntArrayList`（同样不装箱） |
| `org.jetbrains.annotations.Nullable` | `compileOnly`，不参与 dex |

包名、类名、方法签名与上游完全一致，`minSdk 21`、`compileSdk 35`。

### 用法

```kotlin
import com.viaversion.nbt.io.NBTIO
import com.viaversion.nbt.tag.CompoundTag

// 读（自动识别 GZIP）
val tag = NBTIO.reader().withTagLimiter(TagLimiterImpl(…)).read(file)

// 写
NBTIO.writer().write(tag, file)

// SNBT 互转
val parsed = SNBT.parse("{Count:1b,id:\"minecraft:stone\"}")
val text = SNBT.stringified(tag)
```

引入：`implementation(files("libs/vianbt-release.aar"))`，或把 `vianbt/` 作为 module 依赖：`implementation(project(":vianbt"))`。

---

## 二、misode-android：离线数据包生成器

把 misode 的 **137 个生成器**整站打进 AAR 的 assets，通过 `WebViewAssetLoader` 以 `https://misode.local/` 伪域名离线加载，**完全不联网**。

### 移动端改造

- 输出面板由右侧 40vw 抽屉 → **底部弹出 sheet**
- 预览/工程面板 → 全屏/边缘抽屉，改为浮层覆盖（不再挤压内容）
- 所有可点击元素 **≥44px**；输入框 16px 避免 iOS 聚焦缩放
- `env(safe-area-inset-*)` 刘海屏/手势条适配
- 移除 Google Analytics、EthicalAds、giscus 等外部请求
- 单入口 `index.html`（SPA），配合 `shouldInterceptRequest` 做路由回退
- 按 `spyglass / deepslate / editor / zip / vendor` 分包，首屏更快

### 用法

```kotlin
val misode = MisodeView(context)
container.addView(misode, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))

misode.addListener { event ->
    when (event.type) {
        MisodeEvent.READY -> misode.openGenerator("loot_table")
        MisodeEvent.OUTPUT -> Log.d("misode", event.value ?: "")
        MisodeEvent.TITLE -> toolbar.title = event.value
        else -> Unit
    }
}

// 取生成结果
misode.getOutput { json -> … }
// 协程
lifecycleScope.launch { val out = misode.awaitOutput() }
```

`onDestroyView()` 里调用 `misode.destroy()`。

### 暴露给安卓的 API（`MisodeView`）

**生命周期 / 导航**：`isReady` `reload` `navigate(path)` `openGenerator(idOrUrl)` `goBack()` `getCurrentPath` `getTitle` `destroy` `evaluate(js)`

**目录查询**：`getGenerators` `getVersions` `getLanguages` `getGuides` `getGeneratorHistory` `clearHistory`

**设置**：`getVersion/setVersion` `getTheme/setTheme` `getLanguage/setLanguage` `getFormat/setFormat` `getIndent/setIndent` `getHighlighting/setHighlighting` `getSoundsVersion/setSoundsVersion` `getTreeViewMode/setTreeViewMode` `getColormap/setColormap` `getSettings`

**工程与输出**：`getProjects` `getOpenProject/setOpenProject` `getOutput` `scrapeOutput` `awaitOutput` `awaitGenerators`

**事件**：`addListener/removeListener`，事件类型 `READY / ROUTE / OUTPUT / THEME / VERSION / TITLE / STORE`

这些只是 Kotlin 侧的便捷封装；JS 侧的完整接口挂在 `window.MisodeBridge`，可直接用 `misode.evaluate("MisodeBridge.xxx()")` 调用，运行时用 `listBridgeMethods()` 列出全部方法名。

---

## 三、构建

推送后 GitHub Actions 自动构建，产物在 Actions 运行记录里下载：

- `misode-web-assets.zip` — 离线网页包（可直接放进任意 WebView / assets）
- `vianbt-aar` — NBT 库 AAR
- `misode-android-aar` — WebView 组件 AAR（已内含网页包）

打 tag 会自动发布 Release：

```bash
git tag v1.0.0 && git push origin v1.0.0
```

本地构建：

```bash
cd misode-web && npm install && npm run build:mobile   # 输出到 dist-mobile/
./gradlew :vianbt:assembleRelease
unzip -q misode-web/misode-web-assets.zip -d misode-android/src/main/assets/misode
./gradlew :misode-android:assembleRelease
```

---

## 许可

- `vianbt/` — MIT，© Steveice10 / Nassim Jahnke / ViaVersion
- `misode-web/`、`misode-android/` — MIT，© Misode
- 本仓库的改造部分同样以 MIT 发布
