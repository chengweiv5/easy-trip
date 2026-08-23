# Easy Trip v1.0 Pencil Reference

## Source
- File: design/easy-trip-v1.0.pen
- Canvas: 390 × 844
- Primary Button: Qt2FO
- Secondary Button: PlOrR
- Filter Pill: E6x4E3
- Icon Button: N7LYWb

## Main Flow Frames
| State | Frame ID | Frame name | Size |
|---|---|---|---|
| 我的旅行 · 默认 | K9h3r | 01 我的旅行 | 390 × 844 |
| 我的旅行 · 空 | zIbEu | 36 我的旅行 · 空状态 | 390 × 844 |
| 组合态：我的旅行 · 加载 | — | 由现有 v1.0 视觉语言组合 | — |
| 组合态：我的旅行 · 错误 | — | 由现有 v1.0 视觉语言组合 | — |
| 创建旅行 · 默认 | dzhkC | 07 创建旅行 | 390 × 844 |
| 创建旅行 · 校验错误 | yIGiQ | 47 创建旅行 · 表单校验 | 390 × 844 |
| 组合态：创建旅行 · 提交中 | — | 由现有 v1.0 视觉语言组合 | — |
| 组合态：创建旅行 · 提交失败 | — | 由现有 v1.0 视觉语言组合 | — |
| 旅行工作台 · 默认 | A9EKX / LFmzR | 02 旅行工作台 · 地点池 / 04 旅行工作台 · 行程 | 390 × 844 |
| 旅行工作台 · 加载 | GoxB6 | 45 地图 · 加载中 | 390 × 844 |
| 组合态：旅行工作台 · 旅行不存在 | — | 由现有 v1.0 视觉语言组合 | — |
| 组合态：旅行工作台 · 地图未授权 | — | 由现有 v1.0 视觉语言组合 | — |
| 旅行工作台 · 地图失败 | U8R5i | 46 地图 · 加载失败 | 390 × 844 |
| 旅行工作台 · 地图图层浮层 | shoPV | 17 旅行工作台 · 地图图层 | 390 × 844 |

## Semantic Tokens
| Compose semantic name | Pencil source node | Resolved value |
|---|---|---|
| background | variable `bg`; K9h3r | #F5F3EE |
| surface | variable `surface`; status bar | #FFFFFF |
| surface soft | variable `surface-soft` | #E7EFE2 |
| primary | variable `primary`; Qt2FO | #2D5E3A |
| primary dark / text primary | variable `primary-dark`; PlOrR label | #1B3A28 |
| secondary | variable `secondary` | #4A6B52 |
| error | variable `danger`; yIGiQ fields/disclaimer | #BA1A1A |
| text secondary | variable `muted` | #6E7F72 |
| divider / border | variable `border`; PlOrR stroke | #D6DDD0 |
| accent | variable `accent` | #D96F3B |
| heading font | variable `font-heading` | Funnel Sans |
| body font | variable `font-body` | Inter |

### Typography

The following roles are grouped from actual text nodes in the nine registered frames.

| Actual role examples | Family | Size | Weight | Line height |
|---|---|---:|---:|---:|
| Page Title | Funnel Sans | 32 | 700 | default |
| Trip Name / Create Trip Title | Funnel Sans | 26 | 700 | default |
| Section Title | Funnel Sans | 20 | 700 | default |
| Day Content Title | Funnel Sans | 18 | 700 | default |
| Map Loading Title | Funnel Sans | 18 | 600 | default |
| Layer Picker Title | Funnel Sans | 17 | 700 | default |
| Workspace Trip Title | Funnel Sans | 16 | 700 | default |
| Trip/list item title | Inter | 16 | 600 | default |
| Primary Button Label / place name | Inter | 15 | 600 | default |
| Field value | Inter | 15 | normal | default |
| Status time / active tab or place label | Inter | 14 | 600 / 700 | default |
| Search hint | Inter | 14 | normal | default |
| Form labels | Inter | 13 | 600 | default |
| Marker/stop labels | Inter | 13 | 700 | default |
| Eyebrow / metadata | Inter | 13 | 500 / normal | default |
| Route summary / sheet labels | Inter | 12 | 600 / 700 | default |
| Subtitle / place metadata | Inter | 12 | normal | default |
| Empty-state description | Inter | 12 | normal | 1.5 |
| Form hint | Inter | 11 | 500 | default |
| Progress/add-day label | Inter | 11 | 600 | default |
| Countdown/progress number | Inter | 11 | 700 | default |
| Planning tip | Inter | 11 | normal | 1.4 |
| Other hints/disclaimer | Inter | 11 | normal | default |
| Mode hint | Inter | 10 | normal | default |
| Layer persistence hint | Inter | 9 | normal | 1.4 |
| Map legend | Inter | 9 | normal | default |

