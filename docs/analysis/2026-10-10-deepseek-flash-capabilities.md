# deepseek-flash：图片理解与工具调用核验

核验日期：2026-10-10。范围：用户指定的 DeepSeek 官方中文 API 文档。只读取公开文档，未使用用户 API key、未发起模型计费请求、未上传图片、未改变 App 或架构方案。

## 后续真实验证（2026-10-10 23:13，Asia/Shanghai）

用户配置官方 endpoint/key 后，独立探针已完成 **4 次真实请求（全部 HTTP 200）**：文本与合成截图各一条“工具提议 → Fake 搜索结果 → 最终输出”闭环。请求和响应模型均为 `deepseek-flash`，thinking 显式关闭，非流式。

截图用例的文本上下文不含目标城市和名称，未前置 OCR；模型从图片正确提取杭州/西湖、未查询排除项。两条最终输出都回显只在工具结果中出现的随机校验码，并明确尚未收藏。这是当前凭据对选定组合的实际证据，不是普遍准确率、真实地图查询或 Android 接通证据。详细记录见[Provider 协议验证](../testing/v3.0-agent-provider-protocol-validation.md)。

**以下为本次 live smoke 之前的公开文档调研原记录**；其中“未实测/未使用 key”等表述仅属于当时阶段，不覆盖上述最新验证。

## 结论

**deepseek-flash 同时支持图片理解与 Tool Calls。** 当前模型能力表将其映射到 DeepSeek-V4.1-Flash，并分别标记两项能力为支持。旧别名 deepseek-v4-flash 和 deepseek-v4-flash-vision-exp 仍被接受，但由新 Flash 模型承接；新接入使用 deepseek-flash。[S1]

| 核验项 | 结论与证据层级 |
| --- | --- |
| 图片输入与理解 | 官方明确支持，图像指南提供 deepseek-flash 示例。[S2] |
| 读取截图文字 | 官方图像指南明确列为用途；不代表真实攻略的提取准确率已经验证。[S2] |
| 非思考模式工具调用 | 官方 Tool Calls 指南提供示例。[S3] |
| 思考模式工具调用 | 官方思考指南明确支持，并提供多轮调用示例。[S4] |
| 图片 + tools 同一请求 | Chat Completion 请求契约同时定义 user 图片内容和 tools；据此可设计组合调用。本次未找到完整“图片输入 → 地点工具 → 回传结果”的官方端到端示例，未实测组合效果。[S2][S5] |
| App 自带 key 直连 | 官方给出 Base URL 和 Bearer API key 调用方式，不要求额外自建网关；用户具体 endpoint、账号及模型权限未测。[S6] |

## 图片接入要点

- Chat Completion 中，user 的 content 使用内容块数组；图片可用 image_url（Base64 data URL 或外部 URL），也可引用 Files API 的 file_id。支持 JPEG、PNG、GIF、WebP，格式按文件实际内容识别。[S2]
- 单张 Base64/外部 URL 图片上限 32 MiB，请求体上限 48 MiB；Base64 编码会占用请求体。Files API 图片有不同限额，不应把原文件大小等同请求大小。[S2]
- 图片单边最长 8192 像素；单请求达到 15 张及以上时为 4096 像素。移动端设计应按任务裁剪/缩放并控制批量，而不是直接使用服务上限。[S2]
- 建议首期把截图放在 user 消息。图像指南的“图片仅支持 user”与 Chat Completion schema 中“tool 内容可携带图片”的说明存在口径差异；不依赖工具消息返回图片，相关能力单独实测或询问厂商。[S2][S5]

## 工具调用接入要点

- 模型返回 tool_calls，包括工具名和 JSON 参数；工具由客户端执行，再用 role=tool 和 tool_call_id 回传结果。模型本身不执行 App 函数。[S3]
- 当前思考模式默认开启，effort 默认 high。非思考流程应显式设置 thinking.type=disabled，不能仅根据示例标题猜测默认行为。[S4]
- 思考模式携带 tools 时，后续请求必须完整回传相应 assistant 消息中的 reasoning_content，即使某轮未实际调用工具；缺失可能返回 400。保留 content、reasoning_content 和 tool_calls，不只保存最终文本。[S4]
- tool_choice 的 required 或指定具体工具在思考模式下不支持，会返回 400；强制工具调用的协议测试应先关闭思考模式。auto 可以用于正常工具循环。[S5]
- 官方明确提醒模型可能输出无效 JSON 或 schema 外参数，App 仍需自行校验。参数合法不等于用户授权；正式收藏继续由本地确认与业务事务控制。[S5]

## 对 Easy Trip 的影响（设计建议，不是实现结果）

1. 可以将 deepseek-flash 作为首个 provider 候选：截图直接交给模型提取地点，模型请求 App 的地点搜索工具；OCR 作为隐私/兼容/识别失败时的备用路径，不再是必需前置模块。[依据 S1–S3]
2. 模型输出名称不能代替真实地图坐标；继续通过地图 POI 消歧、用户选择和本地提交。
3. ProviderAdapter 应声明图片与工具能力，并正确处理 thinking、reasoning_content、tool_call_id 和工具参数；不能只做最简单的纯文本聊天适配。[依据 S2–S5]
4. 建议首轮协议 smoke 显式关闭思考模式，验证“截图 → tool_calls → 模拟工具结果 → 最终输出”；之后再分别测试开启思考、流式、错误参数与取消恢复。该测试需另行取得安全配置的凭据，当前没有执行。
5. 当前结论只适用于官方文档定义的模型；第三方同名模型或兼容 endpoint 需独立验证。图片准确率、地点匹配正确率、延迟与实际费用均未得出实测结论。

## 官方来源

- [S1 模型与价格：模型能力表](https://api-docs.deepseek.com/zh-cn/quick_start/pricing)
- [S2 图像理解](https://api-docs.deepseek.com/zh-cn/guides/vision)
- [S3 Tool Calls](https://api-docs.deepseek.com/zh-cn/guides/tool_calls)
- [S4 思考模式](https://api-docs.deepseek.com/zh-cn/guides/thinking_mode)
- [S5 对话补全 API](https://api-docs.deepseek.com/zh-cn/api/create-chat-completion)
- [S6 首次调用 API](https://api-docs.deepseek.com/zh-cn/)

## 核验记录与限制

- 六份官方页面经宿主网络直接读取，保存原始内容、正文抽取与 SHA256；临时证据目录 `/tmp/easy-trip-deepseek-flash-20261010/`。Jina 阅读代理超时后切换官方源读取，不用搜索摘要代替正文。
- 网页标题含属性，初次解析标题的正则失败；原始页面和正文实际已下载，修正解析后复核模型表、示例和参数限制。
- 独立复核子任务因服务连接中断未产出结果；上述结论来自主任务逐页核验，不宣称双人复核通过。
- 当前分支为 codex/v3-agent-place-intake；本次只新增本调研记录，不修改技术架构/UI/产品代码，不推送远端。撤销时仅移除本次新增记录或反向提交，不修改既有文件。
