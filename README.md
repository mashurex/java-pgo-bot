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

- Git
- Java 1.8
- Gradle

## Submodules

These submodules will be initialized in the `submodules` directory.

- The [PokeGOAPI Java library](https://github.com/Grover-c13/PokeGOAPI-Java "PokeGOAPI-Java")
- The [geocalc-0.5.1](https://github.com/grumlimited/geocalc) Library

## Building

- Clone this repository
- Change to this root directory
- Initialize the submodules with `git submodule update --init --recursive`
- Build with gradle `gradle build`

This will give you an executable jar in the `build/libs` directory and you'll be ready to go botting!

## Execution

### Configuration

You must place a `pokebot.properties` file in the same folder as the executable jar you can store a number of default 
configuration parameters:

```
# REQUIRED
pokebot.maps.key=<YOUR GOOGLE MAPS API KEY>

# OPTIONAL
pokebot.auth.username=<PTC AUTH USERNAME>
pokebot.auth.password=<PTC AUTH PASSWORD>
pokebot.auth.provider=<PTC or GOOGLE>
pokebot.defaults.origin=<LATITUDE>,<LONGITUDE>
```

#### Command Line Parameters

If you run the jar with the `-h` flag it will output all the available options.

`# TODO make a CLI param help table` 

##### Examples

###### Basic Wandering with PTC Login
```
# With no default username or password
java -jar pgojbot.jar -lat "45.518502" -lng "-122.682156" -l PTC -u "username" -p "password"
```

```
# With a default username and password in the properties file
java -jar pgojbot.jar -lat "45.518502" -lng "-122.682156"
```

###### Basic Wandering with Google Login
```
java -jar pgojbot.jar -lat "45.518502" -lng "-122.682156" -l GOOGLE
```

###### Sniping
```    
# Sniping an Eevee with default login options and origin set in properties file    
java -jar pgojbot.jar -snipe -dest-lat "45.474577292898935" -dest-lng "-122.64651775360109" -pokemon "EEVEE"
```


