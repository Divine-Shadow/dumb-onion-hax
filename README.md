# dumb-onion-hax
High end hax for dominions 6 maps.

Built on the Typelevel stack with Cats, FS2, Log4cats, and property-based tests via ScalaCheck.

See [documentation/engineering/architecture/project_structure.md](documentation/engineering/architecture/project_structure.md) for the module layout and consult [AGENTS.md](AGENTS.md) before contributing.

The short term goal of this project is to be able to parse map files and then edit them with things like:
- Sever the adjacency rules between rows 0 and 7
- Place thrones in specific locations
- Make all tiles have 'high magic sites'

## License

![License: Polyform Noncommercial](https://img.shields.io/badge/license-Polyform%20Noncommercial-blue)

This project is licensed under the [Polyform Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0/).

You may use this project freely for personal, noncommercial purposes.  
Commercial use or distribution requires attribution and explicit permission.
