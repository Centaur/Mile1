/**
 * 此脚本仅用来下载和运行 mile1.jar，其它所有的功能都由java代码完成。
 */

var oShell = new ActiveXObject('WScript.Shell');

(function(ws){
    if (ws.fullName.slice(-12).toLowerCase() !== '\\cscript.exe') {
        var cmd = 'cscript.exe //nologo "' + ws.scriptFullName + '"';
        var args = ws.arguments;
        for (var i = 0, len = args.length; i < len; i++) {
          var arg = args(i);
          cmd += ' ' + (~arg.indexOf(' ') ? '"' + arg + '"' : arg);
        }
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
            xHttp.Open('GET', 'http://git.oschina.net/43284683/Mile1/raw/master/downloads/mile1.jar', false);
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
    if(!ws.arguments[0] == 'update'){
        var cmdline = 'cmd /c java -Dmile1.script.path="' + scriptDirectoryPath + '" -jar "' + mile1JarFilePath + '" ' + ws.arguments.join(' ') + ' 2>&1';
        ws.echo(cmdline);
        var javaProcess = oShell.exec(cmdline);
        do{
            ws.stdout.writeLine(javaProcess.stdout.readLine())
        } while (javaProcess.stdout.atEndOfStream)
    } else {
       fso.DeleteFile(mile1JarFilePath);
       ws.stdout.write("Updating Mile1...");
       download_mile1_jar();
    }
})(WScript);
