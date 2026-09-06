# Publicar una version

La idea es no depender de tener el celular enchufado a esta compu: se sube una version
a GitHub y cualquiera la baja desde el navegador del celular.

## Sacar una version nueva

```bash
git tag v0.2.0
git push origin v0.2.0
```

Eso dispara el workflow `.github/workflows/release.yml`, que compila el APK, lo firma,
verifica que quedo firmado y lo cuelga en
<https://github.com/Maxi-Andres/music-player/releases>.

El tag tiene que ser `vMAYOR.MENOR.PARCHE`. De ahi salen las dos versiones que usa
Android:

| Tag      | `versionName` | `versionCode` |
|----------|---------------|---------------|
| `v0.2.0` | `0.2.0`       | `200`         |
| `v0.2.1` | `0.2.1`       | `201`         |
| `v1.0.0` | `1.0.0`       | `10000`       |

El `versionCode` tiene que crecer siempre: si baja o se repite, Android rechaza la
actualizacion. Por eso se calcula del tag y no se escribe a mano.

## Instalar en un celular

Entrar a Releases desde el navegador del celular, bajar el `.apk` y abrirlo. La primera
vez Android pide permiso para instalar apps de esa fuente (Chrome, o el explorador de
archivos). Despues alcanza con bajar el nuevo y abrirlo: se instala encima y conserva
los ajustes.

## La clave de firma

Android solo deja actualizar una app si el APK nuevo esta firmado con **la misma clave**
que el viejo. La clave vive en `C:/Users/Max/keys/music-player-release.jks`, fuera del
repo, y sus datos en `keystore.properties` (ignorado por git).

**Conviene tener una copia de los dos archivos en otro lado.** Si se pierden, no hay
forma de sacar una actualizacion: todos los que tengan la app instalada tienen que
desinstalarla —perdiendo sus ajustes— para poder poner la version nueva.

En GitHub Actions la clave sale de cuatro secrets del repo: `KEYSTORE_BASE64` (el `.jks`
en base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS` y `KEY_PASSWORD`.

## Compilar el APK firmado sin publicar

```bash
VERSION_NAME=0.2.0 VERSION_CODE=200 ./gradlew assembleRelease
```

Queda en `app/build/outputs/apk/release/app-release.apk`. Sin `keystore.properties` el
build igual funciona, pero el APK sale sin firmar y no se puede instalar.

## Las tres variantes

| Variante  | Para que sirve                                                        |
|-----------|-----------------------------------------------------------------------|
| `debug`   | Desarrollo. Va **lenta**: Compose sin optimizar y con cobertura JaCoCo. |
| `profile` | Probar en el celular por USB (`./gradlew installProfile`). Rapida.      |
| `release` | La que se publica. Optimizada con R8 y firmada.                        |
