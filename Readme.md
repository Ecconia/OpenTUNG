# OpenTUNG

OpenTUNG is basically a WIP clone of "The Ultimate Nerd Game" written in Java.

Started and maintained by @Ecconia

## Kilometer-marks

Arrived at:
- OpenTUNG imports `.tungboard` files.
- OpenTUNG displays everything of the loaded TUNGBoard.
- OpenTUNG can simulate the whole board circuit.
- It is possible to place and remove components and boards. Which also updates the simulation network.

To arrive:
- Add animated interaction with buttons/switches.
- Give label-text a good formatting and graphic.
- Add modification of the builds (collision checking), so that things can be placed or move at each other.
- Improve the looks of the game, better light calculation pending.
- Add advanced modification of boards and components, such as grabbing, stacking, copying, pasting and rotating.
- Add fancy multi-wire placement.

## Why? (Story...)

Some ages ago @JimmyCushnie wrote "The Ultimate Nerd Game".\
But then TUNG got declared as the alpha version of the much superior "Logic World".\
That was some years ago and since then it has not received any update.\
It is however an amazing game/tool to make some of our dreams reality.

Sadly it comes with a bunch of negative factors.\
If you have bad hardware like @Ecconia and run the game on Linux, you might experience the game to only run on 15FPS (or less) as well as having it use 20GB of RAM. The game runs properly, until you start to build very big things, like memories.\
Additional depending on version and OS you have a bunch of bugs which sometimes make building difficult.\
There is a modding framework, but that does not support the latest version, the modding-supported version has a horrible bug on Linux.\
So for @Ecconia that means "no mods", which makes placing many wires a tedious task.

In a nutshell, this project aims to provide a version of TUNG which does not have any of the old bugs and even comes with more features and performance.\
This is nevertheless a Work-In-Progress project which has yet to arrive at the level of TUNG. Some things are better already, some are not even implemented. Things like the nice landscape will however never make it into OpenTUNG.

Everyone is free to use this program/game/tool/simulator, if he/she doesn't hate Java :P

## Install (Build from source...)

#### Preconditions:

- Java Development Kit of at least version 1.8.0_200, but any newer JDK does the job too (9+).
- Maven, any recent version should do.

##### Windows:

- Install JDK
  - Be sure to have Environment Variable `JAVA_HOME` set and pointing to where you installed the JDK.
  - Add `%JAVA_HOME%\bin` to your `Path` if its not there already.
  - You may have to restart after adding/changing an environment variable.
  - Try `java -version` in `cmd`, if it doesn't work consult your favorite search engine.
- Install Maven
  - Download from their website.
  - Extract to anywhere where you don't move it.
  - Set Environment Variable `MAVEN_HOME` pointing to where you extracted the file.
  - Add `%MAVEN_HOME%\bin` to your `Path`.
  - You may have to restart after adding/changing an environment variable.
  - Try `mvn -version` in `cmd`, if it doesn't work consult your favorite search engine.

##### Linux:

- Install OpenJDK
  - But you may just use any other JDK installed via terminal, your favorite search engine helps.
  - Try `java -version` in your favorite terminal, if it doesn't work consult your favorite search engine.
- Install Maven, you may use provided `maven` package.
  - If the version is older, you might have no colors in terminal while compiling it.
  - You can easily get an up to date version. Just use your favorite search engine.
  - Try `mvn -version` in your favorite terminal, if it doesn't work consult your favorite search engine.

#### Build process:

- Clone the project from Github to any folder you like.
- Run command `mvn package`, see Maven installation above.
- Start the resulting .jar file `java -jar target/OpenTUNG-0-WIP-jar-with-dependencies.jar`, see Java installation above.
  - The name of the .jar may change with rising OpenTUNG version.
  - You mave move/copy/rename that file. It should work.
  - Double clicking it might also work, but you may miss information/instructions on terminal.

## Files & Folder:

OpenTUNG creates a folder `OpenTUNG`, wherever you executed it. First output in terminal tells you the location of that folder.\
In it you have to create a `boards` folder, please put any `.tungboard` file there. You can download them from the official LW discord (and some other sources).

### Settings:

In the OpenTUNG-Folder you will find a `settings.txt` file.\
It contains a bunch of settings regards OpenTUNG, you can change the settings-file while OpenTUNG is running.
It will automatically reload the content once you saved. Some settings only apply on start-up though.

## Collaborate/Contact

This project has an [Discord-Server (OpenTUNG)](https://discord.gg/W7ukHBn).

You may always contact developers and users there, if you wish to improve something, seek help or just to talk about/discuss this project.

If you want to collaborate, you should check out that discord.

TUNG/LogicWorld also have a [Discord-Server (LogicWorld/TUNG)](https://discord.gg/C5Qkk53), if you don't know what this game is about, check out that one first.
