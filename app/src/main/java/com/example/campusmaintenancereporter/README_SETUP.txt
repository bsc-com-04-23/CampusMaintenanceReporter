SETUP
1. Create the project with the "Empty Activity" template (Compose).
2. In app/build.gradle.kts add inside dependencies { }:
       implementation("androidx.navigation:navigation-compose:2.8.5")
   then Sync Now.
3. Copy all .kt files into app/src/main/java/<your/package>/ (replace the generated MainActivity.kt).
   You can delete the generated ui/theme folder (these files provide MaintenanceTheme).
4. If your package is not com.example.campusmaintenance, change the first line
   "package ..." in every file (or use Android Studio's refactor/rename).
5. Run the app. Pick a role on the login screen to see the different flows.
