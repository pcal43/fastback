clean:
    rm -rf build

compile:
    ./gradlew compileJava

jar:
    ./gradlew jar

release-jars:
    ./gradlew buildReleaseJars

compile-common:
    ./gradlew :common:compileJava

release:
    ./gradlew release

ide:
    ./gradlew cleanIdea idea

pr:
    gh pr view --web 2>/dev/null || gh pr create --web

prs:
    {{ if os() == "macos" { "open" } else { "firefox" } }} https://github.com/pcal43/copper-hopper/pulls

deps:
    ./gradlew -q dependencies --configuration runtimeClasspath

clearCaches:
    ./gradlew --stop
    rm -rf "$HOME/.gradle/caches" "$HOME/.gradle/wrapper/dists" "$HOME/.gradle/daemon" "$HOME/.gradle/native"


run-fabric:
    ./gradlew :fabric:runClient

run-neoforge:
    ./gradlew :neoforge:runClient