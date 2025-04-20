# VATPlanner Status Importer to PostGIS

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE.md)

The importer requests VATSIM status files from a [Raw Data Archive](https://github.com/vatplanner/raw-data-archiver) and imports them to a PostGIS database for further analysis and directly accessible storage.

Please note that, other than the generally usable sub-projects [dataformats-vatsim-public](https://github.com/vatplanner/dataformats-vatsim-public), [status-fetcher](https://github.com/vatplanner/status-fetcher) and [raw-data-archiver](https://github.com/vatplanner/raw-data-archiver), this sub-project is specific to the VATPlanner application and is not intended to be useful for other applications. As a result, only data needed for the larger VATPlanner project will be imported to database, data irrelevant to the project will not be stored.

## Current State

- import works but resulting data needs to be checked manually for any obvious errors
- plenty of TODOs in code remaining (also see other modules)

## Requirements

- Java 8 or later
- PostgreSQL database (tested with 12) and PostGIS 3.0
  - schema has to be imported manually, see: [sql/status.sql](sql/status.sql)
- RabbitMQ AMQP server to fetch data from archive
- an instance of [raw-data-archiver](https://github.com/vatplanner/raw-data-archiver) serving VATSIM data files to import
- it is recommended to build this application together with [`dataformats-vatsim-public`](https://github.com/vatplanner/dataformats-vatsim-public), although it
  may also work with the latest released version when overwriting the dependency version (snapshots do not get deployed)

## Configuration

By default, a user configuration is expected to be defined in `~/.vatplanner/importer-status-postgis.properties`. When starting the importer, the first parameter can be used to define an alternate config path.

User configuration only needs to specify differences to the [default configuration](src/main/resources/importer-status-postgis.properties).

## Usage

Imports are carried out as chunks configurable in two levels:

1. database is queried for the fetch timestamp of last imported report/data file
2. if database appears empty
   - by default import will abort
   - if `allowImportOnEmptyDatabase` is enabled, import will start requesting archived data starting at `emptyDatabaseEarliestFetchTime`
3. reports for the configured time period of full graph entities are loaded back into memory (required to continue graph import where left off)
4. graph import continues by requesting `maxFilesPerChunk` archived data files from last imported (or requested earliest) fetch time + 1 second
   - application terminates if archive has no further data to be imported
5. results are written to database
6. if `maxFilesBeforeRestart` has not been reached, repeat from step 4
7. if `maxFilesBeforeRestart` has been reached, evict previous graph from memory, check memory limits and restart from step 1
   - application terminates if memory limits have been exceeded

The application will terminate under any of the following conditions:

- expected cases; not indicating errors:
  - archive does not provide any further data to import => restart when archive is expected to hold new data
  - permanently allocated memory exceeds threshold; preventing OOM => restart, import has not finished
- error cases; check log / increase log level if hit:
  - SQL errors
  - connection errors
  - known data inconsistencies

## License

The implementation and accompanying files are released under [MIT license](LICENSE.md). Parsed data is subject to policies and restrictions set by VATSIM and your local regulations.

### Note on the use of/for AI

Usage for AI training is subject to individual source licenses, there is no exception. This generally means that proper
attribution must be given and disclaimers may need to be retained when reproducing relevant portions of training data.
When incorporating source code, AI models generally become derived projects. As such, they remain subject to the
requirements set out by individual licenses associated with the input used during training. When in doubt, all files
shall be regarded as proprietary until clarified.

Unless you can comply with the licenses of this project you obviously are not permitted to use it for your AI training
set. Although it may not be required by those licenses, you are additionally asked to make your AI model publicly
available under an open license and for free, to play fair and contribute back to the open community you take from.

AI tools are not permitted to be used for contributions to this project. The main reason is that, as of time of writing,
no tool/model offers traceability nor can today's AI models understand and reason about what they are actually doing.
Apart from potential copyright/license violations the quality of AI output is doubtful and generally requires more
effort to be reviewed and cleaned/fixed than actually contributing original work. Contributors will be asked to confirm
and permanently record compliance with these guidelines.
