# OpenTUNG

OpenTUNG is basically a WIP clone of "The Ultimate Nerd Game" written in Java.

## Milestones

The first target of this project is to load .tungboard files and display them in 3D, no interaction whatsoever.

The second target is to update the wire-state of this circuit and even run simulation.

The third target is to add collision checking, so that its possible for the player to modify the shown board.

## Why? (Story...)

TUNG was written some time ago and has not received any update since then. It is however an amazing game/tool to make some of your dream reality. Sadly it comes with a bunch of negative factors. If you for example run on linux and don't have a dedicated GPU on hand, then you cannot properly use version 0.2.6, thus also no mods, thus no fancy wire placement and without dedicated GPU you will end up with 5 FPS. Additional to that if you build something bigger, you may end up with huge load times and an almost frozen computer since the game needs 15GB for unknown reasons.

In a nutshell, this clone is an attempt to make a more performant and simplified TUNG which can be used, without the known struggles. If its at a stage similar/equal to TUNG, it will provide a better GUI since its just possible to rewrite it in any desired way, yay open-source.

Everyone is free to use this, if he/she doesn't hate Java :P

## Install (Build from source...)

- Clone the project from Github to any folder you like.
- Run command `mvn package`, you need to have Maven installed for that.
- Start the resulting .jar file `java -jar target/OpenTUNG-0-WIP-jar-with-dependencies.jar` with a Java with at least version 8. The name of the .jar may change with rising version.

## Collaborate/Contact

Everyone who used TUNG before may collaborate. Personally I also require such a person to be a part of the TUNG/LW community, see their [Discord](https://discord.gg/C5Qkk53). You may always contact me there, if you wish to improve something or just to talk/discuss about this project.
