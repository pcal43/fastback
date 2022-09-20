---
layout: default
title: Permissions
nav_order: 70
---

# Permissions

In single-player mode, `/backup` can be run without enabling cheats.

On a dedicated server, only level 4 operators will be able to run `/backup`.


### LuckPerms Support

FastBack exposes fine-grained permissions via the [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api)
so that you can do access control in [LuckPerms](https://luckperms.net/).

Supported Permissions:
* `fastback.command` (top-level /backup command)   
  `fastback.command.enable`       
  `fastback.command.disable`  
  `fastback.command.status`  
  `fastback.command.restore`  
  `fastback.command.purge`        
  `fastback.command.now`          
  `fastback.command.list`         
  `fastback.command.remote`       
  `fastback.command.file-remote`  
  `fastback.command.shutdown`     
  `fastback.command.uuid`         
  `fastback.command.version`      
  `fastback.command.help`         
