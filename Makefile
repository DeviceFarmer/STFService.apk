.PHONY: default clean check-version prebuilt

APK := app/build/outputs/apk/release/app-release.apk
PROTO := app/src/main/proto/wire.proto

APK_VERSION := $(shell sed -n 's/.*versionName "\([^"]*\)".*/\1/p' app/build.gradle | head -1)
PKG_VERSION := $(shell sed -n 's/.*"version": *"\([^"]*\)".*/\1/p' package.json | head -1)

default: prebuilt

clean:
	./gradlew clean
	rm -rf prebuilt

# consumers match the on-device APK against the npm version, so a drift between it and versionName would reinstall the service on every connect
check-version:
	@test -n '$(APK_VERSION)' || { echo 'cannot read versionName from app/build.gradle'; exit 1; }
	@test '$(APK_VERSION)' = '$(PKG_VERSION)' || { echo 'version mismatch: app/build.gradle has $(APK_VERSION), package.json has $(PKG_VERSION)'; exit 1; }

$(APK):
	./gradlew build

# Listing both files here also acts as a safeguard to make sure that we really
# are including everything that is supposed to be there.
prebuilt: check-version \
  prebuilt/noarch/STFService.apk \
  prebuilt/noarch/wire.proto \

prebuilt/noarch/STFService.apk: $(APK)
	mkdir -p $(@D)
	cp $< $@

prebuilt/noarch/wire.proto: $(PROTO)
	mkdir -p $(@D)
	cp $< $@
