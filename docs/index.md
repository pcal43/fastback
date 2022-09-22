---
layout: default
title: FastBack
nav_order: 1
---

# FastBack
*Fast, incremental Minecraft world backups powered by Git*

Fastback is a Fabric Minecraft mod that backs up your world in incremental snapshots.  When it does a backup,
it only saves the parts of your world that changed.  

This means backups are fast.  It also means you can keep snapshots of your world without using up a lot
of disk space.

## Features

* Incrementally backup just the changed files
* Faster, smaller backups than zipping
* Back up locally
* Back up remotely to any git server
* Back up remotely to any network volume (no git server required)
* Easily restore backup snapshots
* Works on clients and dedicated servers
* Works on Linux, Mac and Windows
* LuckPerms support
* ..all with easy-to-use minecraft commands


## Road Map
* Scheduled backups
* UI for managing backups from the title screen
* Better management of remote snapshots
* Automatic snapshot purging strategies
* Forge support (maybe)

## Limitations

FastBack is currently an alpha release.  It has missing features and limitations.  For more information, see [Issues](https://github.com/pcal43/fastback/issues)

## Credits

Fastback's Russian localization provided by [Felix14-v2](https://github.com/Felix14-v2). (Thanks!) 

FastBack includes several other software components.  Many thanks to the committers on these 
projects, whose work made FastBack possible:
* [JGit](https://www.eclipse.org/jgit/) from The Eclipse Software Foundation
* [sshd](https://mina.apache.org/sshd-project/) from The Apache Software Foundation
* [JavaEWAH](https://github.com/lemire/javaewah) from Daniel Lemire, et al.
* [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) from lucko


## Legal
 
FastBack is distributed under [GNU Public License version 2](https://github.com/pcal43/fastback/blob/main/LICENSE). 

You can put it in a modpack but please include attribution with a link to this page.
