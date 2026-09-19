# easy-trip 项目约定

## Git 推送

- 在功能分支开发和提交；本项目始终推送到 `origin/main`，使用 `git push origin HEAD:main`。
- 推送前获取远端最新 `main`，确认 `origin/main` 是当前 `HEAD` 的祖先；仅允许快进推送，禁止强制推送。
- 推送后回读远端 `refs/heads/main`，确认提交哈希与本地 `HEAD` 一致。

## 飞书文档

- 本项目未指定目录时，飞书文档默认放入：
  https://bytedance.larkoffice.com/drive/folder/JIpZfzZMNlZfvNdMwrvc3nf4nxg

## Agent skills

### Issue tracker

Issues are tracked as local Markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Use the five default canonical triage labels. See `docs/agents/triage-labels.md`.

### Domain docs

Use the single-context domain documentation layout. See `docs/agents/domain.md`.
