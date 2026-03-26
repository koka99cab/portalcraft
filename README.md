# Portalcraft

A Fabric mod for Minecraft 1.21.11 in which players build **portal-algorithms**.

The core premise: construct a Nether-like rectangular frame from allowed blocks,
light it with flint-and-steel, and the mod reads the frame's material composition
to derive a **procedural signature** that determines portal colour, parameters,
and destination.

## Status

**Early scaffold.** The build system, source layout, and portal interfaces are in
place. No portal logic has been implemented yet.

## Requirements

| Tool | Version |
|------|---------|
| Java | **21** (required — Minecraft 1.21.11 mandates it) |
| Gradle | 9.2.1 (via wrapper — no manual install needed) |

> **Note on Java version:** Minecraft 1.21.11 requires Java 21. The Gradle wrapper
> handles the build toolchain; you only need a JDK 21+ on your PATH.

## How to run

```bash
# Build the mod jar
./gradlew build

# Run unit tests (pure Java, no Minecraft bootstrap)
./gradlew test

# Launch the Minecraft client with the mod loaded
./gradlew runClient

# Launch a headless Minecraft server with the mod loaded
./gradlew runServer

# Generate Minecraft sources for IDE navigation (recommended after first clone)
./gradlew genSources
```

The compiled jar lands at `build/libs/portalcraft-0.1.0.jar`.

## Sanity check

After `runClient` or `runServer`, open the in-game console and type:

```
/portalcraft ping
```

Expected response: `[Portalcraft] pong!`

## Source layout

```
src/
  main/java/dev/botsu/portalcraft/
    Portalcraft.java                       # ModInitializer entrypoint
    PortalcraftConstants.java              # MOD_ID, logger
    bootstrap/
      PortalcraftCommands.java             # /portalcraft ping command
    portal/
      frame/
        PortalFrameDetector.java           # Interface: scans world for a frame
        PortalFrameValidator.java          # Interface: validates scan result
        PortalFrameScanResult.java         # Value: scan output (blocks, axis, valid)
      signature/
        PortalSignature.java               # Value: (hash, material counts)
        PortalSignatureHasher.java         # Utility: deterministic hash
      activation/
        PortalActivationService.java       # Interface: full activation pipeline
      integration/
        PortalRuntime.java                 # Interface: external integration seam
        VanillaStylePortalRuntime.java     # Default vanilla implementation (stub)
        ExternalPortalRuntime.java         # Placeholder for optional external lib
    mixin/                                 # (empty — ready for future mixins)

  client/java/dev/botsu/portalcraft/client/
    PortalcraftClient.java                 # ClientModInitializer entrypoint

  main/resources/
    fabric.mod.json
    portalcraft.mixins.json
    assets/portalcraft/
      lang/en_us.json
      lang/fr_fr.json
      icon.png
    data/portalcraft/tags/blocks/
      frame_blocks.json                    # Tag: blocks valid as portal frames
      april_fool_frame_blocks.json         # Tag: blocks for April-Fool-style frames

  test/java/dev/botsu/portalcraft/
    portal/
      PortalSignatureHasherTest.java       # Unit test (pure Java, no MC)

  gametest/java/dev/botsu/portalcraft/gametest/
    PortalcraftGameTests.java              # In-game test (run with /test)
```

## Next implementation milestones

1. **Frame detection** — implement `PortalFrameDetector` scanning horizontal/vertical rectangles
2. **Block tag population** — add vanilla full-cube blocks to `frame_blocks.json`
3. **Activation event** — hook `UseItemOnBlockCallback` for flint-and-steel
4. **Portal block** — register a custom portal block with visual/collision
5. **Signature → destination** — map hash to a test dimension or overworld sub-region
6. **Teleportation** — implement `VanillaStylePortalRuntime` transport

## Blocodex

Blocodex is a sibling mod developed in parallel. It provides a curated taxonomy
of Minecraft blocks (shapes, materials, families). Portalcraft may use that
taxonomy's data to enrich portal signatures (e.g., routing wool-based frames to
April-Fool dimensions), but **Blocodex is not a runtime dependency**. There is no
`modImplementation` coupling between the two mods.

## External portal library

`ExternalPortalRuntime` is a placeholder for an optional third-party portal
library. No library with confirmed Minecraft 1.21.11 Fabric compatibility is
known at time of writing. When one is identified, add its version to
`gradle.properties` (`external_portal_lib_version`) and implement
`ExternalPortalRuntime`. The `PortalRuntime` interface is the only coupling point.
