
Set oShell = CreateObject("WScript.Shell")
mile1Home = oShell.ExpandEnvironmentStrings("%USERPROFILE%") & "\.mile1"

Set fso = CreateObject("Scripting.FileSystemObject")

sub mkdirs(ByVal path)
    if not fso.FolderExists(path) then
        mkdirs fso.GetParentFolderName(path)
        fso.CreateFolder path
    end if
end sub

mkdirs mile1Home
mile1JarFilePath = mile1Home & "\mile1.jar"

if not fso.FileExists(mile1JarFilePath) then
    wscript.stdout.writeLine("Installing Mile1...")
    dim xHttp: Set xHttp = createobject("Microsoft.XMLHTTP")
    dim bStrm: Set bStrm = createobject("Adodb.Stream")
    xHttp.Open "GET", "http://git.oschina.net/43284683/Mile1/raw/master/downloads/mile1.jar", False
    xHttp.Send

    with bStrm
        .type = 1 '//binary
        .open
        .write xHttp.responseBody
        .savetofile mile1JarFilePath, 2 '//overwrite
    end with
end if

reDim arr(wscript.arguments.count - 1)
for i = 0 to wscript.arguments.count - 1
    arr(i) = wscript.arguments(i)
next

scriptFilePath = wscript.scriptFullName
scriptDirectoryPath = fso.GetParentFolderName(fso.GetFile(scriptFilePath))

cmdline = "java -Dmile1.script.path=""" & scriptDirectoryPath & """ -jar """ & mile1JarFilePath & """ " & join(arr)

wscript.echo(cmdline)
set javaProcess = oShell.exec(cmdline)
do
    wscript.stdout.writeLine(javaProcess.stdout.readLine())
loop while not javaProcess.stdout.atEndOfStream
do
    wscript.stdout.writeLine(javaProcess.stderr.readLine())
loop while not javaProcess.stderr.atEndOfStream

