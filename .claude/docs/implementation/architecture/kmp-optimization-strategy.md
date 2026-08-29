# KMP Compiler Plugin Optimization Strategy (superseded)

> **Status**: Superseded — see
> [metadata-producer-fir-emission.md](metadata-producer-fir-emission.md)
> **Last Updated**: July 2026

## Why this document was rewritten

The previous revision of this file described an `enabledPlatforms: SetProperty<KotlinPlatformType>`
extension API for selecting which KMP compilations run Fakt. **That API never shipped** — it exists
in no released `FaktPluginExtension`, and the design it described was replaced by the cache-correct
`FaktGenerateTask` architecture (issue #79).

## Where the current design lives

| Concern | Current answer | Doc |
|---|---|---|
| Avoiding redundant per-target FIR validation | One `faktGenerateMetadataCommonMain` producer analyses commonMain once; platform compilations consume its `fir-metadata.json` / generated sources | [metadata-producer-fir-emission.md](metadata-producer-fir-emission.md) |
| Which compilations get which treatment | Routing table in `FaktGradleSubplugin.cacheCorrectDecision` (producer / consumer / suppress / legacy-hybrid) | [metadata-producer-fir-emission.md](metadata-producer-fir-emission.md) §8 |
| KMP projects without a JVM/Android target | Covered — the producer drives `KotlinMetadataCompiler`, which needs no JVM classpath | [metadata-producer-fir-emission.md](metadata-producer-fir-emission.md) §2 |
| Build-cache correctness of generated fakes | Generated `.kt` files are declared task outputs of `FaktGenerateTask` | [metadata-producer-fir-emission.md](metadata-producer-fir-emission.md) §7 |
| Overall plugin architecture | Two emit phases (FIR for the worker paths, IR for the legacy in-process path), two drivers | [ARCHITECTURE.md](ARCHITECTURE.md) |

Everything above is selected by `fakt.useExperimentalGenerateTask`, which now defaults to `true`
(backlog P8, landed). Removing the legacy in-process path (P9) is still tracked as a follow-up to
the issue #79 branch; until then, `false` opts back into it.

## Still-valid background from the original document

- The redundancy problem it described was real: without routing, Fakt's FIR checkers re-validated
  every `@Fake` declaration once per KMP compilation target.
- `CommonConfigurationKeys.METADATA_KLIB` is the correct, stable API for detecting metadata
  compilations from inside the compiler (`PLATFORM_KIND` / `IS_METADATA_KLIB` do not exist;
  `CommonPlatforms.defaultCommonPlatform` is deprecated at `ERROR` level).
- The FIR metadata cache (`fir-metadata.json`, producer/consumer modes) survived into the current
  design unchanged — the producer writes it, platform consumers read it.
