# Uploaded APK inventory (static observations)

The supplied `Tessera.apk` contained compiled DEX and packaged resources, not the original source project. DEX string/class-name inspection showed package identity `com.example.tessera` and launcher-related classes including:

- `HomeActivity`, `AppsWindow`, `AppRepo`, `AppEntry`
- `Pager`, `BarView`, `GlowFrame`, `Patterns`, `Look`, `Prefs`
- `CalendarWindow`, `ClockWindow`, `NotesWindow`, `FetchWindow`
- `CommandParser`, `RofiView`, `Fmt`, `Hijri`, `Fonts`
- `WindowFrame`, `Host`, `LayoutCodec`, `LayoutOps`

These are observed names, not a full decompilation or verification of runtime behaviour. This source tree currently reimplements the core HOME/app-discovery/app-drawer path first; secondary surfaces remain to be ported.
