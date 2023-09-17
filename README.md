# FastBack
*Fast, incremental Minecraft world backups powered by Git*

Fastback is a Minecraft mod that backs up your world in incremental snapshots.  When it does a backup,
it only saves the parts of your world that changed.

This means backups are fast.  It also means you can keep snapshots of your world without using up a lot
of disk space.

![](https://pcal43.github.io/fastback/savescreen_animation.gif)

## Features

* **Now with Forge support!**
* Incrementally backup just the changed files
* Faster, smaller backups than zipping
* Back up locally
* Back up remotely to any git server
* Back up remotely to any network volume (no git server required)
* Schedule backups to run automatically
* Easily restore backup snapshots
* Snapshot pruning, retention policies
* Include mod jars and config files in backup snapshots
* Broadcast server-wide notifications during backups
* LuckPerms support
* Works on clients and dedicated servers
* Works on Linux, Mac and Windows
* ..all with easy-to-use minecraft commands


## Road Map
* Support for restoring remote snapshots
* Better management of remote snapshots
* UI for managing backups from the title screen
* ~~Forge support (maybe)~~

## Acknowledgements

* Russian localization provided by [Felix14-v2](https://github.com/Felix14-v2).
* Chinese localization provided by [buiawpkgew1](https://github.com/buiawpkgew1).
* Fastback includes and was made possible by the work of committers on these projects:
    * [JGit](https://www.eclipse.org/jgit/) from The Eclipse Software Foundation
    * [sshd](https://mina.apache.org/sshd-project/) from The Apache Software Foundation
    * [JavaEWAH](https://github.com/lemire/javaewah) from Daniel Lemire, et al.
    * [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) from lucko
    * [server-translations-api](https://github.com/NucleoidMC/Server-Translations) from the Fabric Community

## Legal

FastBack is distributed under [GNU Public License version 2](https://github.com/pcal43/fastback/blob/main/LICENSE).

You can put it in a modpack but please include attribution with a link to this page.

## Questions?

Please start with the [documentation](https://pcal43.github.io/fastback).  If you have more
questions, please join "pcal's minecraft mods" on Discord:

[https://discord.pcal.net](https://discord.pcal.net)

Note that Curseforge comments have been disabled and I will **not** reply to private messages.

