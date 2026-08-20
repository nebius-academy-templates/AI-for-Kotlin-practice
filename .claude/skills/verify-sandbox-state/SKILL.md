---
name: verify-sandbox-state
description: >-
  Enable, disable, reset and verify one of the six ride-hailing sandbox states.
  Covers PowerShell on Windows and bash on macOS and Linux. Use when asked to
  reproduce a controlled backend or region condition or confirm that a state
  really took effect.
---

# Verify a sandbox state

The six state IDs are `slow_backend_response`, `backend_error`,
`car_unavailable`, `driver_not_found`, `intermittent_backend_delay` and
`region_unavailable`. `ConditionConfig.ALL` is the source of truth.

Start the app or Appium session before enabling a state. Session creation
relaunches the app process and clears in-memory state. Use the receiver
component with `-n`, then verify the observable effect; a successful broadcast
return code alone is not proof that the state applied.

## Commands

Windows PowerShell:

```powershell
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"

& $adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver `
    --es condition backend_error --ez enabled true

& $adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver `
    --es condition backend_error --ez enabled false

& $adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver `
    --ez reset true
```

macOS or Linux:

```bash
adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver \
    --es condition backend_error --ez enabled true

adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver \
    --es condition backend_error --ez enabled false

adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver \
    --ez reset true
```

The same states are available from the app's Settings screen.

## Observable effects

| State | Expected effect |
| --- | --- |
| `slow_backend_response` | Data requests consistently show a long loading state |
| `backend_error` | Data requests show the corresponding error and retry state |
| `car_unavailable` | The Minivan tariff stays visible but is disabled |
| `driver_not_found` | Ordering returns to the tariff list with no-driver feedback |
| `intermittent_backend_delay` | Some requests receive a large latency spike |
| `region_unavailable` | The unavailable-region banner appears on the map |

Reset all states after verification. Use the UI, API response, elapsed request
time or a UI hierarchy dump to confirm the effect appropriate to the selected
state.
