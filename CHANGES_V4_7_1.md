# Magen Phone v4.7.1 Hardening Fix

- Implemented the missing OTA `rolloutBucket` and ECDSA manifest-verification methods, removing the v4.7.0 compile blocker.
- OTA manifest keys are restricted specifically to secp256r1 (NIST P-256) and signatures use SHA256withECDSA.
- APK downloads must remain on the exact HTTPS origin (host + effective port) of the signed manifest.
- Anti-rollback watermarks are per-channel, persisted before rollout exclusion, and detect conflicting payloads reusing one sequence.
- Rollout membership is a persistent random installation bucket rather than a device-identity-derived value.
- Expanded conservative HTTPS-inspection bypasses for sensitive DNS labels to match server-side defense-in-depth.
- Production release builds now require the visual model, its SHA-256 pin, release signing, and release certificate fingerprint.
- First production Visual AI bootstrap no longer trusts primary/mirror consensus alone; it requires a GitHub-published digest or an independently supplied model SHA-256, then creates a repository-committable lock.
- CI/verifier checks were strengthened so missing OTA helper implementations cannot produce a false PASS.
