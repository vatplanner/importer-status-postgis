# VATPlanner Status Importer to PostGIS

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE.md)

The importer requests VATSIM status files from a [https://github.com/vatplanner/raw-data-archiver](Raw Data Archive) and imports them to a PostGIS database for further analysis and directly accessible storage.

Please note that, other than [https://github.com/vatplanner/dataformats-vatsim-public](dataformats-vatsim-public), [https://github.com/vatplanner/status-fetcher](status-fetcher) and [https://github.com/vatplanner/raw-data-archiver](raw-data-archiver), this sub-project is specific to the VATPlanner application. As a result, only data needed for the larger VATPlanner project will be imported to database, irrelevant data will not be stored.

## Current State

- early start of implementation, does not actually do anything yet

## License

The implementation and accompanying files are released under [MIT license](LICENSE.md). Parsed data is subject to policies and restrictions set by VATSIM and your local regulations.
