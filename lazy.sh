./gradlew clean jibBuildTar  
docker load < build/jib-image.tar
docker run -it --rm --env-file ss-parser.env ss-parser:1.12