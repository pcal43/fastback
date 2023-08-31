

.PHONY: clean
clean:
	rm -rf build common/build fabric/build forge/build


.PHONY: jar
jar:
	./gradlew remapJar
	ls -1 fabric/build/libs
	ls -1 forge/build/libs

test:
	./gradlew test

.PHONY: release
release:
	./etc/release.sh

.PHONY: docgen
docgen:
	./etc/docgen.sh

.PHONY: ide
ide:
	./gradlew cleanIdea idea


.PHONY: pr
pr:
	firefox https://github.com/pcal43/gitback/pulls

.PHONY: deps
deps:
	./gradlew -q dependencies --configuration runtimeClasspath


.PHONY: inst
inst:
	rm ~/minecraft/instances/1.20.1-forge-dev/.minecraft/mods/*
	cp forge/build/libs/fastback*-forge.jar ~/minecraft/instances/1.20.1-forge-dev/.minecraft/mods/

.PHONY: tvf
tvf:
	jar -tvf forge/build/libs/fastback*-forge.jar

.PHONY: tvfs
tvfs:
	jar -tvf forge/build/libs/fastback*-shadow.jar
