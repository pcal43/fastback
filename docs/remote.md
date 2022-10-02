---
layout: default
title: Remotes
nav_order: 40
---

# Managing Remote Backups

An important part of any backup strategy is to keep a copy of the backup on a different computer.  FastBack makes that easy.


## Restoring a Remote Snapshot

If your world gets deleted or corrupted, you can restore a snapshot from your remote backup.

**IMPORTANT** FastBack is still a work-in-progress


## Manually Restoring a Remote Snapshot


1. Install `git`.  Mac and Linux users should already have it; Windows users may need to go [here](https://git-scm.com/downloads).

2. Clone the backup repo and list snapshots

```
git clone [repo-url]
cd [directory that just got created]
git branch
```

This will list all of your available snapshot branches:

```
```

To retrieve one of them, type:

```
git checkout snapshots/abcdef-asdfasdf-asdfasdf/2022-10-02
```

You world save files will appear in the directory.  You can then copy them into your minecraft installation.
