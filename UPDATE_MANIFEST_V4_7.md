# Signed OTA manifest v4.7

The outer envelope remains `{payload, signature}` with ECDSA-SHA256 over the exact UTF-8 payload string.

Required payload fields:
- `sequence`: monotonically increasing integer; lower previously-seen values are rejected.
- `versionCode`, `versionName`, `url`, `sha256`.
- `channel`: `stable`, `beta`, or `canary`.
- `rolloutPercent`: 0..100, assigned to a persistent random per-installation bucket (no device identifier is hashed into rollout membership).
- `expiresAtEpoch`: optional Unix seconds expiry.

Both current and pre-provisioned next update verification keys are accepted to permit safe signing-key rotation. APK package identity, signer fingerprint and SHA-256 are still verified before Android Package Installer is opened.

Anti-rollback state is stored independently per channel and is advanced before staged-rollout exclusion. Reuse of one sequence number with different signed payload bytes is rejected.
