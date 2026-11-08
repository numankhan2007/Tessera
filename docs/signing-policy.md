# Tessera signing policy

## Debug/test builds

- CI debug APKs are signed with `app/tessera-debug.keystore`.
- This key uses the conventional Android debug alias/passwords and is deliberately public in this repository.
- It exists only so test APKs from later CI runs can update an installed Tessera debug build without an uninstall.
- The debug application ID is `com.example.tessera.debug`.
- Do not use this keystore for production, Play Store, customer distribution, or any identity-sensitive application.

## First migration to the stable test key

Older Tessera CI artifacts used the runner's temporary debug keystore. Android does not permit an in-place update when the signer changes.

1. In the old build, export a workspace backup from Workspace options.
2. Uninstall the old temporary-signed debug APK.
3. Install the new stable-signed debug APK.
4. Restore the backup. Android widget bindings must be added again because their IDs and grants are local to the previous installation.

After this one-time transition, future builds using the repository's stable debug key can update in place.

## Production release

A production release must use a new user-owned signing key stored outside source control (for example, a protected CI secret or secure signing service). Configure a separate release signing config. Never reuse `tessera-debug.keystore`.
