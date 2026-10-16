package com.yangchengwei.easytrip.assistant

import kotlinx.serialization.json.*

/** Public DOM only. Unknown layouts fail closed; no page state, cookie, token or private API reads. */
internal object AssistantLinkDom {
    const val ADAPTER_VERSION = 3

    fun script(platform: AssistantLinkPlatform): String = """
        (() => {
          const ctrip = '${platform.name}' === 'CTRIP';
          const excluded = 'aside,[class*="recommend"],[class*="comment"],[class*="avatar"],[class*="advert"]';
          const belongs = e => e && !e.closest(excluded);
          const all = (root, selector) => Array.from(root?.querySelectorAll(selector) || []).filter(belongs);
          const textOf = e => (e?.innerText || e?.textContent || '').trim();
          let note, content, title = '', media = [];
          if (ctrip) {
            content = all(document, '.article-content-container').find(e => !e.querySelector('.article-content-container'))
              || document.querySelector('span.content');
            note = content?.closest('.paipai-detail,.article-container');
            if (note) {
              title = textOf(note.querySelector('.article-title,.article-title-text,.title-content'));
              media = all(note, '.swiper-wapper,.article-image-container,.image-gallery,.image-list');
            }
          } else {
            note = document.querySelector('.narmal-note-container,.note-container,.note-detail-mask');
            content = note?.querySelector('.author-desc-content,#detail-desc');
            title = textOf(note?.querySelector('#detail-title,.note-title'));
            media = all(note, '.image-gallery-container,.note-slider,.note-image-gallery');
          }
          media = media.filter(e => !media.some(other => other !== e && other.contains(e)));
          const body = textOf(content);
          if (!title) title = body.split('\n').find(t => t.trim()) || '';
          const pageText = textOf(document.body);
          const restricted = /verify\.ctrip\.com|\/login(?:[/?]|$)/.test(location.href) ||
            (!body && /拖动滑块|安全验证|内容已删除|笔记不存在|当前笔记暂时无法浏览/.test(pageText));
          const video = all(note, 'video,.video-container,.note-video-container,.video-player').length > 0;
          const totals = new Set();
          const positive = raw => {
            if (/^\d+$/.test(raw || '') && Number(raw) > 0 && Number(raw) <= 100000) totals.add(Number(raw));
          };
          const countText = raw => {
            const t = (raw || '').trim();
            if (t.length > 48) return;
            const fraction = t.match(/^(?:第\s*)?(\d+)\s*[/／]\s*(\d+)(?:\s*张)?$/);
            if (fraction && Number(fraction[1]) > 0 && Number(fraction[1]) <= Number(fraction[2])) positive(fraction[2]);
            else { const count = t.match(/^(?:共\s*)?(\d+)\s*张(?:图片)?$/); if (count) positive(count[1]); }
          };
          for (const area of media) {
            all(area, '.carousel-indicator,.swiper-tip,.swiper-pagination-fraction,.pagination-fraction,.image-count')
              .forEach(e => countText(textOf(e)));
            const fractions = all(area, '.swiper-pagination-total');
            fractions.forEach(e => positive(textOf(e)));
            [area, ...all(area, '[aria-setsize],[data-total],[data-count],[data-total-count],[aria-label]')]
              .forEach(e => {
                for (const attr of ['aria-setsize','data-total','data-count','data-total-count']) positive(e.getAttribute(attr));
                countText(e.getAttribute('aria-label'));
              });
            const bullets = all(area, '.swiper-pagination-bullets .swiper-pagination-bullet');
            if (bullets.length) positive(String(bullets.length));
          }
          const expected = totals.size === 1 ? [...totals][0] : -1;
          const imageSelector = ctrip ? '.swiper-item-image img,.image-item img,.swiper-slide img' :
            '.note-image-container img,.note-image-box img,.swiper-slide img,.slide-item img';
          const elements = [...new Set(media.flatMap(area => all(area, imageSelector)))];
          const images = new Map();
          const ordinals = new Map();
          let pendingImages = 0;
          const identity = raw => {
            if (!raw.trim()) return null;
            try {
              const u = new URL(raw, location.href);
              if (!['https:', 'http:'].includes(u.protocol)) return null;
              const trusted = !u.username && !u.password && (!u.port || u.port === '443') && (ctrip ?
                u.protocol === 'https:' && /^dimg\d+\.c-ctrip\.com$/.test(u.hostname) && /^\/images\//.test(u.pathname) :
                /^sns-webpic(?:-[a-z0-9]+)?\.xhscdn\.com$/.test(u.hostname) &&
                (u.pathname.includes('/notes_pre_post/') ||
                  /^\/\d{12}\/[a-fA-F0-9]{32}\/1040[a-zA-Z0-9]+(?:![a-zA-Z0-9_]+)?$/.test(u.pathname)));
              if (!trusted) return u.href;
              const asset = ctrip ? u.pathname.replace(/^\/images\//,'').replace(/_(?:W|R)_\d+.*$/,'')
                  .replace(/\.(?:jpg|png|webp).*$/,'') :
                (u.pathname.includes('/notes_pre_post/') ? u.pathname.split('/notes_pre_post/')[1] :
                  u.pathname.split('/').pop()).split('!')[0];
              return asset;
            } catch (_) { return null; }
          };
          for (const image of elements) {
            const current = image.currentSrc || image.getAttribute('src') || '';
            const lazy = image.getAttribute('data-src') || image.getAttribute('data-original') || '';
            const raw = lazy && (!current.trim() || /^(?:data:|blob:|about:)/.test(current) || /placeholder|loading\./i.test(current)) ?
              lazy : current;
            const id = identity(raw);
            if (!id) { pendingImages++; continue; }
            if (!images.has(id)) {
              images.set(id, new URL(raw, location.href).href);
              const slide = image.closest('[data-swiper-slide-index],[aria-posinset]');
              const index = slide?.getAttribute('data-swiper-slide-index');
              const position = slide?.getAttribute('aria-posinset');
              ordinals.set(id, /^\d+$/.test(index || '') ? Number(index) + 1 :
                /^[1-9]\d*$/.test(position || '') ? Number(position) : null);
            }
          }
          const reason = !note || !media.length ? 'structure' : !body ? 'body' :
            !totals.size ? 'count' : totals.size > 1 ? 'count_conflict' :
            pendingImages || images.size !== expected ? 'images' : 'ready';
          return JSON.stringify({url:location.href,title,body,images:[...images.values()],expected,
            originalOrdinals:[...ordinals.values()],noteLocated:!!note,
            restricted,video,pendingImages,reason,adapter:$ADAPTER_VERSION});
        })()
    """.trimIndent()

