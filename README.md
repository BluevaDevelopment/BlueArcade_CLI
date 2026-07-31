<p align="center">
  <img src="assets/bluearcade-logo.png" alt="BlueArcade" width="760">
</p>

<h1 align="center">bacli</h1>

<p align="center">
  <strong>The official authoring CLI for BlueArcade 3 Universal Modules.</strong>
</p>

<p align="center">
  <img alt="License: GPL v3" src="https://img.shields.io/badge/License-GPLv3-blue.svg">
  <img alt="PRs Welcome" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg">
  <img alt="BlueArcade 3" src="https://img.shields.io/badge/BlueArcade-3-9146FF.svg">
  <img alt="Format" src="https://img.shields.io/badge/format-.bamodule-orange.svg">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white">
  <img alt="GitHub issues" src="https://img.shields.io/github/issues/BluevaDevelopment/BlueArcade_CLI">
  <img alt="GitHub last commit" src="https://img.shields.io/github/last-commit/BluevaDevelopment/BlueArcade_CLI">
</p>

`bacli` scaffolds, checks, tests, and builds `.bamodule` Universal Modules for BlueArcade 3, the
sandboxed Lua module format that runs identically across every BlueArcade Edition, including
Minecraft and Hytale. No prior knowledge of the runtime internals is needed to author a module.

## Installation

Download the latest `bacli` jar from the releases page of this repository, or build it yourself:

```
git clone git@github.com:BluevaDevelopment/BlueArcade_CLI.git
cd BlueArcade_CLI
./gradlew shadowJar
```

The built jar lands in `build/dist/bacli-<version>.jar`.

## Usage

```
bacli init [dir] --id <id> [--type minigame|microgame] [--name <name>] [--author <author>]
bacli check [dir]
bacli build [dir] [--out <dir>]
bacli test [dir]
```

`[dir]` defaults to the current directory.

| Command | What it does |
|---|---|
| `init` | Scaffolds a new module project: `module.toml`, a starter `src/main.lua`, and a language file. |
| `check` | Validates a project: manifest schema, file layout, and Lua syntax, before you try to build it. |
| `build` | Runs `check`, then packages the project into a loadable `.bamodule` archive. |
| `test` | Runs plain Lua unit tests under `tests/` against a small built-in assertion library. |

## The .bamodule format

A module project is a plain directory containing a `module.toml` manifest, a `src/` folder of Lua
files, and an optional `resources/` folder for YAML settings and language files. `bacli build`
packages it into a single `.bamodule` archive, a zip file with a renamed extension, ready to be
dropped into a running BlueArcade 3 server.

## Publishing a module

Once a module is built, it can be published independently at
[blueva.net/store/blue-arcade/modules/manage](https://blueva.net/store/blue-arcade/modules/manage),
with no pull request needed anywhere. Blueva's own official modules, along with the pattern for
adopting a community module officially, live in the
[BlueArcade_Modules](https://github.com/BluevaDevelopment/BlueArcade_Modules) repository.

## Contributing

Pull requests are welcome, whether that means fixing a bug, improving an existing command, or
proposing a new one. Opening an issue first for anything non trivial is appreciated, so the change
can be discussed before any real work goes into it.

## License

Licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for the full text.

## About BlueArcade

Part of [BlueArcade 3](https://blueva.net), a cross platform framework for building modular
multiplayer minigames.
