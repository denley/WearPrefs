# Change Log
All notable changes to this project will be documented in this file.

## 1.1.2 - 2015-03-02
### Fixed
- `DataItemBuffer`s are properly released when no longer needed

## 1.1.1
### Changed
- Each key/value pair now uses a distinct path in the DataApi. This reduces unnecessary data transmitted between devices.
- When a device becomes connected, each value is re-loaded into SharedPreferences from the DataApi in case it has changed while disconnected.

## 1.1 - 2015-02-14
### Changed
- Now syncs on initialisation


## 1.0 - 2015-02-12
# Added
- Initial release