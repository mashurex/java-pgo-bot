# Pokemon GO Command Line Java Bot

## Description

Pokemon GO walking/sniping bot using the [PokeGOAPI Java library](https://github.com/Grover-c13/PokeGOAPI-Java "PokeGOAPI-Java")

This bot currently does the following:

- Auto evolves the highest CP Pokemon
- Auto transfers the lowest ranked Pokemon (under a specified CP value)
- Can walk at the right speed to hatch eggs
- Walks from Pokestop to Pokestop looting and catching along the way
- Can get from Level 0 to Level 10 in about 45 minutes. Level 20 in about 3ish hours.
- Sniper mode can teleport to a specific location for a specific Pokemon and complete the catch back where you want it to prevent soft bans
- Soft ban spin fix: Attempts to macro the Pokestop open/spin/close repeat steps until the account unlocks.

## Requirements

- The PokeGOAPI library
- The [geocalc-0.5.1](https://github.com/grumlimited/geocalc) Library
- Java 1.8
- Gradle

## Building

- Check out and build the PokeGOAPI and GeoCalc libraries
- Copy those JARs into a `lib` directory in this folder
- Run `gradle build bundle` to create a ready to go executable JAR
