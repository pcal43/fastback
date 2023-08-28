

.PHONY: clean
clean:
	rm -rf build

.PHONY: jar
jar:
	./gradlew remapJar
	ls -1 build/libs

test:
	./gradlew test

.PHONY: release
release:
	./etc/release.sh

.PHONY: ide
ide:
	./gradlew cleanIdea idea


.PHONY: pr
pr:
	firefox https://github.com/pcal43/gitback/pulls

.PHONY: deps
deps:
	./gradlew -q dependencies --configuration runtimeClasspath