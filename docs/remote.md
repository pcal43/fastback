---
layout: default
title: Remote Backups
nav_order: 40
---

# Remote Backups

An important part of any backup strategy is to keep a copy of the backup on a different computer.  FastBack makes that easy.

## Setting a remote Backup Target

FastBack can automatically upload copies of your backups to another git repository.  We call this other repository the *remote*.

### Configuring remote backups to a git server

If you have a git server already running (GitHub, for example), all you need to do is
* create a repository on the server to store your world's backups
* get the URL to the repository (e.g., `ssh://192.168.0.99/mygitserver/myworld`)

Then, with your world running in Minecraft, type
```
/backup set-remote ssh://192.168.0.99/mygitserver/myworld
```

### Configuring remote backups to a file remote

If you don't have a git server, no problem.  You can also do remote backups to any network drive on
your computer.  Just type something like

```
/backup create-file-remote /path/to/network/volume/minecraft-backups/myworld
```

You can configure this to be any valid path on your file system.  But it makes the most sense to do your
backups to another machine on your network.

If you ever need to reattach a world to an existing file remote, you can use the `set-remote` command with a `file://` url.  
For the example above, that would be

```
/backup set-remote file:///path/to/network/volume/minecraft-backups/myworld
```


## Restoring a Remote Snapshot

Say the unthinkable happens: your hard drive crashes.  Your Minecraft world is lost...unless you've been keeping
remote backups!

You can list snapshots from the remote just as you can from your local backup:

```
/backup remote-list
2022-09-24_13_23_11
2022-10-02_12_56_33
2022-10-07_11_49_31
```

and then restore one like so:

```
/backup remote-restore 2022-10-02_12_56_33
Snapshot restored to
/home/pcal/minecraft/saves/MyWorld-2022-10-02_12_56_33
```

Just as with local snapshots, restoring a remote snapshots creates a *new* world; existing worlds are never changed.
The path to the restored world will be displayed after you run the command.


## Technical Notes

### World UUID

FastBack tries to stop you from mixing backup snapshots from different worlds.  This is generally a bad idea both in terms of staying organized and backup performance.

If you want to live dangerously, you can view or change the UUID of a world by looking at a file in you world save directory: `fastback/world.uuid`.


## Manually Restoring a Remote Snapshot

FastBack backups are just regular git repos.  This means you can use the terminal and the `git` command line tool to interact with them.

To restore from a remote manually using `git`:

1. Install `git`.  Mac and Linux users should already have it; Windows users may need to go [here](https://git-scm.com/downloads).

2. Clone the backup repo and list snapshots

```
git clone [repo-url]
cd [directory that just got created]
git branch
```

This will list all of your available snapshot branches:

```
snapshots/12345678-1234-5678-1234-567812345678/2022-10-02_12_56_33
snapshots/12345678-1234-5678-1234-567812345678/2022-10-07_11_49_31
```

To retrieve one of them, type:

```
git checkout snapshots/12345678-1234-5678-1234-567812345678/2022-10-02_12_56_33
```

Your world save files will appear in the directory.  You can then copy them into your minecraft installation.



