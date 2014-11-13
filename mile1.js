�1�3/**
 * �˽ű����������غ����� mile1.jar���������еĹ��ܶ���java������ɡ�
 */

var oShell = new ActiveXObject('WScript.Shell');

function join_args(ws) {
    var result = '';
    var args = ws.arguments;
    for (var i = 0, len = args.length; i < len; i++) {
      var arg = args(i);
      result += ' ' + (~arg.indexOf(' ') ? '"' + arg + '"' : arg);
    }
    return result; 
}

(function(ws){
    if (ws.fullName.slice(-12).toLowerCase() !== '\\cscript.exe') {
        var cmd = 'cscript.exe //nologo "' + ws.scriptFullName + '"';
        cmd += join_args(ws);
        oShell.run(cmd);
        ws.quit();
      }
})(WScript);

var mile1Home = oShell.ExpandEnvironmentStrings('%USERPROFILE%')+'\\.mile1';
var fso = new ActiveXObject('Scripting.FileSystemObject');

function mkdirs(path) {
  if(!fso.FolderExists(path)){
    mkdirs(fso.GetParentFolderName(path));
    fso.CreateFolder(path)
  }
}

mkdirs(mile1Home);
var mile1JarFilePath = mile1Home + '\\mile1.jar';

(function(ws){
    function download_mile1_jar() {
            var xHttp = new ActiveXObject("Microsoft.XMLHTTP");
            var bStrm = new ActiveXObject("Adodb.Stream");
            xHttp.Open('GET', 'http://git.oschina.net/43284683/Mile1/raw/no-typesafe/downloads/mile1.jar', false);
            xHttp.Send();

            bStrm.type = 1; // binary
            bStrm.open();
            bStrm.write(xHttp.responseBody);
            bStrm.savetofile(mile1JarFilePath, 2); // overwrite
            bStrm.close();
            ws.stdout.writeLine("Done.")
    }
    if(!fso.FileExists(mile1JarFilePath)) {
        ws.stdout.write("Installing Mile1..");
        download_mile1_jar();
    }
    var scriptFilePath = ws.scriptFullName;
    var scriptDirectoryPath = fso.GetParentFolderName(fso.GetFile(scriptFilePath))
    var args = ws.arguments;
    if(args(0) != 'update'){
        var cmdline = 'cmd /c java -Dmile1.script.path="' + scriptDirectoryPath + '" -jar "' + mile1JarFilePath + '" ';
        cmdline += join_args(ws);
        cmdline += ' 2>&1'; 
        var javaProcess = oShell.exec(cmdline);
        do{
            ws.stdout.writeLine(javaProcess.stdout.readLine())
        } while (!javaProcess.stdout.atEndOfStream);
    } else {
       fso.DeleteFile(mile1JarFilePath);
       ws.stdout.write("Updating Mile1...");
       download_mile1_jar();
    }
    ws.stdout.writeLine("Press Any Key to exit...");
    ws.stdin.readline();
})(WScript);
