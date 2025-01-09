# Tennis Stats API - Quarkus

Tennis Stats API is a RESTful API built using the Quarkus framework. It offers comprehensive features for managing and analyzing tennis (ping-pong) data, with seamless database integration through Panache and file generation support via Apache POI.

## Features

### Tournaments
- **Create Tournament**: Add a new tournament to the database.
- **Get All Tournaments**: Retrieve a list of all tournaments.
- **Get Tournament History**: Fetch historical data for a specific tournament, including past matches and results.

### Players
- **Create Player**: Add a new player to the database.
- **Get Matches History**: Retrieve match history for a specific player.
- **Get Progress**: Track a player's progress over time, including rankings and performance trends.
- **Get Stats**: Generate detailed statistics for a player in various formats:
  - **XLS**: Download stats as an Excel spreadsheet.
  - **CSV**: Download stats as a CSV file.

### Matches
- **Create Match**: Record a new match with details such as players, scores, and tournament.
- **Ratings**: Generate player ratings based on match performance.
- **Best Results**: Retrieve record-breaking performances and best results among all players.

## Technologies Used

### Framework
- **Quarkus**: A modern, Kubernetes-native Java framework tailored for building efficient and lightweight REST APIs.

### Database Integration
- **Panache**: Simplifies database operations and provides an elegant interface for interacting with entities.

### File Generation
- **Apache POI**: Enables generation of Excel and CSV files for exporting player stats and other data.


## Contact

For questions, suggestions, or support, please contact [radik200058@gmail.com](mailto:radik200058@gmail.com).
