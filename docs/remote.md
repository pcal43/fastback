---
layout: default
title: Remotes
nav_order: 40
---

# Managing Remote Backups

An important part of any backup strategy is to keep a copy of the backup on a different computer.  FastBack makes that easy.


## File-remote URLs

If you created a remote using `create-file-remote` you may need to know the URL to the repo if (say) you ever need to attach a restored world to it with `set-remote`.

The URL to a file remote is simply:

```
file:///path/on/your/disk/to/the/remote/backup/dir
```

Note that's *three* slashes after the file.  You can also see the URL when by running `/backup info`.


## World UUID

FastBack tries to stop you from mixing backup snapshots from different worlds.  This is generally a bad idea both in terms of staying organized and backup performance.

If you want to live dangerously, you can view or change the UUID of a world by looking at a file in you world save directory: `fastback/world.uuid`.


## Restoring a Remote Snapshot

If your world gets deleted or corrupted, you can restore a snapshot from your remote backup.

**At the moment, FastBack does not provide commands for doing this.  See [Issues]https://github.com/pcal43/fastback/issues).**

Until those commands are added, you can follow the instructions below to restore remote snapshots manually.


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

You world save files will appear in the directory.  You can then copy them into your minecraft installation.
