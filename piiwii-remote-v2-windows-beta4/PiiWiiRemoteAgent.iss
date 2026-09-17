#define MyAppName "PiiWii Remote Agent"
#define MyAppVersion "2.0.0-beta4"
#define MyAppPublisher "PiiWii"
#define MyAppExeName "PiiWii-Remote-Agent.exe"

[Setup]
AppId={{A7A9D84A-4D5B-4A6F-A62D-2C693C768C21}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
VersionInfoVersion=2.0.0.11
VersionInfoCompany=PiiWii
VersionInfoDescription=PiiWii Remote Agent Setup
VersionInfoProductName=PiiWii Remote Agent
VersionInfoProductVersion=2.0.0.11
DefaultDirName={localappdata}\Programs\PiiWii Remote Agent
DefaultGroupName=PiiWii Remote
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
OutputDir=output
OutputBaseFilename=PiiWii-Remote-Setup-2.0.0-beta4
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
SetupLogging=yes
CloseApplications=yes
RestartApplications=no
UninstallDisplayIcon={app}\{#MyAppExeName}

[Tasks]
Name: "autostart"; Description: "Démarrer PiiWii Remote Agent automatiquement avec Windows"; GroupDescription: "Démarrage :"; Flags: checkedonce

[Files]
Source: "..\build\PiiWii-Remote-Agent-2.0.0-beta4.exe"; DestDir: "{app}"; DestName: "{#MyAppExeName}"; Flags: ignoreversion

[Icons]
Name: "{userdesktop}\PiiWii Remote Agent"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; Comment: "Télécommande PiiWii Remote pour Windows"
Name: "{userprograms}\PiiWii Remote Agent"; Filename: "{app}\{#MyAppExeName}"; WorkingDir: "{app}"; Comment: "Télécommande PiiWii Remote pour Windows"
Name: "{userprograms}\Désinstaller PiiWii Remote Agent"; Filename: "{uninstallexe}"

[Registry]
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "PiiWii Remote Agent"; ValueData: """{app}\{#MyAppExeName}"" --background"; Tasks: autostart; Flags: uninsdeletevalue
Root: HKCU; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueName: "PiiWii Remote Agent"; Flags: deletevalue; Tasks: not autostart

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Lancer PiiWii Remote Agent"; Flags: nowait postinstall skipifsilent

[Code]
procedure KillImage(const ImageName: String);
var
  ResultCode: Integer;
begin
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/F /IM "' + ImageName + '"', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  I: Integer;
  Names: array[0..13] of String;
begin
  Names[0] := 'PiiWii-Remote-Agent.exe';
  Names[1] := 'PiiWii-Remote-Agent-2.0.0-alpha1.exe';
  Names[2] := 'PiiWii-Remote-Agent-2.0.0-alpha2.exe';
  Names[3] := 'PiiWii-Remote-Agent-2.0.0-alpha3.exe';
  Names[4] := 'PiiWii-Remote-Agent-2.0.0-alpha4.exe';
  Names[5] := 'PiiWii-Remote-Agent-2.0.0-alpha5.exe';
  Names[6] := 'PiiWii-Remote-Agent-2.0.0-alpha6.exe';
  Names[7] := 'PiiWii-Remote-Agent-2.0.0-alpha7.exe';
  Names[8] := 'PiiWii-Remote-Agent-2.0.0-beta1.exe';
  Names[9] := 'PiiWii-Remote-Agent-2.0.0-beta2.exe';
  Names[10] := 'PiiWii-Remote-Agent-2.0.0-beta3.exe';
  Names[11] := 'PiiWii Remote Agent.exe';
  Names[12] := 'PiiWiiRemoteAgent.exe';
  Names[13] := 'PiiWii-Remote-Agent-2.exe';
  for I := 0 to 13 do
    KillImage(Names[I]);

  RegDeleteValue(HKCU, 'Software\Microsoft\Windows\CurrentVersion\Run', 'PiiWii Remote Agent');
  Result := '';
end;
