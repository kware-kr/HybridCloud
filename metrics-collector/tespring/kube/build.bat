cd ..

START /wait /b cmd /c gradlew.bat build jar -Pprofile=dev -Dfile.encoding=UTF-8 -Dorg.gradle.java.home=C:\Users\lidbe\AppData\Local\JetBrains\Toolbox\apps\IDEA-U\ch-0\223.7571.182\jbr

docker build --build-arg JAR_FILE=build/libs/*.jar -t lectinua/tespring .

:: docker run -e "SPRING_PROFILES_ACTIVE=dev" -p 8080:8080 -t lect/tespring

docker push lectinua/tespring
