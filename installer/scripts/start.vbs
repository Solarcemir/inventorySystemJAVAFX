' CFT Electronic - Launcher
' Starts the Spring Boot backend silently, waits for it, then opens the browser.

Option Explicit

Dim oShell, oFSO, sAppDir, sJava, sJar, i

Set oShell = CreateObject("WScript.Shell")
Set oFSO   = CreateObject("Scripting.FileSystemObject")

' Resolve paths relative to this script
sAppDir = oFSO.GetParentFolderName(WScript.ScriptFullName)
sJava   = sAppDir & "\jre\bin\javaw.exe"
sJar    = sAppDir & "\app\backend.jar"

' If already running, just open the browser
If ServerReady() Then
    oShell.Run "http://localhost:8080", 1, False
    WScript.Quit
End If

' Verify files exist before trying to launch
If Not oFSO.FileExists(sJava) Then
    MsgBox "ERROR: Java runtime not found." & vbCrLf & sJava, 16, "CFT Electronic"
    WScript.Quit 1
End If
If Not oFSO.FileExists(sJar) Then
    MsgBox "ERROR: Backend JAR not found." & vbCrLf & sJar, 16, "CFT Electronic"
    WScript.Quit 1
End If

' Set working directory to the app folder so Spring Boot auto-loads
' config\application.properties from there (Spring Boot convention).
' This avoids any path/spaces issues with -Dspring.config.location.
oShell.CurrentDirectory = sAppDir & "\app"

' Launch backend with javaw.exe — no console window
Dim sCmd
sCmd = Chr(34) & sJava & Chr(34) & " -jar " & Chr(34) & sJar & Chr(34)
oShell.Run sCmd, 0, False

' Wait up to 60 seconds for Spring Boot to respond
For i = 1 To 60
    WScript.Sleep 1000
    If ServerReady() Then Exit For
Next

If Not ServerReady() Then
    MsgBox "The backend server did not start in time." & vbCrLf & vbCrLf & _
           "Possible causes:" & vbCrLf & _
           "  - Port 8080 is in use by another program" & vbCrLf & _
           "  - No internet connection (needs Supabase)" & vbCrLf & _
           "  - Antivirus blocking Java" & vbCrLf & vbCrLf & _
           "To check: open Command Prompt and run:" & vbCrLf & _
           "  netstat -an | findstr 8080", _
           48, "CFT Electronic - Startup Failed"
    WScript.Quit 1
End If

' Open default browser
oShell.Run "http://localhost:8080", 1, False

' ── Helper: HTTP health check ─────────────────────────────────────────────────
Function ServerReady()
    Dim oHTTP
    On Error Resume Next
    Set oHTTP = CreateObject("MSXML2.XMLHTTP.6.0")
    oHTTP.Open "GET", "http://localhost:8080/api/health", False
    oHTTP.Send
    ServerReady = (Err.Number = 0 And oHTTP.Status = 200)
    On Error GoTo 0
End Function
