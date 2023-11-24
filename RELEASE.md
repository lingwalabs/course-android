# Release checklist

*** Before release ***

- Update versionName and versionCode in AndroidManifest.xml and build.gradle
- Remove "-SNAPSHOT" from version (e.g. from "2.0.0-SNAPSHOT" to "2.0.0")
- Set EnvironmentSettings.ENVIRONMENT to Environment.PROD

Curso Noruego
- Update to package="es.cursonoruego" in mobile/src/main/AndroidMainfest.xml and wear/src/main/AndroidMainfest.xml
- Update to domain cursonoruego.es

*** Google Play release ***
- See details in Google Drive: https://docs.google.com/document/d/18m1LtO6DwHPOTivG9VeJqlRjSPMoHbr6ll1k_IGR5hs/edit#heading=h.930nt5ftcuwa

*** Create a new tag ***

*** After release ***

- Tag released version
- Update versionName and versionCode in AndroidManifest.xml and build.gradle
- Increment version (e.g. from "2.0.0 to 2.0.1")
- Add "-SNAPSHOT" to version (e.g. from "2.0.1" to "2.0.1-SNAPSHOT")
- To enable logging, set EnvironmentSettings.ENVIRONMENT to Environment.TEST

*** After Google Play release **
- Update /rest/v2/version/android (VersionRestV2Helper.java) on web server
