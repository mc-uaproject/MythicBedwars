# MythicBedwars

[//]: # (![MythicBedwars Logo]&#40;https://via.placeholder.com/150x150.png?text=MythicBedwars&#41;)

MythicBedwars is a magical addon for Marcely's Bedwars that integrates the Circle of Imagination magic system into Bedwars gameplay. This plugin enhances the traditional Bedwars experience by adding magical pathways, sequences, and potions that players can use to gain advantages in battle.

## Features

- **Magical Pathways**: Each team is randomly assigned a unique magical pathway (Fool, Door, Error, Priest, Sun, Tyrant, Demoness)
- **Sequence Progression**: Players can advance through magical sequences (9 to 0) by purchasing and consuming potions
- **Custom Shop**: Adds a "Magic" category to the Bedwars shop where players can purchase sequence potions
- **Acting Progression**: Players gain "acting" (magical energy) passively and through game actions like kills and bed breaks
- **Balanced Economy**: Configurable prices for potions using standard Bedwars currencies (iron, gold, diamond, emerald)
- **Team-Based Magic**: Each team has its own magical pathway, creating unique strategies and gameplay dynamics
- **Multilingual Support**: Includes Ukrainian language files and ability to customize them

_## Installation_

1. Make sure you have [MBedwars](https://www.spigotmc.org/resources/marcelys-bedwars-1-8-1-20.50217/) (version 5.5.3 or higher) installed
2. Install the [CircleOfImagination](https://github.com/ikeepcalm/circle-of-imagination) plugin (version 1.1.7 or higher)
3. Download the latest version of MythicBedwars
4. Place the JAR file in your server's `plugins` folder
5. Restart your server
6. Configure the plugin settings in the `config.yml` file

## Configuration

### Main Configuration (config.yml)

```yaml
# Acting progression multipliers
acting:
  # Base amount of acting gained per second
  passive-amount: 10
  # Multiplier for passive acting gain
  passive-multiplier: 1.0
  # Multiplier for kills
  kill-multiplier: 5.0
  # Multiplier for final kills
  final-kill-multiplier: 7.0
  # Multiplier for breaking enemy beds
  bed-break-multiplier: 10.0
```

### Language Files

The plugin includes language files for Ukrainian. You can customize messages in:
- `lang/lang-uk.yml`

## Usage

1. Start a Bedwars game as usual
2. Each team will be automatically assigned a magical pathway
3. Players can purchase sequence potions from the "Magic" category in the shop
4. Consuming potions advances players through magical sequences
5. Players gain acting (magical energy) passively and through game actions
6. Use your magical abilities to gain an advantage in battle!

## Dependencies

- **[Paper](https://papermc.io/)**: 1.21.4 or higher
- **[MBedwars](https://www.spigotmc.org/resources/marcelys-bedwars-1-8-1-20.50217/)**: 5.5.3 or higher
- **[CircleOfImagination](https://github.com/ikeepcalm/circle-of-imagination)**: 1.1.7 or higher

## Commands and Permissions

This plugin doesn't add any additional commands or permissions. It integrates seamlessly with MBedwars' existing systems.

## Support and Issues

If you encounter any issues or have suggestions for improvements, please open an issue on the GitHub repository.

## License

This project is licensed under the [MIT License](LICENSE).

---

Made with ❤️ for the Minecraft community