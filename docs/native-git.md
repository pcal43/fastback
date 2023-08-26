---
layout: default
title: Native Git Support
nav_order: 95
---


NUMBERS ON HERMITCRAFT WORLD

jgit
- commit: 2m 19s
- push:

native
- commit:
- push:


# Native Git Support

As of version `0.13.0`, Fastback includes **experimental** support for native git.  This means that
for some operations, it will run the native git binaries on your 
machine rather than using the embedded java implementation of git (JGit).

The advantage of this approach is that it appears to be significantly faster in many cases.  It may
also be less wasteful of disk space.

The disadvantage of this approach is that you can't use it unless you first install git on your machine.

## How to Enable Native Git Support

*Again, this is experimental.  It has not been thoroughly tested.  It might destroy your world.*


### 1. Install `git` and `git-lfs`

How you do this will depend on your platform.  There are a ton of resources on the web describing
how to do this.  [Start here](https://github.com/git-lfs/git-lfs/wiki/Installation) and ask google for 
more help if needed.


### 2. Create a new Minecraft world

Native support can't be enabled on a world that has already made backups in non-native mode - you need
to start with a clean slate.  Options:

* Create a brand new world
* Use an existing world that has never seen FastBack
* Make a copy of a world and then *in the copy*, delete the `.git` and the `fastback` directories


### 3. Enable `native-git`

Start the world.  In the console type

```
/backup enable
```

as normal and then

```
/backup set native-git enabled
```

If you installed git correctly, it should tell you that native-git is now enabled.  

You can now do backups like normal.  As always, your first `/backup full` is going to take a while (but hopefully will be faster 
than before).



## Other Stuff

### git-lfs

Native mode requires `git-lfs`.  If you are going to push to a remote server, the server must support git-lfs.  Most of them do nowadays.  

I'm very happily running [Gitea](https://docs.gitea.com/), FWIW.

### Debugging

If things go haywire, you can use a new command

```
/backup set force-debug enabled
```

to temporarily send debugging output to the minecraft logs.  This can be useful in tracking down problems.

## Questions?

If you try it out, [stop by on Discord](https://discord.gg/jUP5nSPrjx) and let us know how it went.