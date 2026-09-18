# Veyra Combat and Utility Foundation - Product Requirements Document (PRD)

## Requirements Description

### Background

- **Business problem:** Veyra already contains many modules, but core combat and utility behavior is inconsistent. Hit Select suppresses useful attacks, BlockHit reacts only after outgoing attacks, Bridge Assist remains slower than intended, and modules do not share safe ownership of inputs or packets.
- **Target users:** Minecraft 1.8.9 players who want a configurable general-purpose client. The product must support both conservative and aggressive module configurations instead of enforcing one preset or server-specific configuration.
- **Value proposition:** Provide reliable, individually configurable modules with behavior comparable in completeness to established clients while keeping Veyra's implementation coherent, testable, and safe from stuck inputs or conflicting packet queues.

### Feature Overview

#### Core features

1. A unified binding system with Toggle, Hold, and Press modes.
2. Top-center notifications for every module state transition caused by a bind.
3. A centralized Attack Controller shared by manual input, AutoClicker, Hit Select, and AutoBlock.
4. A complete Hit Select rewrite with Burst and Criticals behavior.
5. A single AutoBlock module with BlockHit, Predictive, and Blatant modes. Lag behavior belongs to Blatant mode.
6. Faster and more deterministic Bridge Assist and Fast Place implementations.
7. New Auto Rod and No Use Delay modules.
8. Shared input and packet ownership so modules restore physical input correctly and do not flush another module's queued packets.

#### Feature boundaries

- This document defines modules and configurable behavior, not a recommended gameplay profile.
- Legitimate-looking and aggressive behavior are both produced through settings and modes.
- Existing BlockHit behavior remains available as the BlockHit AutoBlock mode.
- Timer Range, Lag Range, Piercing, Auto Weapon, inventory modules, and knockback modules are future work and are not part of the first implementation package.
- No notification is shown for state changes initiated by the GUI or configuration loading.
- Visual notifications are not a replacement for persistent array-list state.

#### User scenarios

- A user binds any module in Toggle mode and receives one enabled or disabled notification per press.
- A user binds a module in Hold mode and receives enabled on press and disabled on release.
- A user clicks rapidly while Hit Select suppresses only attacks that are guaranteed not to damage the current target.
- A user selects conservative AutoBlock settings or switches to Blatant mode with packet-lag behavior.
- A user bridges while Bridge Assist sneaks at the last safe point and releases shortly after a successful placement.
- A user presses or holds an Auto Rod bind and Veyra throws, retracts, and restores the intended hotbar slot.

## Detailed Requirements

### Binding and state transition model

- Bind modes:
  - **Toggle:** Press toggles module state.
  - **Hold:** Press enables and release disables.
  - **Press:** Press invokes one bounded action without leaving a persistent enabled state.
- Every state transition must carry a source:
  - `KEYBIND`
  - `GUI`
  - `CONFIG`
  - `AUTO`
- Notifications are emitted for `KEYBIND` transitions.
- An `AUTO` disable emits a notification when it ends a state that was activated by a bind. The notification includes the reason when one is available.
- GUI and configuration transitions never emit notifications.
- Keyboard and mouse binds must use the same path and edge detection.
- Repeated keyboard events must not cause repeated toggles while a key remains held.

### Notifications

- Position: horizontally centered near the top edge of the game viewport.
- Entry animation: slide down and fade in over approximately 180 ms.
- Hold duration: approximately 1.3 seconds.
- Exit animation: slide up and fade out over approximately 220 ms.
- Enabled and disabled states must be visually distinguishable without relying on text alone.
- The module name and state are always present.
- Up to three notifications may be visible; additional notifications are queued or coalesced.
- Rapid repeated transitions of the same module must not leave stale notifications.
- Hold binds use the same full enabled and disabled notifications as Toggle binds.
- Rendering must scale correctly with Minecraft GUI scale and window resizing.

### Attack Controller

- All attack attempts enter one decision pipeline regardless of their source:
  - physical mouse input;
  - AutoClicker;
  - another Veyra module;
  - an external clicker that reaches Minecraft's normal attack path.
- The controller exposes the current candidate target, attack source, target eligibility, predicted damage-immunity state, and final allow/cancel result.
- Target changes reset target-specific timing without resetting unrelated module settings.
- Cancelling an attack must not cancel movement input, leave mouse state stuck, or delay the next eligible attack.
- AutoBlock is notified only after an attack is accepted or when its predictive logic explicitly requires an incoming-hit block.

