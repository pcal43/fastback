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

## Current Features

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


## Current Limitations

FastBack is currently an alpha release.  It has missing features and limitations, including:
* Backups can only run when the world is shutting down.
* Garbage collection does not happen correctly when pruning old snapshots.
* For more, see [Issues](https://github.com/pcal43/fastback/issues)



## Legal
 
FastBack is distributed under [GNU Public License version 2](https://github.com/pcal43/fastback/blob/main/LICENSE). 

FastBack includes the following software components: 
* [JGit](https://www.eclipse.org/jgit/) from The Eclipse Software Foundation
* [sshd](https://mina.apache.org/sshd-project/) from The Apache Software Foundation
* [JavaEWAH](https://github.com/lemire/javaewah) from Daniel Lemire, et al.
* [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) from lucko

Many thanks to the committers on those projects, whose work made FastBack possible.
