# Lab2Wear (Lab-to-Wearable Synchronization Platform)

Lab2Wear is an open, modular synchronization platform developed by the **Wearable Robotic Systems (WRS) Lab at Stevens Institute of Technology** to time-align **BLE-enabled wearable devices** with **TTL-enabled gait-lab instrumentation**.

The system consists of:
- an **Android app** (phone as the primary time reference + wearable integration extensions), and
- a **Raspberry Pi–based Sync Box** (TTL interface + phone-clock alignment)

Lab2Wear is designed to enable accurate synchronization between **off-the-shelf wearables** and **lab instruments** in human movement studies, allowing researchers to combine real-world wearable signals with lab-grade reference events.

---

## Why Lab2Wear

Synchronizing wearables to lab instrumentation is frequently limited by:
- device-specific clock drift and opaque vendor timebases,
- non-uniform SDK timestamp semantics,
- lack of a common time reference spanning BLE wearables and TTL instrumentation.

Lab2Wear addresses this by using the **phone clock as a unifying timebase**, and by providing:
- a **TTL↔phone-clock synchronization path** via the Sync Box, and
- a **wearable↔phone-clock synchronization path** via wearable-specific Android extensions.

---

## High-level architecture

**TTL Instrument** → (TTL pulse/event) → **Sync Box (RPi/Linux SBC)** → (sync protocol) → **Android phone clock**  
**Wearable** → (BLE + vendor SDK/API) → **Android app** → (timestamps mapped to phone clock)

**Outcome:** wearable signals and TTL events can be placed on a shared time axis for downstream analysis.

> Note: Practical accuracy depends on the specific wearable, SDK timestamp semantics, BLE behavior, and instrumentation setup. Lab2Wear provides the framework and tooling to achieve accurate alignment and to characterize synchronization performance for your configuration.

---

## Key features

- **Open and extensible** design (Android + Linux SBC)
- Sync Box supports **any TTL-capable instrument** (via standard digital I/O interface hardware)
- Android app includes a **library of wearable “extensions”** to integrate vendor SDKs/APIs
- Separation of concerns:
  - Sync Box: TTL capture + phone synchronization
  - App: wearable integration + timestamp mapping + data handling
- Designed for research workflows: repeatable setup, logging, and validation hooks

---

## Repository structure

- `android/`  
  Android app + wearable extension library (BLE + vendor SDK/API integrations)

- `syncbox/`  
  Raspberry Pi / Linux SBC services (TTL capture, phone sync protocol, logging)

- `hardware/`  
  PCB Schematics, BOM, wiring diagrams, enclosure CAD, and build notes for the Sync Box

- `docs/`  
  Setup guides, supported device notes, protocol documentation, validation procedures


---

## Getting started

### 1) Prerequisites

**Hardware**
- Raspberry Pi
- TTL interface hardware (SyncBox PCB)

**Software**
- Android Studio (for Android app development/build)
- Linux development environment for the Sync Box (C++)
- Vendor wearable SDKs/APIs (for your target device)

### 2) Build & run (overview)

**Sync Box**
1. Assemble the Sync Box hardware (see `hardware/`).
2. Install the Sync Box software (see `syncbox/`).
3. Connect Sync Box digital input to your instrument’s TTL output.

**Android app**
1. Build and install the Android app from `android/`.
2. Enable the wearable extensions relevant to your device(s).
3. Pair/connect wearables and verify data acquisition.

**Synchronization**
1. Start the Sync Box service.
2. Launch the Android app and connect to the Sync Box.
3. Trigger a test TTL pulse and confirm the event appears in the app logs.
4. Run a short validation recording before collecting study data (see `docs/validation.md` if available).

---

## Supported instruments and wearables

Lab2Wear is designed to be **instrument-agnostic** (any TTL event source) and **wearable-agnostic** (via extensions).  
Supported devices depend on available SDKs/APIs and integration status.

- TTL Instruments: *Any device providing TTL events (e.g., motion capture sync outputs, force plate triggers, EMG systems, treadmill controllers, etc.)*
- Wearables: *Off-the-shelf BLE wearables supported through Android extensions*

See:
- `docs/instruments.md` 
- `docs/wearables.md`

---

## Validation and benchmarking

Synchronization should be validated for your specific configuration. Recommended validation approach: Please refer to:
*****<BioRob2026 Bib entry Here>******

---

## Licensing

This repository may contain multiple artifact types (software, hardware design files, documentation). A common approach is:
- **Software:** Apache-2.0 (permissive + patent grant)
- **Hardware design files:** CERN-OHL (P/W/S depending on your reciprocity goals)
- **Documentation:** CC BY 4.0

See `LICENSE` and subfolder licenses (e.g., `hardware/LICENSE`, `docs/LICENSE`) for the terms that apply.

---

## How to cite

If you use Lab2Wear in academic work, please cite:

*****<BioRob2026 Bib entry Here>******

**BibTeX (template):**
```bibtex
@inprocedings{lab2wear,
  author  = {{WRS Lab, Stevens Institute of Technology}},
  title   = {Lab2Wear: Lab-to-Wearable Synchronization Platform},
  year    = {YYYY},
  url     = {https://github.com/<org-or-user>/lab2wear},
  version = {v0.0.0}
}
