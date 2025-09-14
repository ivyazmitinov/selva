# Selva
CM3070 Computer Science Final Project

# Run instructions

## System requirements

- Ubuntu 24.04 (preferred)
- Java 21.0.8
- Docker 28.3.3

## Steps

- Build test docker image. It will be used for tests and local run
```shell
docker build docker/ -t selva_postgres:17.5
```
- Execute ` ./gradlew playwrightShowDeps`, which will generate a command to install the Playwright dependencies
```shell
./gradlew playwrightShowDeps
 
# > Task :selva:playwrightShowDeps
#sudo -- sh -c "apt-get update&& apt-get install -y --no-install-recommends libasound2t64 libatk-bridge2.0-0t64 libatk1.0-0t64 libatspi2.0-0t64 libcairo2 libcups2t64 libdbus-1-3 libdrm2 libgbm1 libglib2.0-0t64 libnspr4 libnss3 libpango-1.0-0 libx11-6 libxcb1 libxcomposite1 libxdamage1 libxext6 libxfixes3 libxkbcommon0 libxrandr2 libcairo-gobject2 libfontconfig1 libfreetype6 libgdk-pixbuf-2.0-0 libgtk-3-0t64 libpangocairo-1.0-0 libx11-xcb1 libxcb-shm0 libxcursor1 libxi6 libxrender1 gstreamer1.0-libav gstreamer1.0-plugins-bad gstreamer1.0-plugins-base gstreamer1.0-plugins-good libicu74 libatomic1 libenchant-2-2 libepoxy0 libevent-2.1-7t64 libflite1 libgles2 libgstreamer-gl1.0-0 libgstreamer-plugins-bad1.0-0 libgstreamer-plugins-base1.0-0 libgstreamer1.0-0 libgtk-4-1 libharfbuzz-icu0 libharfbuzz0b libhyphen0 libjpeg-turbo8 liblcms2-2 libmanette-0.2-0 libopus0 libpng16-16t64 libsecret-1-0 libvpx9 libwayland-client0 libwayland-egl1 libwayland-server0 libwebp7 libwebpdemux2 libwoff1 libxml2 libxslt1.1 libx264-164 libavif16 xvfb fonts-noto-color-emoji fonts-unifont xfonts-cyrillic xfonts-scalable fonts-liberation fonts-ipafont-gothic fonts-wqy-zenhei fonts-tlwg-loma-otf fonts-freefont-ttf"
```
- Execute tests with `./gradlew test`. If necessary, tweak the [PlaywrightBrowserLauncher.java](selva/src/test/java/org/ivanvyazmitinov/PlaywrightBrowserLauncher.java), disabling headless mode, for example
- Start postgres in docker via `docker compose down && docker compose up -d`
- Run application via `./gradlew run`. The application will be available at http://localhost:8080
- Default admin credentials are `admin/admin`

# Development

For development you will also need [Node](https://nodejs.org/en/download) to install and use [Tailwind CLI](https://tailwindcss.com/docs/installation/tailwind-cli):

```shell
npm install
npx @tailwindcss/cli -i ./tailwind-config.css -o ./selva/src/main/resources/assets/css/app.css --watch
``` 