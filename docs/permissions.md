---
layout: default title: Permissions nav_order: 70
---

# Permissions

In single-player mode, `/backup` can be run without enabling cheats.

On a dedicated server, `/backup` can be run only by level 4 operators.

### LuckPerms Support

FastBack exposes fine-grained permissions via
the [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api)
so that you can do access control in [LuckPerms](https://luckperms.net/).

Supported Permissions:

* `fastback.command` (top-level /backup command)
* `fastback.command.disable`
* `fastback.command.enable`
* `fastback.command.file-remote`
* `fastback.command.gc` 
* `fastback.command.help`
* `fastback.command.list`
* `fastback.command.now`
* `fastback.command.purge`
* `fastback.command.remote`
* `fastback.command.restore`
* `fastback.command.status`
* `fastback.command.uuid`
* `fastback.command.version`      