### Shape, Spacing, Size, Elevation
| Element | Pencil source | Resolved value |
|---|---|---|
| Primary / secondary button | Qt2FO / PlOrR | height 48; radius 24; horizontal padding 20 / 18 |
| Filter pill | E6x4E3 | height 36; radius 18; horizontal padding 16 |
| Icon button | N7LYWb | 48 × 48; radius 24; icon 22 × 22 |
| Page content | K9h3r/PQKKt | horizontal margin 20; top 18; bottom 24; section gap 24 |
| Create page sections | dzhkC/Y4VvQS | horizontal margin 20; top 18; bottom 24; gap 22 |
| Trip card | K9h3r/nNCLm | explicit width `fill_container`, no explicit height; resolved bounds 350 × 235 on 390 canvas; radius 20; padding 20; gap 16 |
| Standard list card/row | K9h3r/qibVM | radius 12; vertical padding 12; gap 14 |
| Trip name field · focused/default | dzhkC/PCZ40 | resolved 350 × 52; white fill; #2D5E3A 2px border; radius 10; padding 0 × 14; gap 10 |
| Trip name field · validation error | yIGiQ/yELOO | resolved 350 × 52; white fill; #BA1A1A 2px border; radius 10; padding 0 × 14; gap 10 |
| Travel date row · default | dzhkC/P55E9c | resolved 350 × 66; white fill; #D6DDD0 1px border; radius 10; padding 0 × 14; gap 12 |
| Travel date row · validation error | yIGiQ/QOFBe | resolved 350 × 66; white fill; #BA1A1A 2px border; radius 10; padding 0 × 14; gap 12 |
| Planning Tip | dzhkC/PqiJp | resolved 350 × 40; #E7EFE2 fill; no stroke; radius 10; padding 11 × 12; gap 10 |
| Top/status bar | K9h3r/zOwIj | height 62; horizontal padding 20 |
| Map header | A9EKX/CZB8z | height 52; radius 16; shadow 0 2 8 #00000012 |
| Map search | A9EKX/SDD10 | height 46; radius 23; padding 0 × 14; shadow 0 2 6 #00000010 |
| Bottom sheet | LFmzR/BYAeJ | radius 24 24 0 0; padding 10 20 20; gap 16; shadow 0 -2 12 #00000014 |
| Dialog / floating picker | shoPV/M6PcXB | width 240; radius 14; padding 14; gap 10; shadow 0 6 18 #1B3A2838 |

## Public Components
| Component | Dimensions and shape | Content typography / icon | Observed state relationship |
|---|---|---|---|
| Primary Button `Qt2FO` | height 48; radius 24; horizontal padding 20; #2D5E3A fill | text label: Inter, 15, 600, default font line height; #FFFFFF | K9h3r/GBsVr and dzhkC/DgpPh are `ref: Qt2FO`; yIGiQ/KKCgm is also a `ref: Qt2FO` with instance overrides `fill: #AAB8A9` and label content/fill |
| Secondary Button `PlOrR` | height 48; radius 24; horizontal padding 18; white fill; #D6DDD0 1px border | text label: Inter, 15, 600, default font line height; #1B3A28 | no separate state instance found in target frames |
| Filter Pill `E6x4E3` | height 36; radius 18; horizontal padding 16; #E7EFE2 fill | text label: Inter, 14, 600, default font line height; #2D5E3A | no separate state instance found in target frames |
| Icon Button `N7LYWb` | 48 × 48; radius 24; white fill; #D6DDD0 1px border | no text label; one 22 × 22 `Material Symbols Rounded` icon, `more_horiz`, #1B3A28 | shoPV/eROYt is a visually related independent frame, not an N7LYWb instance; it uses #2D5E3A fill and a white 21px icon |

## Responsive Notes
| Fixed-canvas behavior | Compose behavior |
|---|---|
| Source frames are fixed 390 × 844 phone canvases | Use full available width and preserve 20dp horizontal content padding |
| Content resolves to 350 width on the 390 canvas | Use fill width with 20dp horizontal padding; do not hard-code 350dp |
| Trip card resolves to 350 × 235 but has no explicit height | Preserve content-driven height, padding and gaps rather than hard-coding 235dp |
| Map and sheets occupy full 390 width | Use full-width map; anchor sheet to bottom with 24dp top corners |
| Status/top bar is 62 high in reference | Respect system insets; preserve visual proportions rather than hard-coding device status-bar pixels |
| Fixed overlay coordinates on map | Anchor controls to safe edges and sheet top; avoid coordinates tied to 390px |

## Screenshot Baseline
| Frame ID | Result | MCP evidence |
|---|---|---|
| K9h3r | SUCCESS | `Screenshots taken of nodes: K9h3r` |
| zIbEu | SUCCESS | `Screenshots taken of nodes: zIbEu` |
| dzhkC | SUCCESS | `Screenshots taken of nodes: dzhkC` |
| yIGiQ | FAILED | two explicit calls returned MCP `-32603` |
| A9EKX | SUCCESS | `Screenshots taken of nodes: A9EKX` |
| LFmzR | SUCCESS | `Screenshots taken of nodes: LFmzR` |
| GoxB6 | SUCCESS | `Screenshots taken of nodes: GoxB6` |
| U8R5i | SUCCESS | `Screenshots taken of nodes: U8R5i` |
| shoPV | SUCCESS | `Screenshots taken of nodes: shoPV` |

`Export(..., "png", ...)` returned MCP `-32603` for both single-frame and multi-frame calls, so no local PNG path is claimed.

## Layout Verification

A structural `ctx.problems` pass reported no problems for K9h3r, zIbEu, dzhkC, yIGiQ, LFmzR or shoPV. A9EKX, GoxB6 and U8R5i report intentionally clipped map-art labels/geometry, including fully clipped `灵隐寺`; these are map-art clipping signals rather than collapsed app layout. Screenshot checks succeeded for those three frames.
