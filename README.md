#Mile1

## Prerequisites
* JDK 6+ and `java` command on your `$PATH`|`%PATH%`
* `curl` or `wget` on your `$PATH` for downloading on Linux or Mac.

## Linux Installation
1. Download shell script at "https://github.com/Centaur/Mile1/raw/master/mile1"
2. Place it on your `$PATH` (`~/bin` is a good choice if it is on your path)
3. Set it to be executable. (`chmod 755 ~/bin/mile1`)
4. run any of the following commands will automatically install required files.

## Windows Installation
1. Download shell script at "https://github.com/Centaur/Mile1/raw/master/mile1.js"
2. Place it on your `%PATH%` (`%USERPROIFLE%/bin` is a good choice if it is on your path)
3. run any of the following commands will automatically install required files.

## install sbt
### install
`mile1 install [VERSION]`

if `VERSION` not specified, `mile1` will install the latest stable `sbt-launch.jar`.

### uninstall
`mile1 uninstall VERSION`

### cleanup
`mile1 cleanup`

Remove all versions except the latest installed.

## list all installed sbt version
`mile1 list`

## list available sbt versions
`mile1 available [-a]`

List all available sbt versions remotely from official repository. By default it only lists stable versions. All versions will be listed if `-a` option is specified.

## switch to specific sbt version
`mile1 use VERSION`

## show usage information
`mile1 usage`