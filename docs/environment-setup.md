# CrystalTech Environment Setup Guide

This guide walks through preparing a macOS development environment for the CrystalTech Forge mod (Minecraft 1.20.1, Forge 47.x, Java 17).

## 1. System Prerequisites
- macOS 12 (Monterey) or newer with admin rights.
- At least 15 GB free disk space (Forge MDK, Gradle caches, Minecraft assets).
- Reliable internet connection for dependency downloads.

## 2. Install Core Tooling
1. Install Homebrew if missing:
   ```sh
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```
2. Install required packages:
   ```sh
   brew install --cask temurin17
   brew install git python@3.12
   ```
   Gradle ships via the Forge MDK wrapper, so a standalone install is not required.
3. Set the Java 17 toolchain (add to `~/.zshrc`):
   ```sh
   export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
   export PATH="$JAVA_HOME/bin:$PATH"
   ```
   Reload the shell: `source ~/.zshrc`.
4. Verify versions:
   ```sh
   java -version
   git --version
   python3 --version
   ```

## 3. Download Forge MDK Skeleton (1.20.1)
1. Create a workspace folder:
   ```sh
   mkdir -p "$HOME/dev/minecraft"
   cd "$HOME/dev/minecraft"
   ```
2. Download the Forge MDK zip (replace the URL if a newer 47.x build is preferred):
   ```sh
   curl -L -o forge-1.20.1-mdk.zip \
     https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.3.0/forge-1.20.1-47.3.0-mdk.zip
   ```
3. Unzip and rename the directory:
   ```sh
   unzip forge-1.20.1-mdk.zip
   mv forge-1.20.1-47.3.0-mdk crystaltech
   cd crystaltech
   ```
4. Remove sample build files you plan to replace (optional cleanup):
   ```sh
   rm -f examplemod.iml
   rm -rf src/main/java/net/minecraftforge/example
   ```

## 4. Initialize Git Repository
1. Inside `crystaltech`:
   ```sh
   git init
   git branch -m main
   git config --global core.autocrlf input
   ```
2. Create a `.gitignore` tailored for Forge projects (see project doc).

## 5. Configure Gradle Wrapper and Dependencies
- Forge MDK includes `gradlew`. Ensure it is executable:
  ```sh
  chmod +x gradlew
  ```
- Run the initial setup task to populate Gradle caches:
  ```sh
  ./gradlew genIntelliJRuns
  ```
  This also validates the Java toolchain.
- For VS Code, the equivalent run configs will be generated later via the Gradle tasks panel or Forge's `genVSCodeRuns`.

## 6. VS Code Setup
1. Install the following extensions:
   - `vscjava.vscode-java-pack`
   - `forgeforge.forge-vscode` (community maintained)
   - `ms-python.python` (for convenience scripts)
   - `redhat.vscode-xml` (Gradle POM and Forge config editing)
2. Open the project directory `crystaltech` in VS Code.
3. Run:
   ```sh
   ./gradlew genVSCodeRuns
   ```
   Then reload the window; VS Code will detect the run configurations in `.vscode/`.

## 7. Launch Configurations
- Use `Run and Debug` → `Minecraft Client` to launch a development instance.
- Default memory arguments are set by Forge; adjust in `.vscode/launch.json` (`-Xmx4G` recommended).

## 8. Project Layout Baseline
After the MDK bootstrap and initial cleanup, target structure:
```
crystaltech/
├─ src/main/java/com/crystaltech/
│  ├─ CrystalTech.java
│  ├─ registry/
│  ├─ capability/
│  ├─ event/
│  └─ core/
├─ src/main/resources/
│  ├─ META-INF/mods.toml
│  ├─ assets/crystaltech/
│  └─ data/crystaltech/
├─ gradle/
├─ gradlew
├─ build.gradle
└─ settings.gradle
```

## 9. Development Workflow
1. Update dependencies in `build.gradle` cautiously (target Forge 47.x).
2. Use `./gradlew runClient` for smoke testing.
3. For unit-style logic tests (capabilities, data), integrate with `JUnit` in Gradle if desired.
4. Before packaging, run `./gradlew build` to produce the mod jar under `build/libs/`.

## 10. Troubleshooting Tips
- **Java mismatch:** Ensure `java -version` reports 17.x. Forge 1.20.1 fails on Java 21+.
- **Gradle daemon memory:** Add `org.gradle.jvmargs=-Xmx4G` to `gradle.properties` if builds exhaust heap.
- **Missing run configs:** Rerun `./gradlew genVSCodeRuns` after deleting `.vscode` run files.
- **Minecraft assets download errors:** Clear the `.gradle` cache (`~/.gradle/caches`) and rerun the Gradle task.

Environment is now ready to begin implementing the CrystalTech mod.