    fun decode(input: AssistantLinkInput, json: JsonObject): AssistantLinkDocument? {
        if (json["restricted"]?.jsonPrimitive?.booleanOrNull == true)
            throw AssistantFailure("笔记需要登录／验证或内容不可用，请改用文字／图片。")
        if (json["video"]?.jsonPrimitive?.booleanOrNull == true)
            throw AssistantFailure("暂不支持视频笔记，请改用文字／图片，原输入已保留。")
        val title = json["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val body = json["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val expected = json["expected"]?.jsonPrimitive?.intOrNull ?: -1
        val images = json["images"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
        if (json["noteLocated"]?.jsonPrimitive?.booleanOrNull != true) return null
        if (title.isBlank() && body.isBlank() && images.isEmpty()) return null
        val ordinals = json["originalOrdinals"]?.jsonArray?.map {
            it.jsonPrimitive.intOrNull?.takeIf { n -> n > 0 }
        }.orEmpty()
        return AssistantLinkDocument(input.platform, json["url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            title, body, images, expected, ordinals, body.isNotBlank())
    }

    fun incompleteFailure(json: JsonObject?): AssistantFailure {
        val reason = json?.get("reason")?.jsonPrimitive?.contentOrNull
        return AssistantFailure(when (reason) {
            "count", "count_conflict" -> "无法确认笔记图片总数，请改用文字／图片，原输入已保留。"
            "images" -> "笔记图片尚未读取完整，请重试或改用文字／图片，原输入已保留。"
            "body" -> "未读取到完整笔记正文，请重试或改用文字／图片，原输入已保留。"
            "structure" -> "暂不支持此笔记页面结构，请改用文字／图片，原输入已保留。"
            else -> "读取笔记超时，请重试或改用文字／图片，原输入已保留。"
        })
    }
}
