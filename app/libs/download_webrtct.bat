del libwebrtc-usman-new.aar
@echo off
set /p branchname= "Enter branchname: "
curl -u confu:APK@lat --output libwebrtc-%branchname%-new.aar --url https://builds.confu.info/libwebrtc-%branchname%-new.aar