#Mile1

## Prerequisites
* JDK 6+ and `java` command on your `$PATH`
* `curl` or `wget` on your `$PATH` for downloading

## Installation
1. Download shell script at "http://git.oschina.net/43284683/Mile1/raw/master/mile1"
2. Place it on your `$PATH` (`~/bin` is a good choice if it is on your path)
3. Set it to be executable. (`chmod 755 ~/bin/mile1`)
4. run any of the following commands will automatically install required files. So the first run may take more time, be patient.

## install & upgrade sbt
### install
`mile1 sbt install [VERSION]|latest`

if `VERSION` nor `latest` not specified, `mile1` will list all versions of `sbt-launch.jar` to choose.

## create sbt project
### simple project
`mile1 new PROJECT_NAME [as properties|scala]`

`as properties` will create a project with a `build.properties` file in `$PROJECT_ROOT`.

`as scala` will create a project with a `Build.scala` file in `$PROJECT_ROOT/project/`.
Default is `as scala`.

### from template
`mile1 new PROJECT_NAME extends GITHUB_REPO`
the format of

## upgrade itself
`mile1 upgrade`