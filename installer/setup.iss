; Inventory Management System - Complete Installer
; Includes: Java JRE, PostgreSQL, Backend, Frontend, Database

#define MyAppName "Inventory Management System"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Your Company"
#define MyAppURL "https://yourcompany.com"
#define MyAppExeName "start-all.bat"

[Setup]
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\InventorySystem
DefaultGroupName={#MyAppName}
AllowNoIcons=yes
LicenseFile=
OutputDir=output
OutputBaseFilename=InventorySystem-Setup-v{#MyAppVersion}
SetupIconFile=
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=admin
DisableProgramGroupPage=yes
UninstallDisplayIcon={app}\scripts\{#MyAppExeName}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "spanish"; MessagesFile: "compiler:Languages\Spanish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "quicklaunchicon"; Description: "{cm:CreateQuickLaunchIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked; OnlyBelowVersion: 6.1; Check: not IsAdminInstallMode

[Files]
; Java JRE (Runtime Environment) - DEBE estar en installer\jre\
Source: "jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs

; Backend Application
Source: "backend.jar"; DestDir: "{app}\backend"; Flags: ignoreversion
Source: "application.properties"; DestDir: "{app}\backend\config"; Flags: ignoreversion

; Frontend Application
Source: "frontend.jar"; DestDir: "{app}\frontend"; Flags: ignoreversion
Source: "frontend-lib\*"; DestDir: "{app}\frontend\lib"; Flags: ignoreversion recursesubdirs

; Database
Source: "database-backup.sql"; DestDir: "{app}\database"; Flags: ignoreversion
Source: "postgresql-18.1-2-windows-x64.exe"; DestDir: "{tmp}"; DestName: "postgresql-installer.exe"; Flags: deleteafterinstall

; Scripts
Source: "scripts\*"; DestDir: "{app}\scripts"; Flags: ignoreversion

; Product Images (opcional - comentar si no tienes)
Source: "product-images\*"; DestDir: "{app}\product-images"; Flags: ignoreversion recursesubdirs createallsubdirs; Check: DirExists(ExpandConstant('{src}\product-images'))

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\scripts\start-all.bat"; WorkingDir: "{app}"; IconFilename: "{sys}\shell32.dll"; IconIndex: 21
Name: "{group}\Backend Server Only"; Filename: "{app}\scripts\start-backend.bat"; WorkingDir: "{app}"; IconFilename: "{sys}\imageres.dll"; IconIndex: 73
Name: "{group}\Frontend Client Only"; Filename: "{app}\scripts\start-frontend.bat"; WorkingDir: "{app}"; IconFilename: "{sys}\imageres.dll"; IconIndex: 2
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\scripts\start-all.bat"; WorkingDir: "{app}"; Tasks: desktopicon; IconFilename: "{sys}\shell32.dll"; IconIndex: 21

[Run]
; Instalar PostgreSQL
Filename: "{tmp}\postgresql-installer.exe"; Parameters: "--mode unattended --unattendedmodeui minimal --superpassword admin123 --servicename postgresql-x64-15 --serverport 5432"; StatusMsg: "Installing PostgreSQL Server..."; Flags: waituntilterminated runhidden

; Esperar a que PostgreSQL inicie completamente
Filename: "{cmd}"; Parameters: "/c timeout /t 10"; StatusMsg: "Waiting for PostgreSQL to initialize..."; Flags: runhidden waituntilterminated

; Crear base de datos
Filename: "{cmd}"; Parameters: "/c set PGPASSWORD=admin123 && ""{code:GetPostgreSQLPath}\psql.exe"" -U postgres -c ""CREATE DATABASE inv_db;"""; StatusMsg: "Creating database..."; Flags: runhidden waituntilterminated

; Restaurar backup de base de datos
Filename: "{cmd}"; Parameters: "/c set PGPASSWORD=admin123 && ""{code:GetPostgreSQLPath}\psql.exe"" -U postgres -d inv_db -f ""{app}\database\database-backup.sql"""; StatusMsg: "Restoring database backup..."; Flags: runhidden waituntilterminated

; Mensaje de éxito
Filename: "{app}\scripts\start-all.bat"; Description: "Launch {#MyAppName}"; Flags: postinstall nowait skipifsilent unchecked

[UninstallDelete]
Type: filesandordirs; Name: "{app}"

[Code]
var
  PostgreSQLPath: String;

function GetPostgreSQLPath(Param: String): String;
begin
  if RegQueryStringValue(HKLM, 'SOFTWARE\PostgreSQL\Installations\postgresql-x64-15', 'Base Directory', PostgreSQLPath) then
    Result := PostgreSQLPath + '\bin'
  else
    Result := 'C:\Program Files\PostgreSQL\15\bin';
end;

function InitializeSetup(): Boolean;
begin
  // All files are embedded in the installer, no need to check {src}
  Result := True;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  // Post-install actions if needed in the future
end;
