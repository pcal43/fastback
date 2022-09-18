---
layout: default
title: Permissions
nav_order: 70
---

# Permissions

When playing in single-player mode, the `/backup` command can be run without enabling cheats.

On a dedicated server, only operators with minecraft permission level 4 will be able to run
the `/backup` command.


## Custom Permissions

FastBack supports fine-grained permissions via the [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api).

Supported Permissions:
* `fastback.command` (top-level /backup command)   
* `fastback.command.enable`       
* `fastback.command.disable`      
* `fastback.command.status`       
* `fastback.command.restore`      
* `fastback.command.purge`        
* `fastback.command.list`         
* `fastback.command.remote`       
* `fastback.command.create-remote`  
* `fastback.command.shutdown`     
* `fastback.command.uuid`         
* `fastback.command.version`      
* `fastback.command.help`         