---
layout: default
title: Native Git Support
nav_order: 95
---


# Native Git Support

As of version `0.13.0`, Fastback includes support for native git.  This means that for some operations, it 
will run the native git binaries on your machine rather than using the embedded java implementation of git (JGit).

The advantage of this approach is that it appears to be significantly faster in many cases.  It may
also be less wasteful of disk space.

The disadvantage of this approach is that you can't use it unless you first install git on your machine.


## How to Enable Native Git Support

### If you already have git installed

If you already have git and git-lfs installed, native support will be enabled automatically when you
run `/backup init`.  

If for some reason you don't want this, you can disable it by immediately running 
`/backup set native-git-enabled false`.

### If you *don't* have git intsalled

If you don't have git installed, you'll be told about this when you run `/backup init`.  

If possible, you're strongly encouraged to install it.  How you do this will depend on
your platform.  There are a ton of resources on the web describing how to do this.  [Start here](https://github.com/git-lfs/git-lfs/wiki/Installation) and ask google for more help if needed.

After installing git and git-lfs, you can run `/backup set native-git-enabled true` *if you haven't
already done a backup*. 

### Changing `native-git-enabled` if you've already done a backup

You're strongly discouraged from enabling native support on a world that has already made backups in non-native mode 
(or vice versa).  Native and non-native employ different strategies, and mixing them in the same repo may cause performance
issues or other problems in the future.

If you want to change the native setting on a world that you've already backed up, you have three options:

* Start over: Create a brand new world that you've never backed up
* Erase backups: delete the `.git` directory from an existing world.  
  * THIS WILL DELETE ALL BACKUP DATA FOR THAT WORLD.
* Live dangerously: edit `.git/config` by hand to force a change to `fastback.native-git-enabled`
  * YOU MAY HAVE BAD PERFORMANCE OR OTHER PROBLEMS IN THE FUTURE IF YOU DO THIS.

### Pushing to a remote with native mode enabled

If you're going to be pushing remote backups with native mode enabled, the server **must** support `git-lfs`.   


## Performance Analysis

Some rough numbers using a [5.5gb world](https://hermitcraft.fandom.com/wiki/Season_4) and pushing to a gitea server
over LAN:


| Test               | JGit     | Native  | 
|--------------------|----------|---------|
| first commit       | 2m 20s   | **23s** |
| first push         | 6m 18s   | 8m 34s  |
| incremental commit | 1s       | 1s      |
| incremental push   | 6m 25s   | **11s** |

Incremental remote backups are obviously a huge win here.  Mainly because native mode is using LFS.

jGit actually does have some LFS support but it's not mature enough to be used here (in particular, it doesn't
have support for `lfs prune`)