### Hit Select

#### Required modes and settings

- Modes:
  - **Burst**
  - **Criticals**
- Common settings:
  - Pause duration
  - Range
  - Weapon only
  - Use server attack time
  - Fake swing
  - Cancel rate in combat
  - Cancel rate for missed swings
- Burst settings:
  - Wait for first hit
  - Hit later in trades
- Criticals settings:
  - Disable during knockback
  - Only while damaged

#### Behavior

- Hit Select must allow the first attack on the first tick where the target is predicted to be damageable.
- A hard-coded spacing delay must not be the primary source of attack eligibility.
- At high input CPS, repeated ineligible attempts must not cause the next eligible attempt to be skipped.
- Burst mode suppresses only attacks predicted to fall inside damage immunity, subject to the configured cancel rate.
- Wait for first hit delays the opening attack until the player receives damage or the configured maximum wait expires.
- Hit later in trades applies only after combat has started and must not persist when the target changes.
- Criticals mode delays attacks while rising and allows them when falling, but bypasses this behavior when critical hits are impossible or inappropriate, including ladders, liquids, flight, web-like obstruction, or being stuck inside a block.
- Server attack time uses observed server-confirmed damage state and is allowed to account for rod, projectile, fire, and other non-melee damage.
- Fake swing affects client animation only and never sends an attack packet.
- Missed-swing filtering never changes whether a valid player hit is allowed.

### AutoBlock

#### Common settings

- Mode: BlockHit, Predictive, or Blatant
- Range
- Chance
- Maximum self hurt time
- Maximum hold duration
- Start delay
- Force block animation
- Conditions:
  - Left mouse button pressed
  - Right mouse button pressed
  - Recently damaged
  - Holding weapon
  - Allow first hit
  - Allow during combos

#### BlockHit mode

- Preserves the useful current behavior: briefly press use-item after an accepted player attack.
- Supports randomized start delay, hold duration, chance, and cooldown.
- Must not overwrite a physically held use-item key.
- On disable, restores the actual physical key state.

#### Predictive mode

- Predicts an incoming melee hit using target distance, target validity, recent combat state, player hurt time, and configured conditions.
- Can block before the first incoming hit when Allow first hit is enabled.
- Releases block early enough that an accepted attack is not delayed.
- Must not activate while eating, drinking, drawing a bow, interacting with a GUI, or using an incompatible item.

#### Blatant mode

- Attempts to keep the player blocked from the server's perspective while combat conditions remain valid.
- Lag is an internal subsystem of Blatant mode, not a separate AutoBlock mode.
- Lag settings:
  - Lag chance
  - Maximum lag duration
  - Prevent delaying attacks
  - Block again immediately
  - Flush before attack
- Prevent delaying attacks takes precedence over continuing a lag window.
- Packet release is scoped to AutoBlock-owned packets and must not flush Blink or FakeLag queues.
- Leaving combat, changing items, opening a screen, changing worlds, dying, or disabling the module flushes or discards owned state safely and restores input.

### Bridge Assist

- Required settings:
  - Edge offset
  - Unsneak delay
  - Randomize
  - Sneak on jump
  - Avoid double-sneaking
  - Select blocks: Never, On depletion, Always
  - Conditions: sneak key pressed, holding blocks, looking down, not moving forward
- Edge detection uses the projected player bounding box and movement direction rather than only the block under the player's center.
- A larger edge offset permits later sneaking and faster bridging.
- Unsneak timing is based on successful block placement when available, with a bounded timer fallback.
- Avoid double-sneaking prevents two sneak cycles for one diagonal placement.
- Disabling Bridge Assist always restores the physical sneak state.
- Safety fallback must prevent falling without applying sneak slowdown throughout the entire traversal of a supported block.

### Fast Place

- Required settings:
  - Maximum CPS
  - Activation delay
  - Blocks only
- Placement cadence is based on elapsed time, not only `rightClickDelayTimer`.
- Repeated placement attempts against the same unchanged target must be rate limited.
- Releasing use-item resets activation-delay state.
- Fast Place and Bridge Assist may cooperate but must not generate duplicate placements.

