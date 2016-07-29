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

## Command Line Parameters

If you run the jar with the `-h` flag it will output all the available options.

### Authentication Parameters

*All of these can be configured in the `pokebot.properties` file and do not require being entered at the command line*

Option | Description | Type | Sample Value
------ | ----------- | ---- | ------------
`-u` | PTC username | `String` | SuperCoolPokemonBot
`-p` | PTC password | `String` | MyPasswordNeedsChanging1
`-l` | Login provider to use | `Enum` | `PTC` or `GOOGLE`

### Bot Mode Parameters

Mode | Description | Add'l Required Parameters
---- | ----------- | -------------------
`--wander` (or nothing) | This is the **default mode**, simply having no other mode will enable this. | None
`--snipe` | Teleports to the destination to capture a rare Pokemon | `--dest-lat`, `--dest-lng`. If `--pokemon` is provided it will only catch that type of Pokemon at the destination.
`--softban` | Attempts to use the spin fix method of releasing a soft ban. | None
`--fight` | Will run to the nearest Gym and attempt to take it over. **This is still in development and really just does not work at the moment** | None (yet)

### Location Parameters

*`lat` and `lng` can be put in the `pokebot.properties` file if you always want your bot to start at the same place*

Option | Description | Type | Sample Value
------ | ----------- | ---- | ------------
`--lat` | Bot's starting latitude | `Double` | 45.2181432
`--lng` | Bot's starting longitude | `Double` | -121.1812911
`--dest-lat` | The destination or target latitude to use for various modes (like Sniping) | `Double` | 45.218123
`--dest-lng` | The destination or target longitude to use for various modes (like Sniping) | `Double` | -121.181291

### All Other Parameters

Option | Description | Type | Default Value
------ | ----------- | ---- | ------------
`-b` | Perform heartbeat every `[value]` number of operation ticks | `Integer` | 50
`-c` | Maximum CP value to auto transfer (Pokemon with CP above this value will be kept) | `Integer` | 250
`-e` | Auto-evolve Pokemon when possible | `Flag` |
`-f` | Attempt to fight at nearby Gyms while walking | `Flag ` |
`-s` | Size of each bot 'step' in meters | `Double` | 10
`-t` | Auto-transfer Pokemon under `-c` CP | `Flag ` |
`-w` | Use walking speed to enable egg hatching, slows the bot down to not exceed roughly 2 m/s | `Flag ` |
`-x` | Enables debug mode | `Flag ` |
`--pokemon` | The family name (like `EEVEE`) to target for a specific mode or operation | `PokemonId` | None

## Examples

### Wandering

```
# With no default username or password
java -jar pgojbot.jar -lat "45.518502" -lng "-122.682156" -l PTC -u "username" -p "password"
```

```
# With a default username and password in the properties file
java -jar pgojbot.jar -lat "45.518502" -lng "-122.682156"
```

```
# Wandering with Google Login
java -jar pgojbot.jar -lat "45.518502" -lng "-122.682156" -l GOOGLE
```

### Advanced Wandering
```
# Walking speed, auto transfer, evolve, and fight in debug mode
java -jar pgojbot.jar -lat "45.518502" -lng "-122.682156" -x -w -e -f -t
```

### Sniping
```    
# Sniping an Eevee with default login options and origin set in properties file    
java -jar pgojbot.jar -snipe -dest-lat "45.474577292898935" -dest-lng "-122.64651775360109" -pokemon "EEVEE"
```

## Configuration / Performance Tips

### Travel Speed / Walking Speed

The faster the bot travels, the faster the bot can earn XP. *However*, the faster you travel the more likely your bot is 
to run into a ban of some sort. If the bot travels too fast it can't tally up the distance needed to hatch eggs. Using 
the `-w` flag forces the bot to keep it at a walking speed as much as possible.
 
### Using the properties file

If you intend to just run a single account you should really just fill out all the fields in the `pokebot.properties`file to save the keystrokes.
 

