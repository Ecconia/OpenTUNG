# OpenTUNG

OpenTUNG is basically a WIP clone of "The Ultimate Nerd Game" written in Java.

Started and maintained by @Ecconia

## Kilometer-marks

Arrived at:
- OpenTUNG has its own save files `.opentung` and it can import `.tungboard` files.
- OpenTUNG renders every TUNG component, imported TUNG boards look almost identical.
- OpenTUNG has fully simulation support. The simulation can be set to any target ticks-per-second speed. Can be paused and single-stepped.
- OpenTUNG allows editing the world. Tools/Features:
  - Place/Delete boards components. Boards can be drawn. (LW-Inspired)
  - Grab components and boards to move/rotate them. (LW-Inspired)
  - Copy components to quickly duplicate circuits. (LW-Inspired)
  - Change color of displays and boards.
  - Resize boards after they had been placed. (LW-Inspired)
  - Create buses quickly using Multi-Wire-Placement. (LW-Inspired)
- Console output will be written into log-files.

To arrive:
- Collision is fully missing. Components can be placed inside each other. Wires can go through anything.
- Buttons and Switches do not have animations. Switches always look the same.
- OpenTUNG does not create any sound output.
- Missing main-menu, missing save-loading.
- It is currently impossible to edit labels. Further, imported labels do not wrap.
- Light shading is very basic Blinn-Phong style with specular disabled. Room for improvement.  
- Some advanced editing tools/feature are still missing: Stacking, Merging/Splitting boards. (And more).
- Import/Export of boards is not possible. Only the main board/save can be edited.

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

## Files & Folder / Getting started:

For running OpenTUNG, you need Java version 8. Any newer version should work as well.

OpenTUNG creates a folder `OpenTUNG`, wherever you executed it. If you want to change the location, use a script to switch to a different directory `cd <somefolder>` before launch it.\
If you cannot find the OpenTUNG folder, but have the console feedback open, you can find its path in the first output messages. Along with the executing OpenTUNG version.

In the `OpenTUNG` folder you will find:
- Log files in `logs` zipped to save memory.
- `keybindings.txt` which contains the keybindings you can change. (Use the `Keybindings` button in the Pause-Window, to change them).
- `settings.txt` many settings can be found here. Most of them will be reloaded while runtime, just save the file to apply the changes.
- The `boards` folder, by default you should put your board files here.

Once you start OpenTUNG, you will see an empty board which you can use as the base of your creations.\
If is however possible to import/load existing boards, by putting them into the `boards` folder and adding their filename as argument to the OpenTUNG.jar\
Example: `java -jar OpenTUNG.jar myAwesomeBoard.opentung` or with `.tungboard` if you want to import TUNG files.

If your imported TUNG board is upside down or wrongly positioned, check the settings, some are for the importing of boards. Once it is properly aligned, save it as `.opentung` file.


## Obtaining OpenTUNG

OpenTUNG can be downloaded from the website.
OpenTUNG can be built from sources using JDK and Maven.

### Download prebuilt OpenTUNG.jar

- Visit the [OpenTUNG website](https://opentung.ecconia.com/).
- Download the prebuilt `OpenTUNG.jar`.
- Make sure you have Java 8 or newer installed.
- Open the .jar file with your respective OS's method of running jar files.\
  (Double clicking works if Java is properly installed).

### Build from Source

#### Preconditions:

- Java Development Kit of at least version 1.8.0_200 must be installed, but any newer JDK does the job too (9+).
- Maven, any recent version must be installed.

##### Prepare Windows:

- Install JDK (OpenJDK)
  - Be sure to have Environment Variable `JAVA_HOME` set and pointing to where you installed the JDK.
  - Add `%JAVA_HOME%\bin` to your `Path` if it is not already added.
  - You may have to restart after adding/changing an environment variable.
  - Try `java -version` in `cmd`, if it doesn't work consult your favorite search engine.
- Install Maven
  - Download from their website.
  - Extract to anywhere where you don't move it.
  - Set Environment Variable `MAVEN_HOME` pointing to where you extracted the file.
  - Add `%MAVEN_HOME%\bin` to your `Path`.
  - You may have to restart after adding/changing an environment variable.
  - Try `mvn -version` in `cmd`, if it doesn't work consult your favorite search engine.

##### Prepare Linux:

- Install OpenJDK
  - You may just use any other JDK installed via terminal, your favorite search engine helps.
  - Try `java -version` in your favorite terminal, if it doesn't work consult your favorite search engine.
- Install Maven, you may use provided `maven` package.
  - If the version is older, you might have no colors in terminal while compiling it.
  - You can easily get an up to date version. Just use your favorite search engine.
  - Try `mvn -version` in your favorite terminal, if it doesn't work consult your favorite search engine.

#### Build instructions:

- Clone the project from Github to any folder you like.\
  It is important that you use `git clone` and not the download button. Since you need the `.git` folder to build OpenTUNG.
- Run command `mvn package` in the project folder.
- Start the resulting .jar file `java -jar target/OpenTUNG-0-WIP-jar-with-dependencies.jar`.
  - The name of the .jar may change with rising OpenTUNG version.
  - You may move/copy/rename that file. It should work.
  - Starting OpenTUNG by double clicking might also work depending on your installation. If something goes wrong while starting open it with your favorite terminal.

## Collaborate/Contact

If you have questions or need help,\
If you have ideas or want to collaborate and improve things,\
If you just want to share your builds or discuss or talk about this project,

- then join the [OpenTUNG Discord-Server](https://discord.gg/W7ukHBn).

You will find fellow OpenTUNG users and the developer of OpenTUNG at your service.\
You may always contact developers and users there.

- There also is the [LogicWorld / TUNG Discord-Server](https://discord.gg/C5Qkk53)
  
If you don't know what this game is about, you should join it!

