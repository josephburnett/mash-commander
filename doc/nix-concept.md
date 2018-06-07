# Nix Concept

Nix is a friendly and helpful robot.  But she has a rebooting problem and she needs your help to fix it.  She can't do it herself but she can teach you how.  Use the terminal and keyboard to interact with her Unix-like system.

![nix drawing](nix-drawing.jpg)

# Character

* forgetful
* loveable
* i can teach you but i can't do it myself
* keyboard physically attached
* eyes move
* reboots after 5 minutes of inactivity
* `set` command to customize appearance

# Game Play

Nix will guide you through a series of command-line tasks to fix some small problems.  Maybe finding a missing file or a fixing a glitch in her system.  Ultimately you will help her fix her rebooting problem.  If you stop interacting with her terminal for 5 minutes then she will reboot and forget everything you've done so far.  But don't worry, you can remind her of where you left off with a special command.

## Learning Objectives

### Level 1

* Typing commands
* Using history
* Get help (`man`) ?

### Level 2

* List the current directory (`ls`)
* Change directories (`cd`)
* Find a file by name (`find`)
* Find a file by contents (`grep`)

### Level 3

* Copy a file (`cp`)
* Move a file (`mv`)
* Create a file (`touch`)
* Edit a file (`edit`)
* Remove a file (`rm`)

### Level 4

* Run a script (`./`)
* Chmod a file (`chmod`)
* Write a script (`edit` & `chmod`)
* Fix a script (`./` & `edit`)

### Level 5

* List running processes (`ps`)
* Start a background process (`&`)
* Kill a process (`kill`)
* Start a process somewhere else (`ssh` & `&`)

## Concepts excluded

* Multi-user systems
* Home directories

## Errors

* `404: Not Found`
* `403: Forbidden`
* `500: Internal Error`

# Implementation

* [Nix mode](https://github.com/josephburnett/mash-commander/tree/master/src/mash_commander/nix)
* [Story](https://github.com/josephburnett/mash-commander/blob/master/src/mash_commander/nix/story.cljc)
