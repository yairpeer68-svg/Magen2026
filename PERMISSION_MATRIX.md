# Magen v4.7 Permission Matrix

Magen requests only capabilities used by enabled product features. Android does not permit a normal application to silently grant every permission.

| Capability | Android mechanism | First-launch flow | Required |
|---|---|---|---|
| Notifications | Runtime permission (Android 13+) | Dialog | Recommended |
| Draw over other apps | Special App Access | Settings | Core |
| Local VPN | VpnService consent | System dialog | Core |
| Usage access | AppOps / Special App Access | Settings | Recommended |
| Fine/coarse location | Runtime permission | Dialog | Optional, geofence |
| Background location | Staged permission / app settings | Settings | Optional, geofence |
| Ignore battery optimization | Special App Access | System dialog | Recommended |
| OEM autostart | Vendor settings | Settings | Recommended where applicable |
| Install verified updates | Unknown app source special access | Settings | Recommended for OTA |
| Device Admin | DeviceAdmin activation | System dialog | Core |
| Accessibility | Accessibility Settings | Settings | Core |
| Device Owner | Provisioning capability | ADB/QR/MDM only | Optional strongest mode |
| QUERY_ALL_PACKAGES | Manifest/policy permission | No runtime dialog | Used for installed-app protection |

Not requested without a real feature dependency: microphone, camera, contacts, SMS/call logs, broad file storage.