### Auto Rod

- Supports Toggle, Hold, and Press binding semantics where applicable.
- State flow: select item, wait optional action delay, throw, observe hook/target state, retract on hit or miss, restore slot.
- Required settings:
  - Retract speed
  - Delay between actions
  - Randomize delays
  - Always switch back to weapon
  - Allow snowballs and eggs
  - Throwable speed
  - Disable within melee range
- Auto Rod must restore the previous slot unless configured to select a weapon.
- Manual slot changes cancel or safely rebase the sequence instead of fighting the user.
- World change, death, GUI opening, item depletion, or module disable terminates the sequence safely.
- Auto Rod damage must integrate with Hit Select server attack time.

### No Use Delay

- Removes the client-side delay after completing food consumption or potion drinking.
- Must remain separate from No Hit Delay and No Slow.
- Initial version excludes optional desynchronization abuse.
- Must not continuously reset Fast Place or generic right-click cadence.
- Must restore vanilla behavior immediately when disabled.

### Configuration data

- Existing module settings must migrate without deleting unrelated user configuration.
- New enum values are persisted by stable identifiers rather than display labels.
- Missing settings use safe defaults.
- Invalid or out-of-range values are clamped or replaced with defaults.
- Config loading must not emit notifications or leave temporary module runtime state active.

### Edge cases

- Null player or world during connection and disconnection.
- Player death, respawn, dimension/world change, and server disconnect.
- Opening ClickGUI, inventory, chat, or other screens mid-action.
- Physical key held while a module enables or disables.
- Mouse-button binds represented by negative Minecraft key codes.
- Target death, invisibility, bot/friend/team filtering, or sudden target replacement.
- Rapid module toggling and repeated Hold-bind presses.
- Multiple packet-owning modules enabled at the same time.
- Timer speed changes affecting tick-based durations.
- Low FPS and high or unstable ping.

## Design Decisions

### Technical Approach

- **Architecture:** Add small shared coordinators instead of allowing every module to mutate Minecraft input and packet state independently.
- **Key components:**
  - `BindManager`
  - `ModuleTransition` and transition-source model
  - `NotificationManager` and HUD renderer
  - `InputLeaseManager`
  - `PacketQueueCoordinator`
  - `AttackController`
  - target and weapon context helpers
- **Data storage:** Extend the existing JSON module configuration with stable bind mode and new setting values. Runtime queues, target history, and input leases are never persisted.
- **Interfaces:** Modules request input or packet ownership through scoped handles. Closing a handle restores only state owned by that module.

### Constraints

- **Performance:** No per-frame world-wide entity scans. Combat target selection operates on the current raycast target and a bounded nearby-player set when predictive behavior requires it.
- **Responsiveness:** Bind transitions and notification creation occur during the same client tick as the physical edge event.
- **Compatibility:** Minecraft 1.8.9, Java 17 build, Weave event and mixin architecture, keyboard and mouse binds, variable GUI scale.
- **Safety:** Disabling a module or leaving a world must never leave movement, attack, use-item, sneak, timer speed, or packet queues stuck.
- **Scalability:** Shared coordinators must support future Timer Range, Lag Range, Piercing, Auto Weapon, and inventory automation without rewriting the binding or notification layer.

### Risk Assessment

- **Input conflicts:** Multiple modules may request the same key. Mitigation: reference-counted or priority-aware input leases with physical input as the final restoration source.
- **Packet conflicts:** AutoBlock, Blink, and FakeLag may hold overlapping packet types. Mitigation: owner-scoped queues and explicit release policies.
- **Combat timing errors:** Hurt-time prediction may differ under latency or non-melee damage. Mitigation: optional server attack time and deterministic target-history tests.
- **Mixin fragility:** Attack and item-use hooks may differ from assumed mappings. Mitigation: minimal injections, startup validation, and runtime browser/game verification where possible.
- **Config migration:** Renaming BlockHit to AutoBlock may lose settings. Mitigation: legacy aliases and migration tests.
- **Schedule risk:** Implementing Blatant packet behavior before packet ownership would create rework. Mitigation: complete coordinator infrastructure first.

## Acceptance Criteria

### Functional Acceptance

