---
layout: default
title: Remotes
nav_order: 40
---

# Managing Remote Backups


## Restoring a snapshot from a remote


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
