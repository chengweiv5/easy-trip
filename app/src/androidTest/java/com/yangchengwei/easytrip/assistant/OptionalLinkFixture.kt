package com.yangchengwei.easytrip.assistant

/** Production read/prepare contract with deterministic, offline source bytes. */
internal fun optionalLinkFixture(store: AssistantImageStore, text: String, count: Int,
    bytes: ByteArray, onRead: () -> Unit = {}): AssistantLinkLoader {
    val reader = AssistantLinkReader { input ->
        onRead()
        AssistantLinkDocument(input.platform, if (input.platform == AssistantLinkPlatform.XIAOHONGSHU)
            "https://www.xiaohongshu.com/explore/fixture" else input.url, "", text, (1..count).map {
            if (input.platform == AssistantLinkPlatform.CTRIP) "https://dimg04.c-ctrip.com/images/$it.jpg"
            else "https://sns-webpic-qc.xhscdn.com/notes_pre_post/fixture$it"
        }, count)
    }
    val delegate = DefaultAssistantLinkLoader(reader, store, AssistantLinkImageFetcher { _, _ -> bytes })
    return object : AssistantLinkLoader by delegate {
        override suspend fun read(input: AssistantLinkInput, existing: List<AssistantImage>, onProgress: (String) -> Unit) =
            delegate.read(input, existing, onProgress).copy(modelText = text)
    }
}
