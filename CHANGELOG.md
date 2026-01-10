# Changelog

## [0.2.1-techline] - 2026-01-10
- Added protocol telemetry service to capture live energy snapshots and risk backlog metrics in `technology-status.json`.
- Appended social feed entries to `events.jsonl` for long-lived history alongside per-event JSON artefacts.

## [0.2.0-techline] - 2026-01-09
- Integrated manifestation intent protocol scaffolding on Forge side (protocol package, player intent capability, debug command).
- Added server tick driver and capability gating so stage progression requires a valid Manifestation Intent.
- Added core resource items: amethyst powder, quartz powder, and amethyst alloy ingot.
- Switched Stage 0 → 1 progression to crafting-based triggers with capability logging.
- Introduced shapeless placeholder recipes and updated localization (en_us, zh_cn).
- Reserved Stage 3 in the capability to align with the technology line roadmap.
