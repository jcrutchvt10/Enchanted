# OpenChamber UI – Design & Feature Mapping

This document maps the most important UI concepts from the **OpenChamber** React code‑base to the corresponding Jetpack Compose composables that will be implemented in the **Enchanted** Android app.

The goal is to preserve the user experience and functional behaviour while re‑implementing the UI in Kotlin/Compose.

---

## 1. Core Application Entry

| React File | Description | Target Compose Screen |
|------------|-------------|-----------------------|
| `src/main.tsx` | Root of the web app – mounts the `<App />` component and provides global providers (theme, runtime API, etc.). | `MainActivity` – sets up `EnchantedApp` composable with `MaterialTheme` and top‑level navigation. |

---

## 2. Global Contexts & Providers

| React File | Context Purpose | Compose Equivalent |
|------------|----------------|-------------------|
| `contexts/ThemeSystemContext.tsx` | Manages light/dark theme and colour palette. | `CompositionLocalProvider` with `LocalEnchantedColors` and `LocalEnchantedTypography`. |
| `contexts/RuntimeAPIProvider.tsx` | Exposes runtime API (e.g., file system, git) to the UI. | Kotlin `object RuntimeApi` injected via Hilt/DI and accessed through `remember { RuntimeApi }`. |
| `contexts/FireworksContext.tsx` | Handles visual fireworks animation state. | `MutableStateFlow<FireworksState>` observed with `collectAsState`. |
| `contexts/DrawerContext.tsx` | Controls side‑drawer open/close state. | `MutableState<Boolean>` stored in a `DrawerState` composable. |
| `sync/sync-context.tsx` | Central sync manager – session materialisation, queueing, debounce, and status. | `SyncViewModel` (Jetpack ViewModel) exposing `StateFlow<SyncState>` and functions `enqueueMaterialization()`. |

---

## 3. Navigation & Layout

| React File | UI Role | Compose Target |
|------------|--------|----------------|
| `components/layout/MainLayout.tsx` | Main app layout with header, sidebars, and content area. | `Scaffold` with `TopAppBar`, `NavigationDrawer`, and `NavHost`. |
| `components/layout/Sidebar.tsx` | Project/file tree navigation. | `LazyColumn` inside a `ModalDrawer` – `ProjectTreeScreen`. |
| `components/layout/RightSidebar.tsx` | Context‑specific panels (e.g., Git, Settings). | Separate composables displayed via `ModalBottomSheet` or `NavigationRail`. |
| `components/layout/Header.tsx` | Top bar with actions, status, and search. | `TopAppBar` with `IconButton`s and `DropdownMenu`. |

---

## 4. Sync Queue & Materialisation

| React File | Feature | Compose Implementation |
|------------|---------|----------------------|
| `sync/sync-context.tsx` (functions `enqueueSessionMaterialization`, `pendingSessionMaterializations`) | Queues a directory for materialisation, debounces rapid changes, tracks cooldown. | `SyncViewModel.enqueueMaterialization(dir: String, sessionId: String)` using `MutableStateFlow` and `debounce` from Kotlin Coroutines. |
| `components/chat/ChatInput.tsx` (queue handling) | Allows user to queue messages before sending. | `MessageQueueScreen` with `LazyColumn` of `QueuedMessageItem` and a `SendAllButton`. |

---

## 5. Voice Interaction

| React File | Description | Compose Counterpart |
|------------|-------------|--------------------|
| `components/voice/VoiceProvider.tsx` | Provides microphone access and voice‑to‑text state. | `VoiceViewModel` with `MutableStateFlow<VoiceState>` and `AudioRecord` integration. |
| `components/voice/VoiceStatusIndicator.tsx` | UI badge showing listening/processing status. | `VoiceStatusChip` composable observing `VoiceState`. |
| `components/voice/BrowserVoiceButton.tsx` | Button to start/stop voice capture. | `IconButton` calling `VoiceViewModel.toggleListening()`. |

---

## 6. Session / Project Sidebar

| React File | Purpose | Compose Mapping |
|------------|---------|----------------|
| `components/session/sidebar/SidebarProjectsList.tsx` | Lists open projects/workspaces. | `ProjectListScreen` with `LazyColumn` of `ProjectItem`. |
| `components/session/sidebar/SessionNodeItem.tsx` | Represents a file/folder node. | `FileNodeItem` composable with expandable `TreeItem`. |
| `components/session/sidebar/SessionGroupSection.tsx` | Groups sessions by type (e.g., Git, Local). | `SessionGroupScreen` using `SectionHeader` and `LazyColumn`. |
| `components/session/sidebar/SidebarFooter.tsx` | Footer actions (new project, settings). | `DrawerFooter` composable with `Button`s. |

---

## 7. Usage & Metrics Panels

| React File | UI Element | Compose Equivalent |
|------------|------------|-------------------|
| `components/sections/usage/UsagePage.tsx` | Shows usage statistics, progress bar, and pacing. | `UsageScreen` with `LinearProgressIndicator` and `Text` for metrics. |
| `components/sections/usage/UsageProgressBar.tsx` | Visual progress bar for token usage. | `LinearProgressIndicator` bound to `UsageViewModel.usageRatio`. |
| `components/sections/usage/PaceIndicator.tsx` | Displays current request/response pace. | `PaceIndicator` composable with animated `CircularProgressIndicator`. |

---

## 8. Git & Version Control Views

| React File | Feature | Compose Counterpart |
|------------|---------|--------------------|
| `components/views/git/HistorySection.tsx` | Commit history list. | `GitHistoryScreen` with `LazyColumn` of `CommitItem`. |
| `components/views/git/CommitInput.tsx` | Input for new commit message. | `CommitMessageInput` composable with `OutlinedTextField`. |
| `components/views/git/BranchSelector.tsx` | Branch selection dropdown. | `BranchDropdown` using `ExposedDropdownMenuBox`. |
| `components/views/git/SyncActions.tsx` | Buttons for pull/push, stash, etc. | `GitActionBar` with `IconButton`s invoking `GitViewModel`. |

---

## 9. Miscellaneous UI Components

| React Component | Description | Compose Replacement |
|------------------|-------------|--------------------|
| `components/ui/button.tsx` | Generic button component. | `Button` from Material3 with theming. |
| `components/ui/dialog.tsx` | Modal dialog wrapper. | `AlertDialog` composable. |
| `components/ui/slider.tsx` | Slider control. | `Slider` composable. |
| `components/ui/tooltip.tsx` | Hover tooltip. | `TooltipArea` from Accompanist. |
| `components/ui/alert.tsx` | Inline alert banner. | `SnackbarHost` with `Snackbar`. |

---

## 10. Mapping Summary

The table above provides a **one‑to‑one mapping** from the original React component hierarchy to the Jetpack Compose architecture we will build.  Each entry includes:

1. **React source** – the file or component that implements the feature.
2. **Purpose** – a short description of what the UI does.
3. **Compose target** – the name of the new composable or ViewModel that will replace it.

Implementation will follow the standard Android MVVM pattern:

* **ViewModel** – holds state (`StateFlow`/`LiveData`).
* **Composable** – renders UI, collects state via `collectAsState()`.
* **DI** – Hilt will provide singleton services (e.g., `RuntimeApi`, `SyncManager`).

The first concrete composable to be built is **`SyncQueueScreen`**, which mirrors the sync‑queue logic from `sync-context.tsx`. Subsequent screens will be added iteratively, respecting navigation order defined in `MainLayout.tsx`.

---

*Document generated on $(date).*