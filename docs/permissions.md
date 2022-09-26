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

* `fastback.command`
* `fastback.command.enable`       
* `fastback.command.disable`      
* `fastback.command.local`        
* `fastback.command.full`         
* `fastback.command.info`         
* `fastback.command.restore`      
* `fastback.command.create-file-remote`
* `fastback.command.set-remote`   
* `fastback.command.set-shutdown-action`
* `fastback.command.set-retention`
* `fastback.command.prune`        
* `fastback.command.purge`        
* `fastback.command.gc`           
* `fastback.command.list`         
* `fastback.command.help`
