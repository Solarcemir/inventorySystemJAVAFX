; ============================================================
;  CFT Electronic - Web Inventory System Installer
;  Architecture: Spring Boot JAR (serves API + React frontend)
;                Database on Supabase (cloud) - no local DB needed
; ============================================================

#define AppName    "CFT Electronic"
#define AppVersion "1.4.0"
#define AppPublisher "CFT Electronic"

[Setup]
AppId={{B7E4C2A1-F3D8-4E90-BC12-8A7D5E6F1234}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={autopf}\CFT Electronic
DefaultGroupName={#AppName}
OutputDir=output
OutputBaseFilename=CFT-Electronic-Setup-v{#AppVersion}
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=admin
DisableProgramGroupPage=yes
; No icon file required — uses system default
UninstallDisplayName={#AppName}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "firewall"; Description: "Allow CFT Electronic through Windows Firewall (required so other PCs can connect on your network)"; GroupDescription: "Network:"

[Files]
; ── Bundled Java JRE (no Java install needed on target PC) ──────────────────
Source: "jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs

; ── Spring Boot JAR (contains the React frontend + backend API) ──────────────
; Run build.bat first to generate this file
Source: "app\backend.jar"; DestDir: "{app}\app"; Flags: ignoreversion

; ── External config (DB connection — can be edited without rebuilding) ───────
Source: "app\config\application.properties"; DestDir: "{app}\app\config"; Flags: ignoreversion

; ── Launcher scripts ─────────────────────────────────────────────────────────
Source: "scripts\start.vbs";  DestDir: "{app}"; Flags: ignoreversion
Source: "scripts\stop.bat";   DestDir: "{app}"; Flags: ignoreversion
Source: "scripts\info.bat";   DestDir: "{app}"; Flags: ignoreversion

[Icons]
; Start Menu
Name: "{group}\Start CFT Electronic";  Filename: "{sys}\wscript.exe"; Parameters: """{app}\start.vbs"""; WorkingDir: "{app}"; Comment: "Start the CFT Electronic server and open browser"
Name: "{group}\Stop CFT Electronic";   Filename: "{app}\stop.bat";  WorkingDir: "{app}"; Comment: "Stop the backend server"
Name: "{group}\Network Info";          Filename: "{app}\info.bat";  WorkingDir: "{app}"; Comment: "Show your LAN address for other PCs"
Name: "{group}\Uninstall {#AppName}";  Filename: "{uninstallexe}"

; Desktop — always created (no task condition)
Name: "{commondesktop}\CFT Electronic"; Filename: "{sys}\wscript.exe"; Parameters: """{app}\start.vbs"""; WorkingDir: "{app}"; Comment: "Start CFT Electronic"
Name: "{userdesktop}\CFT Electronic";   Filename: "{sys}\wscript.exe"; Parameters: """{app}\start.vbs"""; WorkingDir: "{app}"; Comment: "Start CFT Electronic"

[Run]
; Open Windows Firewall for port 8080 (lets other LAN PCs connect)
Filename: "netsh"; Parameters: "advfirewall firewall add rule name=""CFT Electronic"" protocol=TCP dir=in localport=8080 action=allow"; Flags: runhidden; Tasks: firewall

; Launch the app after install
Filename: "{sys}\wscript.exe"; Parameters: """{app}\start.vbs"""; WorkingDir: "{app}"; Description: "Launch CFT Electronic now"; Flags: postinstall nowait skipifsilent

[UninstallRun]
; Stop this app's backend before uninstall (only its own java process, see stop.bat)
Filename: "{app}\stop.bat"; Flags: runhidden; RunOnceId: "StopBackend"

; Remove firewall rule
Filename: "netsh"; Parameters: "advfirewall firewall delete rule name=""CFT Electronic"""; Flags: runhidden; RunOnceId: "RemoveFirewallRule"

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
