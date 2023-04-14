---
layout: default
title: Using FastBack
nav_order: 10
---

# Using FastBack

FastBack adds a custom `/backup` command that is used for all backup operations.  To get detailed help about
using it, just type.

```
/backup help
```

This page explains how to do most common tasks.

## Enabling Backups on a world

To enable backups on your world, just run


```
/backup enable
Enabled automatic local backups on world shutdown.
```

## Listing available backup snapshots

Every time FastBack runs, it creates a *snapshot* of your world.  Snapshots are identified by the time 
they were created.

To see all snapshots in your backup of the current world, run
```
/backup list

Available snapshots:
2022-10-07_10-11-12
2022-10-02_23-43-01
2022-09-24_11-32-59
2022-05-08_08-56-33
```


## Restoring a backup snapshot

You can restore your world from any snapshot in the backup by running

```
/backup restore 2022-10-02_10-11-12
Restoring 2022-10-02_10-11-12 to
/home/pcal/minecraft/saves/MyWorld-2022-10-02_10-11-12
```

This will create a copy of your world as it was when that snapshot was made.  

Note that files in the current world are never touched by `restore`; the restored snapshot is placed in either your `saves` directory (in singleplayer mode) or under your system temp directory (in server mode).  In either case, the full path to restore location will be output when the command completes.

To look at the restored snapshot, quit the current world and open the restored snapshot world.  (In server mode, you'll have to manually copy
the restored files from the location displayed at the end of the command).

