# OpenTUNG

OpenTUNG is basically a WIP clone of "The Ultimate Nerd Game" written in Java.

## Kilometer-marks

Arrived at:
- The game imports `.tungboard` files. (Missing Noisemaker and Delayer support currently).
- The game displays the loaded TUNGBoard.
- Wires have a state and are connected with each other, waiting for the simulation to control them.

To arrive:
- Add animated interaction with buttons/switches.
- Have every component visible in the game and give label-text a good formatting.
- Run the simulation so that it behaves the same as in TUNG, with better visuals on low tick rates.
- Add modification of the builds (collision checking), so that things can be placed or move at each other.
- Update the simulation network properly on modifications of it.
- Improve the looks of the game, better light calculation pending.

## Why? (Story...)

TUNG was written some time ago and has not received any update since then.
It is however an amazing game/tool to make some of your dream reality.

Sadly it comes with a bunch of negative factors.
Without a dedicated GPU, the game does not run very well (5 FPS with decent content).
Additional the modding framework is only compatible with 0.2.6 which has an aweful typing bug on Linux. And without mods the game might be tricky.
And 0.2.7 has a bunch of bugs on Windows, but works on Linux. In either OS with that version one cannot stack boards, which is painful.
Additional to that if you build something bigger, you may end up with huge load times and an almost frozen computer since the game needs 15GB for unknown reasons.

In a nutshell, this clone is an attempt to make a more performant and simplified TUNG which can be used, without the known struggles.
If its at a stage similar/equal to TUNG, it will provide a better GUI since its just possible to rewrite it in any desired way, yay open-source.

Everyone is free to use this, if he/she doesn't hate Java :P

## Install (Build from source...)

#### Preconditions:

- Java Development Kit of at least version 1.8.0_200, but any newer JDK does the job too (9+).
- Maven, any recent version should do.

##### Windows:

- Install JDK
  - Be sure to have Environment Variable `JAVA_HOME` set and pointing to where you installed the JDK.
  - Add `%JAVA_HOME%\bin` to your `Path` if its not there already.
  - Try `java -version` in `cmd`, if it doesn't work consult your favorite search engine.
- Install Maven
  - Download from their website.
  - Extract to anywhere where you don't move it.
  - Set Environment Variable `%MAVEN_HOME%` pointing to where you extracted the file.
  - Add `%MAVEN_HOME%\bin` to your `Path`.
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

## Collaborate/Contact

Everyone who used TUNG before may collaborate. Personally I also require such a person to be a part of the TUNG/LW community, see their [Discord](https://discord.gg/C5Qkk53). You may always contact me there, if you wish to improve something or just to talk/discuss about this project.
