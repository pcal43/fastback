

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
ifndef SKIP_CHECKS
	@gitStatus=$$(git status --porcelain) ;\
	if [ "$${gitStatus}" != "" ]; then \
		echo $${gitStatus} ;\
		echo "You have uncommitted work."; echo; false; \
	fi

	@currentBranch=$$(git rev-parse --abbrev-ref HEAD) ;\
	if [ "$${currentBranch}" != "main" ]; then \
		echo "Releases must be performed on main"; false; \
	fi
endif
# todo port this into the makefile.  i think
	./release.sh

.PHONY: ide
ide:
	./gradlew cleanIdea idea


.PHONY: pr
pr:
	firefox https://github.com/pcal43/gitback/pulls

deps:
	./gradlew -q dependencies --configuration runtimeClasspath