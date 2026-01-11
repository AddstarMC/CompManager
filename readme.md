# CompManager

[![Build Status](https://jenkins.addstar.com.au/buildStatus/icon?job=CompManager)](https://jenkins.addstar.com.au/job/CompManager/)

A Minecraft server plugin system for managing build competitions across multiple servers. Consists of two separate Bukkit plugins: one for lobby servers and one for competition servers.

## Overview

CompManager handles competition lifecycles including player entry, plot management, voting systems, and cross-server communication. It integrates with PlotSquared for plot management and uses Redis for server coordination.

## Features

- **Build Competition Management** - Full lifecycle management from entry to voting
- **Voting System** - Flexible voting strategies with result tracking
- **Whitelist Management** - Player whitelisting for competitions
- **Cross-Server Communication** - Redis-based coordination between lobby and comp servers
- **Plot Integration** - Seamless integration with PlotSquared
- **Notifications** - Configurable notifications and broadcasts

## Building

```bash
mvn clean package
```

Artifacts will be generated in:
- `lobby/target/CompLobbyManager-<version>.jar`
- `server/target/CompManager-<version>.jar`

## Project Structure

```
CompManager/
├── common/     # Shared code between lobby and server
├── lobby/      # Lobby server plugin
└── server/     # Competition server plugin
```