- [ ] Toggle binds emit exactly one enabled or disabled notification per press.
- [ ] Hold binds emit enabled on press and disabled on release.
- [ ] GUI toggles and config loading emit no notifications.
- [ ] Every attack source passes through the same Attack Controller decision.
- [ ] Hit Select allows the first eligible hit even while the user supplies continuous high-CPS input.
- [ ] Hit Select resets target-specific delay when the target changes.
- [ ] Fake swing never sends an attack packet.
- [ ] AutoBlock exposes BlockHit, Predictive, and Blatant modes.
- [ ] Blatant mode contains Lag settings and never flushes packets owned by Blink or FakeLag.
- [ ] Disabling AutoBlock restores physical use-item input.
- [ ] Bridge Assist releases sneak after successful placement and does not remain slowed over supported blocks.
- [ ] Diagonal bridging does not trigger duplicate sneak cycles when Avoid double-sneaking is enabled.
- [ ] Fast Place respects configured CPS and activation delay.
- [ ] Auto Rod completes select, throw, retract, and slot restoration flows for Press and Hold usage.
- [ ] No Use Delay affects post-consumption delay without changing No Slow, No Hit Delay, or Fast Place behavior.

### Quality Standards

- [ ] Unit tests cover bind edges, transition sources, notification eligibility, combat timing decisions, and config migration.
- [ ] State-machine tests cover AutoBlock and Auto Rod cancellation on disable, death, screen open, and world change.
- [ ] Packet ownership tests prove one module cannot release another module's queue.
- [ ] Manual runtime verification covers low/high CPS, keyboard/mouse binds, Hold mode, target switching, and variable GUI scale.
- [ ] A full Gradle build and test run succeeds on JDK 17.
- [ ] Final code review finds no stuck-input, stale-state, or unrelated-config regression.

### User Acceptance

- [ ] Hit Select no longer feels as if it stops useful hits during continuous clicking.
- [ ] Existing BlockHit behavior remains available and recognizable.
- [ ] Conservative and aggressive configurations can be created without code changes.
- [ ] Notifications are smooth, readable, top-centered, and appear only for bind-driven state transitions.
- [ ] Module descriptions explain modes and settings without prescribing one configuration.

## Execution Phases

### Phase 1: Shared Foundation

**Goal:** Establish safe state, input, packet, and notification infrastructure.

- [ ] Implement transition sources and Toggle/Hold/Press bind modes.
- [ ] Implement notification queue and renderer.
- [ ] Implement input leases and packet ownership.
- [ ] Add focused unit tests and configuration migration.
- **Deliverables:** Shared coordinators, notification system, bind migration, tests.
- **Estimated time:** 2-4 working days after the existing dirty worktree is reconciled.

### Phase 2: Combat Rewrite

**Goal:** Replace attack filtering and deliver all AutoBlock modes.

- [ ] Implement Attack Controller.
- [ ] Rewrite Hit Select with Burst and Criticals modes.
- [ ] Move current BlockHit behavior into AutoBlock BlockHit mode.
- [ ] Implement Predictive and Blatant/Lag behavior.
- [ ] Verify AutoClicker, manual clicking, rods, Blink, and FakeLag interactions.
- **Deliverables:** Hit Select rewrite, three-mode AutoBlock, combat timing tests.
- **Estimated time:** 4-7 working days.

### Phase 3: Block and Utility Modules

**Goal:** Improve bridging and add requested utility modules.

- [ ] Rewrite Bridge Assist edge and placement state machine.
- [ ] Replace Fast Place delay-only behavior with CPS scheduling.
- [ ] Implement Auto Rod.
- [ ] Implement No Use Delay.
- **Deliverables:** Faster Bridge Assist, bounded Fast Place, Auto Rod, No Use Delay.
- **Estimated time:** 4-6 working days.

### Phase 4: Integration and Release

**Goal:** Validate the package as one coherent Veyra release.

- [ ] Run automated build and tests.
- [ ] Perform in-game scenario matrix verification.
- [ ] Review configuration migration and descriptions.
- [ ] Conduct code-quality review.
- [ ] Build and install a test artifact into Moonrise.
- **Deliverables:** Verified JAR, migration notes, updated module documentation.
- **Estimated time:** 2-3 working days.

---

**Document Version:** 1.0
**Created:** 2026-08-12
**Clarification Rounds:** 3
**Quality Score:** 94/100
