# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.10] – 2024-08-24
### Fixed
* Displayed username was wrong.

### Added
* Added descriptions to bookmarks

## [0.9] – 2024-04-16
### Added
* Restored search and registration
* Implemented Anti-Forgery protection

## [0.8] – 2023-11-04
### Changed
* The search now considers both site title and url

### Added
* When user is unauthenticated, the login process will redirect the user to the original URL after successful login

## [0.7] – 2023-04-05
### Added
* Search!!

## [0.6] – 2023-03-01
### Fixed
* Navigation renders correctly on mobile devices

### Added
* Token authentication – no yet though

## [0.5] – 2022-10-23
### Fixed
* Username/Password check was too permissive and did not check the password correctly

### Added
* Bookmarks can be edited now!

## [0.4] – 2022-10-22
### Changed
* Integrated HTMX for dynamic functions
* Small visual improvements (shows original URL, favicon)
* CI changes

## [0.3] – 2022-10-13
### Added
* Authentication system!
    * Registration and Login now possible!
    * Bookmarks are now assigned to users

### Changed
* Myuri is now using the [Bulma CSS](https://bulma.io/) framework – looks much better now!
* IDs were changed from integers to UUID

## [0.2] – 2022-05-09
### Added
- Bookmarks can be added using a bookmarklet and a popup
- Delete button for all the bookmarks
- Multi stage Dockerfile for builds without local tool stack installation
- Docker-Compose file for instant launch of MyUri

## [0.1] – 2022-05-04
### Added
- Initial Release.
