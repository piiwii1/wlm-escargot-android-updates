#define MyAppName "PiiWii Remote Agent"
#define MyAppVersion "2.0.0-beta3"
#define MyAppPublisher "PiiWii"
#define MyAppExeName "PiiWii-Remote-Agent.exe"

[Setup]
AppId={{A7A9D84A-4D5B-4A6F-A62D-2C693C768C21}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
VersionInfoVersion=2.0.0.10
VersionInfoCompany=PiiWii
VersionInfoDescription=PiiWii Remote Agent Setup
VersionInfoProductName=PiiWii Remote Agent
VersionInfoProductVersion=2.0.0.10
DefaultDirName={localappdata}\Programs\PiiWii Remote Agent
DefaultGroupName=PiiWii Remote
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
OutputDir=output
OutputBaseFilename=PiiWii-Remote-Setup-2.0.0-beta3
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
SetupLogging=yes
CloseApplications=yes
RestartApplications=no
UninstallDisplayIcon={app}\{#MyAppExeName}

[Files]
Source: "..\build\PiiWii-Remote-Agent-2.0.0-beta3.exe"; DestDir: "{app}"; DestName: "{#MyAppExeName}"; Flags: ignoreversion

[Icons]
Name: "{userdesktop}\PiiWii Remote Agent"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; Comment: "Télécommande PiiWii Remote pour Windows"
Name: "{userprograms}\PiiWii Remote Agent"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; Comment: "Télécommande PiiWii Remote pour Windows"
Name: "{userprograms}\Désinstaller PiiWii Remote Agent"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Lancer PiiWii Remote Agent"; Flags: nowait postinstall skipifsilent
